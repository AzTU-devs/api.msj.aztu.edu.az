package az.edu.aztu.msj.user;

import az.edu.aztu.msj.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Super-admin user directory + role assignment. */
@Service
public class UserAdminService {

    /** Roles a super-admin may grant. */
    public static final Set<String> ASSIGNABLE = Set.of(
            "ADMIN", "EDITOR_IN_CHIEF", "EDITOR", "REVIEWER", "AUTHOR");

    private final UserRepository users;

    public UserAdminService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserAdminDtos.UserRow> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getId).reversed())
                .map(u -> new UserAdminDtos.UserRow(u.getId(), u.getEmail(), u.getFirstName(),
                        u.getLastName(), u.getStatus(), List.copyOf(u.getRoles())))
                .toList();
    }

    @Transactional
    public UserAdminDtos.UserRow setRoles(Long id, List<String> requested, Long actorId) {
        Set<String> roles = new LinkedHashSet<>();
        for (String r : requested) {
            String role = r == null ? "" : r.trim().toUpperCase();
            if (!ASSIGNABLE.contains(role)) throw ApiException.badRequest("Unknown role: " + r);
            roles.add(role);
        }
        if (roles.isEmpty()) throw ApiException.badRequest("A user must have at least one role");
        // guard against locking yourself out of the admin
        if (id.equals(actorId) && !roles.contains("ADMIN")) {
            throw ApiException.badRequest("You cannot remove your own admin role");
        }
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        users.save(user);
        return new UserAdminDtos.UserRow(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getStatus(), List.copyOf(user.getRoles()));
    }
}
