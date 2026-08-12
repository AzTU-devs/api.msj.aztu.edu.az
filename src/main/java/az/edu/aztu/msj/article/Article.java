package az.edu.aztu.msj.article;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@Getter
@Setter
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "abstract", columnDefinition = "text")
    private String abstractText;

    @Column(columnDefinition = "text")
    private String keywords;

    @Column(name = "subject_area")
    private String subjectArea;

    @Column(nullable = false)
    private String language = "en";

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    @Column(name = "handling_editor_id")
    private Long handlingEditorId;

    @Column(name = "issue_id")
    private Long issueId;

    private String doi;

    @Column(name = "page_start")
    private Integer pageStart;

    @Column(name = "page_end")
    private Integer pageEnd;

    @Column(name = "article_order")
    private Integer articleOrder;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("authorOrder asc")
    private List<ArticleAuthor> authors = new ArrayList<>();

    /** Keeps both sides of the relationship in sync so the FK is set on insert. */
    public void addAuthor(ArticleAuthor author) {
        author.setArticle(this);
        this.authors.add(author);
    }
}
