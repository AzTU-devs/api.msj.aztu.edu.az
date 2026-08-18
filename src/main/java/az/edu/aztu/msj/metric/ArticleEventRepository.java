package az.edu.aztu.msj.metric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleEventRepository extends JpaRepository<ArticleEvent, Long> {

    /**
     * Insert a raw event, ignoring the once-per-session/day dedup collision.
     * Returns rows inserted (1 = counted, 0 = duplicate for this session/day).
     */
    @Modifying
    @Query(value = """
            insert into article_events (article_id, event_type, session_hash, ip_hash, referrer, user_agent, country)
            values (:articleId, :eventType, :sessionHash, :ipHash, :referrer, :userAgent, :country)
            on conflict do nothing
            """, nativeQuery = true)
    int insertDedup(@Param("articleId") Long articleId,
                    @Param("eventType") String eventType,
                    @Param("sessionHash") String sessionHash,
                    @Param("ipHash") String ipHash,
                    @Param("referrer") String referrer,
                    @Param("userAgent") String userAgent,
                    @Param("country") String country);

    /** Reader countries for the export report. NULL country = events recorded
     *  before capture existed, surfaced as "unknown" rather than dropped. */
    @Query(value = """
            select coalesce(country, '??') as code, count(*) as views
              from article_events
             where event_type in ('FULLTEXT_VIEW','ABSTRACT_VIEW')
             group by coalesce(country, '??')
             order by views desc
            """, nativeQuery = true)
    List<CountryCount> countryBreakdown();

    interface CountryCount {
        String getCode();
        long getViews();
    }
}
