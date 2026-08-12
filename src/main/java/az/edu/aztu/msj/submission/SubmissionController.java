package az.edu.aztu.msj.submission;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "Author submissions")
public class SubmissionController {

    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    private Long uid(JwtPrincipal p) {
        if (p == null) throw ApiException.unauthorized("Not authenticated");
        return p.id();
    }

    @GetMapping("/api/v1/me/submissions")
    @Operation(summary = "List my submissions")
    public List<SubmissionDtos.SubmissionSummary> mine(@AuthenticationPrincipal JwtPrincipal p) {
        return service.listMine(uid(p));
    }

    @PostMapping("/api/v1/submissions")
    @Operation(summary = "Create a new draft submission")
    public SubmissionDtos.SubmissionDetail create(@AuthenticationPrincipal JwtPrincipal p,
                                                  @Valid @RequestBody SubmissionDtos.SubmissionInput in) {
        return service.create(uid(p), in);
    }

    @GetMapping("/api/v1/submissions/{id}")
    @Operation(summary = "Get one of my submissions")
    public SubmissionDtos.SubmissionDetail get(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id) {
        return service.getMine(uid(p), id);
    }

    @PutMapping("/api/v1/submissions/{id}")
    @Operation(summary = "Update a draft or revision")
    public SubmissionDtos.SubmissionDetail update(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id,
                                                  @Valid @RequestBody SubmissionDtos.SubmissionInput in) {
        return service.update(uid(p), id, in);
    }

    @PostMapping(value = "/api/v1/submissions/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a manuscript / supplementary / cover-letter file")
    public SubmissionDtos.FileDto upload(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "MANUSCRIPT") String kind) {
        return service.upload(uid(p), id, file, kind);
    }

    @DeleteMapping("/api/v1/submissions/{id}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@AuthenticationPrincipal JwtPrincipal p,
                                           @PathVariable Long id, @PathVariable Long fileId) {
        service.deleteFile(uid(p), id, fileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/submissions/{id}/submit")
    @Operation(summary = "Submit the manuscript for review (or resubmit after revision)")
    public SubmissionDtos.SubmissionDetail submit(@AuthenticationPrincipal JwtPrincipal p, @PathVariable Long id) {
        return service.submit(uid(p), id);
    }
}
