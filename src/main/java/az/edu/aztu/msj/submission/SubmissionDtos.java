package az.edu.aztu.msj.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public final class SubmissionDtos {

    public record AuthorInput(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String email,
            String affiliation,
            String country,
            String orcid,
            boolean corresponding) {}

    public record SubmissionInput(
            @NotBlank String title,
            String abstractText,
            String keywords,
            String subjectArea,
            String language,
            Long issueId,                     // target section the author submits to
            @NotEmpty @Valid List<AuthorInput> authors) {}

    public record FileDto(Long id, String kind, String originalName, Long sizeBytes, String contentType, Instant createdAt) {}

    public record AuthorDto(String firstName, String lastName, String email, String affiliation,
                            String country, String orcid, boolean corresponding) {}

    public record StatusEvent(String fromStatus, String toStatus, String comment, Instant at) {}

    /** What the author is allowed to see of a review: recommendation + comments-to-author only. */
    public record ReviewForAuthor(String recommendation, String commentsToAuthor, OffsetDateTime submittedAt) {}

    public record SubmissionSummary(Long id, String title, String status, String subjectArea,
                                    OffsetDateTime submittedAt, Instant updatedAt) {}

    public record SubmissionDetail(Long id, String title, String abstractText, String keywords,
                                   String subjectArea, String language, String status, String doi,
                                   Long issueId, String issueTitle,
                                   OffsetDateTime submittedAt, Instant createdAt, Instant updatedAt,
                                   List<AuthorDto> authors, List<FileDto> files,
                                   List<StatusEvent> history, List<ReviewForAuthor> reviews,
                                   String editorNote, boolean canEdit) {}

    private SubmissionDtos() {}
}
