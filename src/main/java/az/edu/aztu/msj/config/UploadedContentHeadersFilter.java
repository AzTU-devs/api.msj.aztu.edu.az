package az.edu.aztu.msj.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Neutralises anything served out of the uploads subtree at {@code /files/**}.
 *
 * <p>{@link az.edu.aztu.msj.storage.UploadPolicy} already guarantees only real
 * PDFs and images are stored there, so this is the second line rather than the
 * first — but uploaded bytes served from the API's own origin are exactly the
 * shape of a stored-XSS bug, and the cost of assuming the first line never fails
 * is the whole origin.
 *
 * <ul>
 *   <li>{@code nosniff} stops a browser from deciding a mislabelled file is HTML.</li>
 *   <li>The {@code sandbox} CSP strips scripting, plugins and same-origin
 *       privileges from whatever does get rendered — including JavaScript
 *       embedded in a genuine PDF.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UploadedContentHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; object-src 'none'; sandbox");
        response.setHeader("X-Frame-Options", "DENY");
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/files/");
    }
}
