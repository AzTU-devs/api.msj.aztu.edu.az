package az.edu.aztu.msj.article;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "article_files")
@Getter
@Setter
public class ArticleFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(nullable = false)
    private String kind;                 // MANUSCRIPT, SUPPLEMENTARY, REVISION, CAMERA_READY, COVER_LETTER, PUBLISHED_PDF

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
