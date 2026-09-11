package az.edu.aztu.msj.user;

import az.edu.aztu.msj.article.ArticleRepository;
import az.edu.aztu.msj.auth.RefreshTokenRepository;
import az.edu.aztu.msj.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Super-admin user directory: roles, account status, and removal. */
@Service
public class UserAdminService {

    /** Roles a super-admin may grant. */
    public static final Set<String> ASSIGNABLE = Set.of(
            "ADMIN", "EDITOR_IN_CHIEF", "EDITOR", "REVIEWER", "AUTHOR");

    /** Account states an admin may set. Anything else is rejected. */
    public static final Set<String> SETTABLE_STATUSES = Set.of("ACTIVE", "BLOCKED", "PENDING");

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final ArticleRepository articles;

    public UserAdminService(UserRepository users, RefreshTokenRepository refreshTokens,
                            ArticleRepository articles) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.articles = articles;
    }

    @Transactional(readOnly = true)
    public List<UserAdminDtos.UserRow> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getId).reversed())
                .map(this::toRow)
                .toList();
    }

    /** The full profile behind one row, for the admin's detail panel. */
    @Transactional(readOnly = true)
    public UserAdminDtos.UserDetail detail(Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        return new UserAdminDtos.UserDetail(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getMiddleName(),
                u.getTitle(), u.getDegree(), u.getPosition(), u.getAffiliation(), u.getCountry(),
                u.getCity(), u.getPostalCode(), u.getPhone(), u.getOrcid(), u.getScopusId(),
                u.getWebsiteUrl(), u.getAvatarUrl(), u.getBio(), u.getStatus(),
                u.getEmailVerifiedAt() != null, u.getEmailVerifiedAt(),
                u.isAvailableReviewer(), u.getPreferredLocale(), u.getLastLoginAt(),
                u.getCreatedAt(), u.getUpdatedAt(), List.copyOf(u.getRoles()),
                articles.countBySubmitterId(u.getId()));
    }

    @Transactional
    public UserAdminDtos.UserRow setRoles(Long id, List<String> requested, Long actorId) {
        Set<String> roles = new LinkedHashSet<>();
        for (String r : requested) {
            String role = r == null ? "" : r.trim().toUpperCase(Locale.ROOT);
            if (!ASSIGNABLE.contains(role)) throw ApiException.badRequest("Unknown role: " + r);
            roles.add(role);
        }
        if (roles.isEmpty()) throw ApiException.badRequest("A user must have at least one role");
        // guard against locking yourself out of the admin
        if (id.equals(actorId) && !roles.contains("ADMIN")) {
            throw ApiException.badRequest("You cannot remove your own admin role");
        }
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getRoles().contains("ADMIN") && !roles.contains("ADMIN")) {
            requireAnotherAdminRemains(user, "demote");
        }
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        users.save(user);
        return toRow(user);
    }

    /**
     * Blocks or re-activates an account.
     *
     * <p>Blocking revokes the refresh tokens as well as flipping the status, and
     * {@code JwtAuthenticationFilter} re-checks the status on every request — so a
     * blocked user is out immediately rather than when their access token happens
     * to expire.
     */
    @Transactional
    public UserAdminDtos.UserRow setStatus(Long id, String requested, Long actorId) {
        String status = requested == null ? "" : requested.trim().toUpperCase(Locale.ROOT);
        if (!SETTABLE_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unknown status: " + requested);
        }
        if (id.equals(actorId) && !"ACTIVE".equals(status)) {
            throw ApiException.badRequest("You cannot block your own account");
        }
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (!"ACTIVE".equals(status) && user.getRoles().contains("ADMIN")) {
            requireAnotherAdminRemains(user, "block");
        }

        user.setStatus(status);
        users.save(user);
        if (!"ACTIVE".equals(status)) {
            refreshTokens.revokeAllForUser(user.getId(), Instant.now());
        }
        return toRow(user);
    }

    /**
     * Permanently removes an account.
     *
     * <p>Refused for anyone who has submitted a manuscript: the editorial record
     * has to stay intact and attributable, and a deleted submitter would orphan
     * it. Blocking is the right tool there, so the error says so.
     */
    @Transactional
    public void delete(Long id, Long actorId) {
        if (id.equals(actorId)) throw ApiException.badRequest("You cannot delete your own account");

        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (user.getRoles().contains("ADMIN")) {
            requireAnotherAdminRemains(user, "delete");
        }

        long submissions = articles.countBySubmitterId(id);
        if (submissions > 0) {
            throw ApiException.badRequest("This account has " + submissions
                    + " submission(s) and cannot be deleted without breaking the editorial record. "
                    + "Block the account instead.");
        }

        refreshTokens.revokeAllForUser(id, Instant.now());
        try {
            users.delete(user);
            users.flush();
        } catch (DataIntegrityViolationException e) {
            // Reviews, assignments or status-history rows still point at this user.
            throw ApiException.badRequest("This account is referenced by the review history "
                    + "and cannot be deleted. Block the account instead.");
        }
    }

    /** Stops the last usable administrator from being demoted, blocked or deleted. */
    private void requireAnotherAdminRemains(User target, String verb) {
        boolean anotherRemains = users.findByRole("ADMIN").stream()
                .anyMatch(u -> !u.getId().equals(target.getId()));
        if (!anotherRemains) {
            throw ApiException.badRequest(
                    "This is the only active administrator — you cannot " + verb + " it. "
                            + "Grant the ADMIN role to another account first.");
        }
    }

    private UserAdminDtos.UserRow toRow(User u) {
        return new UserAdminDtos.UserRow(u.getId(), u.getEmail(), u.getFirstName(),
                u.getLastName(), u.getStatus(), List.copyOf(u.getRoles()));
    }
}
