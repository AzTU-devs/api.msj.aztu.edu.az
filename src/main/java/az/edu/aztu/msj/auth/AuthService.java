package az.edu.aztu.msj.auth;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.config.AppProperties;
import az.edu.aztu.msj.security.JwtService;
import az.edu.aztu.msj.user.User;
import az.edu.aztu.msj.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AppProperties props) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req, String userAgent, String ip) {
        User user = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw ApiException.forbidden("Account is not active");
        }
        user.setLastLoginAt(OffsetDateTime.now());
        return issueTokens(user, userAgent, ip);
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest req, String userAgent, String ip) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("Email already registered");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setAffiliation(req.affiliation());
        user.setCountry(req.country());
        user.setOrcid(req.orcid());
        user.setStatus("ACTIVE"); // NOTE: switch to PENDING + email verification before production
        user.getRoles().add("AUTHOR");
        users.save(user);
        return issueTokens(user, userAgent, ip);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(String rawRefreshToken, String userAgent, String ip) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (!stored.isActive()) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        // rotate: revoke the used token, issue a fresh pair
        stored.setRevokedAt(Instant.now());
        return issueTokens(user, userAgent, ip);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    private AuthDtos.TokenResponse issueTokens(User user, String userAgent, String ip) {
        List<String> roles = List.copyOf(user.getRoles());
        String access = jwtService.issueAccessToken(user.getId(), user.getEmail(), roles);

        String rawRefresh = randomToken();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(sha256(rawRefresh));
        rt.setExpiresAt(Instant.now().plus(props.jwt().refreshTokenTtlDays(), ChronoUnit.DAYS));
        rt.setUserAgent(userAgent);
        rt.setIpAddress(ip);
        refreshTokens.save(rt);

        var summary = new AuthDtos.UserSummary(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), roles);
        return new AuthDtos.TokenResponse(access, rawRefresh, "Bearer", jwtService.accessTtlSeconds(), summary);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
