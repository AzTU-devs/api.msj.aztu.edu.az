package az.edu.aztu.msj.admin;

import az.edu.aztu.msj.common.PageResponse;
import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Editorial dashboard counters")
    public AdminDtos.Dashboard dashboard() {
        return service.dashboard();
    }

    @GetMapping("/articles")
    @Operation(summary = "List submissions for the editorial desk")
    public PageResponse<AdminDtos.ArticleRow> articles(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listArticles(status, page, size);
    }

    @PatchMapping("/articles/{id}/status")
    @Operation(summary = "Advance an article through the editorial workflow")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @RequestBody AdminDtos.StatusUpdateRequest req,
                                             @AuthenticationPrincipal JwtPrincipal principal) {
        service.updateStatus(id, req.status(), req.comment(), principal == null ? null : principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/metrics/overview")
    @Operation(summary = "Aggregate metrics: totals, time series, and top articles")
    public AdminDtos.MetricsOverview metrics(@RequestParam(defaultValue = "30") int days) {
        return service.metricsOverview(days);
    }
}
