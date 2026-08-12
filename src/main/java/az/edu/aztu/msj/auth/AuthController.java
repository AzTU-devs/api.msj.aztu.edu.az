package az.edu.aztu.msj.auth;

import az.edu.aztu.msj.security.JwtPrincipal;
import az.edu.aztu.msj.user.User;
import az.edu.aztu.msj.user.UserRepository;
import az.edu.aztu.msj.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest req, HttpServletRequest http) {
        return authService.login(req, http.getHeader("User-Agent"), clientIp(http));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new author account")
    public AuthDtos.TokenResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req, HttpServletRequest http) {
        return authService.register(req, http.getHeader("User-Agent"), clientIp(http));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token")
    public AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest req, HttpServletRequest http) {
        return authService.refresh(req.refreshToken(), http.getHeader("User-Agent"), clientIp(http));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke all refresh tokens for the current user")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal JwtPrincipal principal) {
        if (principal != null) {
            authService.logout(principal.id());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Return the current authenticated user's profile")
    public AuthDtos.UserSummary me(@AuthenticationPrincipal JwtPrincipal principal) {
        if (principal == null) {
            throw ApiException.unauthorized("Not authenticated");
        }
        User user = users.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("Not authenticated"));
        return new AuthDtos.UserSummary(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), List.copyOf(user.getRoles()));
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
