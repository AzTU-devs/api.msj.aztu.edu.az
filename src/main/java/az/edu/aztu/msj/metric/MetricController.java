package az.edu.aztu.msj.metric;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics")
public class MetricController {

    private final MetricService service;

    public MetricController(MetricService service) {
        this.service = service;
    }

    public record EventRequest(@NotNull Long articleId, @NotNull String type) {}

    @PostMapping("/events")
    @Operation(summary = "Record an anonymous view/download event (deduplicated per session/day)")
    public ResponseEntity<Void> record(@RequestBody EventRequest req, HttpServletRequest http) {
        service.record(req.articleId(), req.type(), clientIp(http),
                http.getHeader("User-Agent"), http.getHeader("Referer"), country(http));
        return ResponseEntity.accepted().build();
    }

    /**
     * Reader country, from the CDN rather than a GeoIP database. Cloudflare
     * fronts the public site and sets CF-IPCountry on every request; nginx and
     * the Next proxy pass it through untouched. CF-Ipcountry is the fallback
     * spelling some intermediaries normalise to.
     */
    private String country(HttpServletRequest req) {
        String c = req.getHeader("CF-IPCountry");
        if (c == null || c.isBlank()) c = req.getHeader("X-Country-Code");
        return c;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
