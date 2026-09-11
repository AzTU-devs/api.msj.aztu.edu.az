package az.edu.aztu.msj.user;

import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Super-admin only (gated in SecurityConfig): list users and grant/revoke roles. */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all user accounts with their roles")
    public List<UserAdminDtos.UserRow> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full profile for one account")
    public UserAdminDtos.UserDetail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace a user's roles")
    public UserAdminDtos.UserRow setRoles(@PathVariable Long id,
                                          @Valid @RequestBody UserAdminDtos.RolesUpdate req,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        return service.setRoles(id, req.roles(), principal.id());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Block or re-activate an account (takes effect immediately)")
    public UserAdminDtos.UserRow setStatus(@PathVariable Long id,
                                           @Valid @RequestBody UserAdminDtos.StatusUpdate req,
                                           @AuthenticationPrincipal JwtPrincipal principal) {
        return service.setStatus(id, req.status(), principal.id());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete an account that has no editorial history")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        service.delete(id, principal.id());
        return ResponseEntity.noContent().build();
    }
}
