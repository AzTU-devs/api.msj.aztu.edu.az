package az.edu.aztu.msj.user;

import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace a user's roles")
    public UserAdminDtos.UserRow setRoles(@PathVariable Long id,
                                          @Valid @RequestBody UserAdminDtos.RolesUpdate req,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        return service.setRoles(id, req.roles(), principal.id());
    }
}
