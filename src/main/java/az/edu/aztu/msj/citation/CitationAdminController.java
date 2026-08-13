package az.edu.aztu.msj.citation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Editorial: trigger a Crossref citation refresh on demand (also runs weekly automatically). */
@RestController
@RequestMapping("/api/v1/admin/citations")
@Tag(name = "Admin")
public class CitationAdminController {

    private final CitationService service;

    public CitationAdminController(CitationService service) {
        this.service = service;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Fetch citation counts from Crossref for all published articles with a DOI")
    public CitationService.Result refresh() {
        return service.refreshAll();
    }

    @PostMapping("/sync")
    @Operation(summary = "Import missing DOIs from Crossref (by title), then refresh citation counts")
    public CitationService.SyncResult sync() {
        // separate calls so matchDois() runs inside its own proxied transaction
        CitationService.MatchResult dois = service.matchDois();
        CitationService.Result citations = service.refreshAll();
        return new CitationService.SyncResult(dois, citations);
    }
}
