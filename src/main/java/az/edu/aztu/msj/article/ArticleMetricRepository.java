package az.edu.aztu.msj.article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ArticleMetricRepository extends JpaRepository<ArticleMetric, Long> {

    List<ArticleMetric> findByArticleIdIn(List<Long> articleIds);

    /** Projection mapped by column alias — avoids ambiguous Object[] native results. */
    interface Totals {
        long getViews();
        long getDownloads();
        long getCitations();
    }

    @Query(value = """
            select coalesce(sum(view_count),0)     as views,
                   coalesce(sum(download_count),0)  as downloads,
                   coalesce(sum(citation_count),0)  as citations
            from article_metrics
            """, nativeQuery = true)
    Totals totals();

    @Query(value = """
            select am.article_id, a.title, am.view_count, am.download_count, am.citation_count
            from article_metrics am
            join articles a on a.id = am.article_id
            order by am.view_count desc
            limit :limit
            """, nativeQuery = true)
    List<Object[]> topArticles(@Param("limit") int limit);

    /** Upsert-and-increment a counter atomically; creates the row if missing. */
    /** Same requirement as setCitationCount — MetricService.record() supplies a
     *  transaction today, but the annotation belongs on the write itself so a
     *  future caller cannot silently reintroduce the same failure. */
    @Transactional
    @Modifying
    @Query(value = """
            insert into article_metrics (article_id, view_count, abstract_view_count, download_count, citation_count)
            values (:articleId,
                    case when :col = 'FULLTEXT_VIEW' then 1 else 0 end,
                    case when :col = 'ABSTRACT_VIEW' then 1 else 0 end,
                    case when :col = 'PDF_DOWNLOAD'  then 1 else 0 end,
                    0)
            on conflict (article_id) do update set
                view_count          = article_metrics.view_count          + (case when :col = 'FULLTEXT_VIEW' then 1 else 0 end),
                abstract_view_count = article_metrics.abstract_view_count + (case when :col = 'ABSTRACT_VIEW' then 1 else 0 end),
                download_count      = article_metrics.download_count      + (case when :col = 'PDF_DOWNLOAD'  then 1 else 0 end),
                updated_at          = now()
            """, nativeQuery = true)
    void increment(@Param("articleId") Long articleId, @Param("col") String eventType);

    /**
     * Set the citation count (from Crossref), creating the metrics row if missing.
     *
     * @Transactional is required, not decorative: a @Modifying query with no
     * active transaction throws TransactionRequiredException. CitationService
     * .refreshAll() is deliberately NOT transactional — it makes 51 HTTP calls
     * with sleeps between them, and holding one transaction open across that
     * would pin a connection for ~20s — so the transaction has to live here,
     * one short write per article. Without it every call threw, refreshAll()
     * swallowed the exception into its `failed` counter, and the sync reported
     * "0/51 updated, 51 FAILED" while the Crossref fetches were all succeeding.
     */
    @Transactional
    @Modifying
    @Query(value = """
            insert into article_metrics (article_id, citation_count)
            values (:articleId, :count)
            on conflict (article_id) do update set
                citation_count = :count,
                updated_at     = now()
            """, nativeQuery = true)
    void setCitationCount(@Param("articleId") Long articleId, @Param("count") long count);
}
