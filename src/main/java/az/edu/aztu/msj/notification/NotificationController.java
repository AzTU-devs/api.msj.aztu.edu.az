package az.edu.aztu.msj.notification;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.common.PageResponse;
import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/me/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    public record NotificationDto(Long id, String type, String title, String body,
                                  String linkUrl, boolean read, Instant createdAt) {
        static NotificationDto from(Notification n) {
            return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                    n.getLinkUrl(), n.isRead(), n.getCreatedAt());
        }
    }

    private Long uid(JwtPrincipal p) {
        if (p == null) throw ApiException.unauthorized("Not authenticated");
        return p.id();
    }

    @GetMapping
    @Operation(summary = "List my notifications")
    public PageResponse<NotificationDto> list(@AuthenticationPrincipal JwtPrincipal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        var result = repo.findByUserIdOrderByCreatedAtDesc(uid(principal),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.of(result, NotificationDto::from);
    }

    @GetMapping("/unread-count")
    public long unread(@AuthenticationPrincipal JwtPrincipal principal) {
        return repo.countByUserIdAndReadFalse(uid(principal));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal JwtPrincipal principal) {
        repo.markAllRead(uid(principal));
        return ResponseEntity.noContent().build();
    }
}
