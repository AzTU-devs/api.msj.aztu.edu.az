package az.edu.aztu.msj.article;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Payloads for editors adding a published article directly (no author-submission flow). */
public final class AdminArticleDtos {

    public record AuthorInput(
            @NotBlank @Size(max = 255) String firstName,
            @NotBlank @Size(max = 255) String lastName,
            @Size(max = 255) String email,
            @Size(max = 1000) String affiliation,
            @Size(max = 255) String country,
            @Size(max = 255) String orcid,
            boolean corresponding) {}

    public record CreateArticleRequest(
            @NotBlank @Size(max = 500) String title,
            @Size(max = 20_000) String abstractText,
            @Size(max = 1000) String keywords,
            @Size(max = 255) String subjectArea,
            @Size(max = 16) String language,
            @Size(max = 255) String doi,
            Long issueId,
            Integer pageStart,
            Integer pageEnd,
            Integer articleOrder,
            @Size(max = 50) @Valid List<AuthorInput> authors) {}

    public record CreatedResponse(Long id) {}

    private AdminArticleDtos() {}
}
