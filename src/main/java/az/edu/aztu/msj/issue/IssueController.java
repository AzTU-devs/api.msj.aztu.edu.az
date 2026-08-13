package az.edu.aztu.msj.issue;

import az.edu.aztu.msj.article.ArticleDtos;
import az.edu.aztu.msj.article.ArticleService;
import az.edu.aztu.msj.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/issues")
@Tag(name = "Issues (public)")
public class IssueController {

    private final IssueRepository issues;
    private final ArticleService articleService;

    public IssueController(IssueRepository issues, ArticleService articleService) {
        this.issues = issues;
        this.articleService = articleService;
    }

    public record IssueDto(Long id, Integer volume, Integer number, String numberRoman, Integer year, String title,
                           String description, String coverUrl, String fullPdfUrl, String doi, String slug,
                           String status, LocalDate publishedAt, LocalDate submissionDeadline) {
        static IssueDto from(Issue i) {
            return new IssueDto(i.getId(), i.getVolume(), i.getNumber(), roman(i.getNumber()), i.getYear(),
                    i.getTitle(), i.getDescription(), i.getCoverUrl(), i.getFullPdfUrl(), i.getDoi(),
                    i.getSlug(), i.getStatus(), i.getPublishedAt(), i.getSubmissionDeadline());
        }
    }

    /** Minimal shape for the author's "submit to section" dropdown. */
    public record OpenSection(Long id, Integer year, Integer number, String numberRoman,
                              String title, LocalDate submissionDeadline) {
        static OpenSection from(Issue i) {
            return new OpenSection(i.getId(), i.getYear(), i.getNumber(), roman(i.getNumber()),
                    i.getTitle(), i.getSubmissionDeadline());
        }
    }

    public record IssueWithArticles(IssueDto issue, List<ArticleDtos.ArticleSummary> articles) {}

    static String roman(Integer n) {
        if (n == null) return "";
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; default -> String.valueOf(n);
        };
    }

    @GetMapping
    @Operation(summary = "List public issues (published + archived) for the archive")
    public List<IssueDto> list() {
        return issues.findByStatusInOrderByYearDescNumberAsc(List.of("PUBLISHED", "ARCHIVED")).stream()
                .map(IssueDto::from).toList();
    }

    @GetMapping("/open")
    @Operation(summary = "Sections currently accepting submissions (deadline not passed)")
    public List<OpenSection> open() {
        return issues.findOpenForSubmission(LocalDate.now()).stream().map(OpenSection::from).toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get an issue and its published table of contents by slug")
    public IssueWithArticles get(@PathVariable String slug) {
        Issue issue = issues.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Issue"));
        return new IssueWithArticles(IssueDto.from(issue), articleService.listByIssue(issue.getId()));
    }
}
