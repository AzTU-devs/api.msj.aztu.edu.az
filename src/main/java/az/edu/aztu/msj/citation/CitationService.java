package az.edu.aztu.msj.citation;

import az.edu.aztu.msj.article.Article;
import az.edu.aztu.msj.article.ArticleMetricRepository;
import az.edu.aztu.msj.article.ArticleRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Refreshes each published article's citation count from Crossref
 * (message.is-referenced-by-count), looked up by DOI. Free, no API key —
 * we send a mailto so Crossref routes us through its faster "polite pool".
 */
@Service
public class CitationService {

    private static final Logger log = LoggerFactory.getLogger(CitationService.class);

    private final ArticleRepository articles;
    private final ArticleMetricRepository metrics;
    private final boolean enabled;
    private final String mailto;
    private final RestClient http;

    public CitationService(ArticleRepository articles, ArticleMetricRepository metrics,
                           @Value("${msj.citations.enabled:true}") boolean enabled,
                           @Value("${msj.citations.mailto:msj@aztu.edu.az}") String mailto) {
        this.articles = articles;
        this.metrics = metrics;
        this.enabled = enabled;
        this.mailto = mailto;

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(5));
        rf.setReadTimeout(Duration.ofSeconds(12));
        this.http = RestClient.builder()
                .requestFactory(rf)
                .defaultHeader("User-Agent",
                        "MachineScience/1.0 (https://msj.aztu.edu.az; mailto:" + mailto + ")")
                .build();
    }

    /** Weekly automatic refresh (Sunday 03:00 by default). */
    @Scheduled(cron = "${msj.citations.cron:0 0 3 * * SUN}")
    public void scheduledRefresh() {
        if (!enabled) {
            log.info("Citation refresh is disabled (msj.citations.enabled=false)");
            return;
        }
        log.info("Scheduled citation refresh: {}", refreshAll());
    }

    /** Fetch and store the Crossref citation count for every published article with a DOI. */
    public Result refreshAll() {
        List<Article> list = articles.findPublishedWithDoi();
        int updated = 0, failed = 0;
        long totalCitations = 0;
        for (Article a : list) {
            try {
                Integer count = fetchCount(a.getDoi());
                if (count == null) { failed++; continue; }
                metrics.setCitationCount(a.getId(), count);
                updated++;
                totalCitations += count;
                Thread.sleep(150); // stay polite between Crossref calls
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                failed++;
                log.warn("Citation fetch failed for article {} (doi {}): {}", a.getId(), a.getDoi(), e.getMessage());
            }
        }
        Result r = new Result(list.size(), updated, failed, totalCitations);
        log.info("Citation refresh complete: {}", r);
        return r;
    }

    private Integer fetchCount(String doi) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://api.crossref.org/works/" + doi.trim())
                .queryParam("mailto", mailto)
                .build()
                .toUri();
        CrossrefResponse resp = http.get().uri(uri).retrieve().body(CrossrefResponse.class);
        return (resp == null || resp.message() == null) ? null : resp.message().isReferencedByCount();
    }

    public record Result(int articles, int updated, int failed, long totalCitations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CrossrefResponse(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(@JsonProperty("is-referenced-by-count") Integer isReferencedByCount) {}

    // ---- DOI matching from the journal's registered Crossref works ----

    /**
     * Fill in the DOI for published articles that don't have one, by matching their
     * title against the journal's works in Crossref (both ISSNs). Never overwrites an
     * existing DOI and never assigns the same DOI twice.
     */
    @Transactional
    public MatchResult matchDois() {
        Map<String, String> byTitle = fetchJournalDois();
        Set<String> used = new HashSet<>();
        int published = 0, matched = 0, already = 0, unmatched = 0;
        for (Article a : articles.findAll()) {
            if (!"PUBLISHED".equals(a.getStatus())) continue;
            published++;
            if (a.getDoi() != null && !a.getDoi().isBlank()) {
                already++;
                used.add(a.getDoi().toLowerCase());
                continue;
            }
            String doi = byTitle.get(normTitle(a.getTitle()));
            if (doi == null) { unmatched++; continue; }
            if (used.contains(doi) || articles.findByDoi(doi).isPresent()) { unmatched++; continue; }
            a.setDoi(doi);          // dirty-tracked; flushed at commit
            used.add(doi);
            matched++;
        }
        MatchResult r = new MatchResult(published, matched, already, unmatched);
        log.info("DOI match complete: {}", r);
        return r;
    }

    /** normalizedTitle -> DOI, for every work Crossref lists under the journal's ISSNs. */
    private Map<String, String> fetchJournalDois() {
        Map<String, String> map = new HashMap<>();
        for (String issn : List.of("2790-0479", "2227-6912")) {
            String cursor = "*";
            for (int guard = 0; guard < 100 && cursor != null; guard++) {
                URI uri = UriComponentsBuilder
                        .fromUriString("https://api.crossref.org/journals/" + issn + "/works")
                        .queryParam("rows", 200)
                        .queryParam("select", "DOI,title")
                        .queryParam("cursor", cursor)
                        .queryParam("mailto", mailto)
                        .build().toUri();
                WorksResponse resp;
                try {
                    resp = http.get().uri(uri).retrieve().body(WorksResponse.class);
                } catch (Exception e) {
                    log.warn("Crossref works fetch failed for ISSN {}: {}", issn, e.getMessage());
                    break;
                }
                if (resp == null || resp.message() == null) break;
                List<WorkItem> items = resp.message().items();
                if (items == null || items.isEmpty()) break;
                for (WorkItem it : items) {
                    if (it.doi() != null && it.title() != null && !it.title().isEmpty()) {
                        map.putIfAbsent(normTitle(it.title().get(0)), it.doi().toLowerCase());
                    }
                }
                cursor = resp.message().nextCursor();
                try { Thread.sleep(120); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        log.info("Fetched {} Crossref titles for the journal", map.size());
        return map;
    }

    /** Lowercase + strip everything but letters/digits, so "medium-density" == "MEDIUMDENSITY". */
    private static String normTitle(String t) {
        return t == null ? "" : t.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public record MatchResult(int publishedArticles, int matched, int alreadyHadDoi, int unmatched) {}
    public record SyncResult(MatchResult dois, Result citations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorksResponse(WorksMessage message) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorksMessage(@JsonProperty("next-cursor") String nextCursor, List<WorkItem> items) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkItem(@JsonProperty("DOI") String doi, List<String> title) {}
}
