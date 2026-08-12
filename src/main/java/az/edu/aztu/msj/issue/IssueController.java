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

    public record IssueDto(Long id, Integer volume, Integer number, Integer year, String title,
                           String description, String coverUrl, String fullPdfUrl, String doi,
                           String slug, LocalDate publishedAt) {
        static IssueDto from(Issue i) {
            return new IssueDto(i.getId(), i.getVolume(), i.getNumber(), i.getYear(), i.getTitle(),
                    i.getDescription(), i.getCoverUrl(), i.getFullPdfUrl(), i.getDoi(),
                    i.getSlug(), i.getPublishedAt());
        }
    }

    public record IssueWithArticles(IssueDto issue, List<ArticleDtos.ArticleSummary> articles) {}

    @GetMapping
    @Operation(summary = "List published issues (the archive)")
    public List<IssueDto> list() {
        return issues.findByStatusOrderBySortOrderAsc("PUBLISHED").stream()
                .map(IssueDto::from)
                .toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get an issue and its table of contents by slug")
    public IssueWithArticles get(@PathVariable String slug) {
        Issue issue = issues.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Issue"));
        return new IssueWithArticles(IssueDto.from(issue), articleService.listByIssue(issue.getId()));
    }
}
