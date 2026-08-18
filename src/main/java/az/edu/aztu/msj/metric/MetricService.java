package az.edu.aztu.msj.metric;

import az.edu.aztu.msj.article.ArticleMetricRepository;
import az.edu.aztu.msj.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Set;

@Service
public class MetricService {

    private static final Set<String> VALID = Set.of("ABSTRACT_VIEW", "FULLTEXT_VIEW", "PDF_DOWNLOAD");

    private final ArticleEventRepository events;
    private final ArticleMetricRepository metrics;
    private final ArticleMetricDailyRepository daily;

    public MetricService(ArticleEventRepository events, ArticleMetricRepository metrics,
                         ArticleMetricDailyRepository daily) {
        this.events = events;
        this.metrics = metrics;
        this.daily = daily;
    }

    /**
     * Record a metric event. De-duplicated to once per (article, type, visitor-session, day):
     * counters only move when the raw insert actually lands.
     */
    @Transactional
    public void record(Long articleId, String type, String ip, String userAgent, String referrer,
                       String country) {
        if (articleId == null || !VALID.contains(type)) {
            throw ApiException.badRequest("Invalid metric event");
        }
        String today = LocalDate.now().toString();
        String sessionHash = hash((ip == null ? "" : ip) + "|" + (userAgent == null ? "" : userAgent) + "|" + today);
        String ipHash = ip == null ? null : hash(ip);

        int inserted = events.insertDedup(articleId, type, sessionHash, ipHash, referrer,
                truncate(userAgent, 400), normalizeCountry(country));
        if (inserted == 0) {
            return; // duplicate within the dedup window — do not double count
        }
        metrics.increment(articleId, type);
        boolean isView = "FULLTEXT_VIEW".equals(type);
        boolean isDownload = "PDF_DOWNLOAD".equals(type);
        if (isView || isDownload) {
            daily.upsert(articleId, LocalDate.now(), isView ? 1 : 0, isDownload ? 1 : 0);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }

    /**
     * ISO-3166-1 alpha-2, or null. Cloudflare sends CF-IPCountry on every
     * proxied request; it also uses "XX" for unknown and "T1" for Tor, neither
     * of which is a country, so both are stored as null.
     */
    static String normalizeCountry(String raw) {
        if (raw == null) return null;
        String c = raw.trim().toUpperCase();
        if (c.length() != 2 || !c.chars().allMatch(Character::isLetter)) return null;
        if (c.equals("XX") || c.equals("T1")) return null;
        return c;
    }
}
