package az.edu.aztu.msj.metric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleEventRepository extends JpaRepository<ArticleEvent, Long> {

    /**
     * Insert a raw event, ignoring the once-per-session/day dedup collision.
     * Returns rows inserted (1 = counted, 0 = duplicate for this session/day).
     */
    @Modifying
    @Query(value = """
            insert into article_events (article_id, event_type, session_hash, ip_hash, referrer, user_agent)
            values (:articleId, :eventType, :sessionHash, :ipHash, :referrer, :userAgent)
            on conflict do nothing
            """, nativeQuery = true)
    int insertDedup(@Param("articleId") Long articleId,
                    @Param("eventType") String eventType,
                    @Param("sessionHash") String sessionHash,
                    @Param("ipHash") String ipHash,
                    @Param("referrer") String referrer,
                    @Param("userAgent") String userAgent);
}
