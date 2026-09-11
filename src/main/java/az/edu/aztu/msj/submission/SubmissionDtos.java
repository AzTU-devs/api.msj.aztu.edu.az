package az.edu.aztu.msj.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public final class SubmissionDtos {

    public record AuthorInput(
            @NotBlank @Size(max = 255) String firstName,
            @NotBlank @Size(max = 255) String lastName,
            @Size(max = 255) String email,
            @Size(max = 1000) String affiliation,
            @Size(max = 255) String country,
            @Size(max = 255) String orcid,
            boolean corresponding) {}

    public record SubmissionInput(
            @NotBlank @Size(max = 500) String title,
            @Size(max = 20_000) String abstractText,
            @Size(max = 1000) String keywords,
            @Size(max = 255) String subjectArea,
            @Size(max = 16) String language,
            Long issueId,                     // target section the author submits to
            // Capped: the list is replaced wholesale on every save, so an
            // unbounded one is a cheap way to fill the database.
            @NotEmpty @Size(max = 50) @Valid List<AuthorInput> authors) {}

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
