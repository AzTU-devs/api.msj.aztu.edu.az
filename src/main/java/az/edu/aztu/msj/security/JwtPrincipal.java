package az.edu.aztu.msj.security;

import java.util.List;

/** Immutable authenticated principal derived from a validated JWT. */
public record JwtPrincipal(Long id, String email, List<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
