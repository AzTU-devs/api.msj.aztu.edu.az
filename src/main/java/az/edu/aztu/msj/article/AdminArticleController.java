package az.edu.aztu.msj.article;

import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Editorial: add a published article directly (separate from the author-submission workflow). */
@RestController
@RequestMapping("/api/v1/admin/articles")
@Tag(name = "Admin")
public class AdminArticleController {

    private final AdminArticleService service;

    public AdminArticleController(AdminArticleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a published article directly and place it in an issue")
    public AdminArticleDtos.CreatedResponse create(@Valid @RequestBody AdminArticleDtos.CreateArticleRequest req,
                                                   @AuthenticationPrincipal JwtPrincipal principal) {
        Long id = service.createPublished(req, principal.id());
        return new AdminArticleDtos.CreatedResponse(id);
    }
}
