package az.edu.aztu.msj.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "article_events")
@Getter
@Setter
public class ArticleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "occurred_at", insertable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "ip_hash")
    private String ipHash;

    @Column(name = "session_hash")
    private String sessionHash;

    private String country;
    private String referrer;

    @Column(name = "user_agent")
    private String userAgent;
}
