package az.edu.aztu.msj.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(nullable = false)
    private String recommendation;      // ACCEPT, MINOR_REVISION, MAJOR_REVISION, REJECT

    private Integer score;

    @Column(name = "comments_to_author", columnDefinition = "text")
    private String commentsToAuthor;    // sanitized HTML

    @Column(name = "comments_to_editor", columnDefinition = "text")
    private String commentsToEditor;    // sanitized HTML, hidden from the author

    @Column(name = "attachment_key")
    private String attachmentKey;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private OffsetDateTime submittedAt;
}
