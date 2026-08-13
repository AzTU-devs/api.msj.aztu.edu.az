package az.edu.aztu.msj.article;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Payloads for editors adding a published article directly (no author-submission flow). */
public final class AdminArticleDtos {

    public record AuthorInput(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String email,
            String affiliation,
            String country,
            String orcid,
            boolean corresponding) {}

    public record CreateArticleRequest(
            @NotBlank String title,
            String abstractText,
            String keywords,
            String subjectArea,
            String language,
            String doi,
            Long issueId,
            Integer pageStart,
            Integer pageEnd,
            Integer articleOrder,
            List<AuthorInput> authors) {}

    public record CreatedResponse(Long id) {}

    private AdminArticleDtos() {}
}
