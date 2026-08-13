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
                .csrf(csrf -> csrf.disable())
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = Arrays.stream(props.cors().allowedOrigins().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        // Origin PATTERNS (not plain origins) so a wildcard "*" is permitted even with
        // allowCredentials(true) — Spring reflects the request origin back instead of
        // emitting a literal "*". Exact domains still match as patterns.
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
