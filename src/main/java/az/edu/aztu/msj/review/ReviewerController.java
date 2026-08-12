package az.edu.aztu.msj.review;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviewer")
@Tag(name = "Reviewer console")
public class ReviewerController {

    private final ReviewerService service;

    public ReviewerController(ReviewerService service) {
        this.service = service;
    }

    private Long uid(JwtPrincipal p) {
        if (p == null) throw ApiException.unauthorized("Not authenticated");
        return p.id();
    }

    @GetMapping("/assignments")
    @Operation(summary = "My review assignments")
    public List<ReviewDtos.AssignmentSummary> assignments(@AuthenticationPrincipal JwtPrincipal p) {
        return service.myAssignments(uid(p));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "An assignment with the manuscript and my review")
    public ReviewDtos.AssignmentDetail assignment(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id) {
        return service.getAssignment(uid(p), id);
    }

    @PostMapping("/assignments/{id}/respond")
    @Operation(summary = "Accept or decline a review invitation")
    public ResponseEntity<Void> respond(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id,
                                        @RequestParam boolean accept) {
        service.respond(uid(p), id, accept);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments/{id}/review")
    @Operation(summary = "Submit the review (rich text + recommendation)")
    public ReviewDtos.MyReview submit(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id,
                                      @Valid @RequestBody ReviewDtos.ReviewInput in) {
        return service.submitReview(uid(p), id, in);
    }
}
