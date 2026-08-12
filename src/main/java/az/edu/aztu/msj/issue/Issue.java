package az.edu.aztu.msj.issue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "issues")
@Getter
@Setter
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer volume;
    private Integer number;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "full_pdf_url")
    private String fullPdfUrl;

    private String doi;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
