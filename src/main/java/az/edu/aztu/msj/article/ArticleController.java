package az.edu.aztu.msj.article;

import az.edu.aztu.msj.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/articles")
@Tag(name = "Articles (public)")
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List published articles (search, filter by issue/subject, paginated)")
    public PageResponse<ArticleDtos.ArticleSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long issueId,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPublished(q, issueId, subject, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published article by id")
    public ArticleDtos.ArticleDetail get(@PathVariable Long id) {
        return service.getPublishedById(id);
    }
}
