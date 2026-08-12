package az.edu.aztu.msj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly-typed binding for all {@code msj.*} configuration. */
@ConfigurationProperties(prefix = "msj")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Storage storage,
        Mail mail,
        Frontend frontend
) {
    public record Jwt(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays, String issuer) {}
    public record Cors(String allowedOrigins) {}
    public record Storage(String provider, String localPath, String publicBaseUrl) {}
    public record Mail(String from, boolean enabled) {}
    public record Frontend(String webUrl, String adminUrl) {}
}
