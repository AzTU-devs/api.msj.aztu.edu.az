package az.edu.aztu.msj.article;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "article_status_history")
@Getter
@Setter
public class ArticleStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
