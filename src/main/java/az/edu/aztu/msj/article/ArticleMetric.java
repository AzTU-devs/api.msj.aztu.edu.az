package az.edu.aztu.msj.article;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Denormalized per-article counters for fast reads. */
@Entity
@Table(name = "article_metrics")
@Getter
@Setter
public class ArticleMetric {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "abstract_view_count", nullable = false)
    private long abstractViewCount = 0;

    @Column(name = "download_count", nullable = false)
    private long downloadCount = 0;

    @Column(name = "citation_count", nullable = false)
    private long citationCount = 0;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
