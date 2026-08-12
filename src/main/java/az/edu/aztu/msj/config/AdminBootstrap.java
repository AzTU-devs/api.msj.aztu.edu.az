package az.edu.aztu.msj.config;

import az.edu.aztu.msj.user.User;
import az.edu.aztu.msj.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** Ensures a first administrator account exists (idempotent), for fresh environments. */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${MSJ_ADMIN_EMAIL:}") String adminEmail,
                          @Value("${MSJ_ADMIN_PASSWORD:}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (users.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setStatus("ACTIVE");
        admin.setEmailVerifiedAt(OffsetDateTime.now());
        admin.getRoles().add("ADMIN");
        users.save(admin);
        log.info("Bootstrapped initial admin account: {}", adminEmail);
    }
}
