package az.edu.aztu.msj.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "Admin reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Full metrics roll-up: totals, per-article, per-issue and reader countries")
    public ReportDtos.MetricsExport metrics() {
        return service.export();
    }
}
