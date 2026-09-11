package az.edu.aztu.msj.security;

import az.edu.aztu.msj.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AppProperties props;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, AppProperties props) {
        this.jwtFilter = jwtFilter;
        this.props = props;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                // Safe to disable: authentication is a Bearer token read from a
                // header, never an ambient cookie, so there is nothing for a
                // cross-site form post to ride on.
                .csrf(csrf -> csrf.disable())
                .headers(h -> h
                        .contentTypeOptions(withDefaults -> {})
                        .frameOptions(f -> f.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .referrerPolicy(r -> r.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                // No global CSP: Swagger UI is served from this origin and needs to
                // run its own scripts. The policy that matters is the strict one
                // UploadedContentHeadersFilter puts on /files/**, where attacker-
                // supplied bytes actually live.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public reads
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/public/**",
                                "/api/v1/home",
                                "/api/v1/articles/**",
                                "/api/v1/issues/**",
                                "/api/v1/board/**",
                                "/api/v1/pages/**",
                                "/api/v1/announcements/**",
                                "/api/v1/settings").permitAll()
                        // metrics recording + contact form are open to anonymous visitors
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/metrics/events",
                                "/api/v1/public/contact").permitAll()
                        // auth + docs + health + uploaded public assets
                        .requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                        // user & role management is super-admin only
                        .requestMatchers("/api/v1/admin/users/**")
                        .hasRole("ADMIN")
                        // admin / editorial area
                        .requestMatchers("/api/v1/admin/**")
                        .hasAnyRole("ADMIN", "EDITOR_IN_CHIEF", "EDITOR")
                        // reviewer console
                        .requestMatchers("/api/v1/reviewer/**")
                        .hasAnyRole("REVIEWER", "EDITOR", "EDITOR_IN_CHIEF", "ADMIN")
                        // author submissions, personal area, authenticated file access — any signed-in user
                        // (ownership is enforced per-resource in the services)
                        .requestMatchers("/api/v1/submissions/**", "/api/v1/me/**", "/api/v1/files/**")
                        .authenticated()
                        // everything else needs a token
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return provider::authenticate;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> configured = Arrays.stream(props.cors().allowedOrigins().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        // Origin PATTERNS (not plain origins) so exact domains still match as
        // patterns. A bare "*" is dropped: combined with allowCredentials(true)
        // Spring reflects whatever Origin was sent straight back, which turns
        // every site on the internet into a permitted caller. When that is all
        // that was configured, fall back to the journal's own front ends.
        List<String> origins = configured.stream().filter(o -> !o.equals("*")).toList();
        if (origins.size() != configured.size()) {
            log.warn("msj.cors.allowed-origins contained \"*\"; ignoring it — a wildcard cannot be "
                    + "combined with credentialed CORS. Configure the real front-end origins.");
        }
        if (origins.isEmpty()) {
            origins = Stream.of(props.frontend().webUrl(), props.frontend().adminUrl())
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList();
            log.warn("No usable CORS origins configured; defaulting to {}", origins);
        }
        cfg.setAllowedOriginPatterns(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
