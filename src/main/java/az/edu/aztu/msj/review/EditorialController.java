package az.edu.aztu.msj.review;

import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Editorial (admin)")
public class EditorialController {

    private final EditorialService service;

    public EditorialController(EditorialService service) {
        this.service = service;
    }

    private Long actor(JwtPrincipal p) {
        return p == null ? null : p.id();
    }

    @GetMapping("/reviewers")
    @Operation(summary = "List users who can review")
    public List<ReviewDtos.ReviewerUser> reviewers() {
        return service.reviewers();
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "Full editorial view: metadata, files, assignments, reviews, history")
    public ReviewDtos.EditorialArticleDetail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/articles/{id}/assign")
    @Operation(summary = "Assign reviewers to a submission")
    public ResponseEntity<Void> assign(@PathVariable Long id, @RequestBody ReviewDtos.AssignRequest req,
                                       @AuthenticationPrincipal JwtPrincipal p) {
        service.assign(id, req, actor(p));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/articles/{id}/assignments/{assignmentId}")
    @Operation(summary = "Cancel a review assignment")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @PathVariable Long assignmentId) {
        service.cancelAssignment(id, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/articles/{id}/decision")
    @Operation(summary = "Editor-in-Chief decision: publish, request revision, or reject")
    @PreAuthorize("hasAnyRole('EDITOR_IN_CHIEF','ADMIN')")
    public ResponseEntity<Void> decide(@PathVariable Long id, @Valid @RequestBody ReviewDtos.DecisionRequest req,
                                       @AuthenticationPrincipal JwtPrincipal p) {
        service.decide(id, req, actor(p));
        return ResponseEntity.noContent().build();
    }
}
