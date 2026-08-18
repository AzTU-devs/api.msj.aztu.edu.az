package az.edu.aztu.msj.review;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ReviewDtos {

    public record AuthorDto(String firstName, String lastName, String email, String affiliation,
                            String country, String orcid, boolean corresponding) {}

    public record FileDto(Long id, String kind, String originalName, Long sizeBytes, String contentType) {}

    /** Manuscript as a reviewer sees it (single-blind: authors visible). */
    public record ArticleForReview(Long id, String title, String abstractText, String keywords,
                                   String subjectArea, String language, String status,
                                   OffsetDateTime submittedAt, List<AuthorDto> authors, List<FileDto> files) {}

    public record ReviewInput(
            @NotBlank String recommendation,   // ACCEPT, MINOR_REVISION, MAJOR_REVISION, REJECT
            Integer score,
            String commentsToAuthor,
            String commentsToEditor) {}

    public record MyReview(Long id, String recommendation, Integer score,
                           String commentsToAuthor, String commentsToEditor, OffsetDateTime submittedAt) {}

    public record AssignmentSummary(Long id, Long articleId, String articleTitle, String subjectArea,
                                    String assignmentStatus, String articleStatus, LocalDate dueDate,
                                    OffsetDateTime invitedAt, boolean reviewSubmitted) {}

    public record AssignmentDetail(AssignmentSummary assignment, ArticleForReview article, MyReview myReview) {}

    // ---- editorial ----

    public record ReviewerUser(Long id, String name, String email, String affiliation) {}

    public record AssignmentDto(Long id, Long reviewerId, String reviewerName, String status,
                                LocalDate dueDate, OffsetDateTime invitedAt, OffsetDateTime completedAt,
                                boolean reviewSubmitted) {}

    public record EditorReview(Long id, Long reviewerId, String reviewerName, String recommendation,
                               Integer score, String commentsToAuthor, String commentsToEditor,
                               OffsetDateTime submittedAt) {}

    public record StatusEvent(String fromStatus, String toStatus, String changedByName, String comment, Instant at) {}

    /**
     * pageStart/pageEnd/articleOrder are part of the record because the editor
     * form round-trips them: it loads this payload and PUTs the whole object
     * back. While they were missing, every save sent nulls and silently wiped
     * an article's pagination and its position in the issue — and pagination is
     * what feeds citation_firstpage/citation_lastpage for Google Scholar.
     *
     * issueVolume/issueNumber are the parent issue's, denormalised for display
     * only. Volume and number belong to the issue, not the article; sending
     * them down avoids a second fetch just to show "Volume 12 · Number II".
     */
    public record EditorialArticleDetail(Long id, String title, String abstractText, String keywords,
                                         String subjectArea, String language, String status, String doi,
                                         Long issueId, Integer pageStart, Integer pageEnd, Integer articleOrder,
                                         String issueTitle, Integer issueVolume, Integer issueNumber,
                                         OffsetDateTime submittedAt, Instant createdAt,
                                         List<AuthorDto> authors, List<FileDto> files,
                                         List<AssignmentDto> assignments, List<EditorReview> reviews,
                                         List<StatusEvent> history) {}

    public record AssignRequest(List<Long> reviewerIds, LocalDate dueDate) {}

    public record DecisionRequest(
            @NotBlank String decision,   // PUBLISH, REVISE, REJECT
            String note,
            Long issueId) {}

    private ReviewDtos() {}
}
