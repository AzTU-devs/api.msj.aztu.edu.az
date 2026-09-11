package az.edu.aztu.msj.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
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

    /**
     * Everything the directory holds about one account.
     *
     * <p>The password hash is deliberately absent: an admin never needs it, and a
     * hash that reaches a browser is a hash that reaches anyone who can read that
     * browser's memory, cache or devtools log.
     */
    public record UserDetail(
            Long id,
            String email,
            String firstName,
            String lastName,
            String middleName,
            String title,
            String degree,
            String position,
            String affiliation,
            String country,
            String city,
            String postalCode,
            String phone,
            String orcid,
            String scopusId,
            String websiteUrl,
            String avatarUrl,
            String bio,
            String status,
            boolean emailVerified,
            OffsetDateTime emailVerifiedAt,
            boolean availableReviewer,
            String preferredLocale,
            OffsetDateTime lastLoginAt,
            Instant createdAt,
            Instant updatedAt,
            List<String> roles,
            long submissionCount) {}

    public record RolesUpdate(@NotNull List<String> roles) {}

    /** ACTIVE re-enables an account; BLOCKED locks it out immediately. */
    public record StatusUpdate(@NotBlank String status) {}

    private UserAdminDtos() {}
}
