package az.edu.aztu.msj.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request/response payloads for the authentication endpoints. */
public final class AuthDtos {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            String affiliation,
            String country,
            String orcid) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserSummary user) {}

    public record UserSummary(
            Long id,
            String email,
            String firstName,
            String lastName,
            List<String> roles) {}

    private AuthDtos() {}
}
