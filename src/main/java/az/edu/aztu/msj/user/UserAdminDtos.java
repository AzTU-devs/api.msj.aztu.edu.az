package az.edu.aztu.msj.user;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Payloads for the super-admin Users & Roles screen. */
public final class UserAdminDtos {

    public record UserRow(
            Long id,
            String email,
            String firstName,
            String lastName,
            String status,
            List<String> roles) {}

    public record RolesUpdate(@NotNull List<String> roles) {}

    private UserAdminDtos() {}
}
