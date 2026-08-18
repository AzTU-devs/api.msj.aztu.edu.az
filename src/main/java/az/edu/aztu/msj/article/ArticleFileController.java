package az.edu.aztu.msj.article;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.metric.MetricService;
import az.edu.aztu.msj.security.JwtPrincipal;
import az.edu.aztu.msj.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "Article files")
public class ArticleFileController {

    private final ArticleRepository articles;
    private final ArticleFileRepository files;
    private final FileStorageService storage;
    private final MetricService metrics;

    public ArticleFileController(ArticleRepository articles, ArticleFileRepository files,
                                 FileStorageService storage, MetricService metrics) {
        this.articles = articles;
        this.files = files;
        this.storage = storage;
        this.metrics = metrics;
    }

    // ---- public download ----
    @GetMapping("/api/v1/articles/{id}/pdf")
    @Operation(summary = "Download an article's PDF (records a download metric)")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id, HttpServletRequest http) {
        Article article = articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        ArticleFile file = files.findFirstByArticleIdAndKindOrderByVersionDesc(id, "PUBLISHED_PDF")
                .or(() -> files.findFirstByArticleIdAndKindOrderByVersionDesc(id, "MANUSCRIPT"))
                .orElseThrow(() -> ApiException.notFound("PDF"));

        metrics.record(id, "PDF_DOWNLOAD", clientIp(http), http.getHeader("User-Agent"),
                http.getHeader("Referer"), http.getHeader("CF-IPCountry"));

        // A stored PDF is served inline; an external URL is redirected to.
        String key = file.getStorageKey();
        if (key != null && (key.startsWith("http://") || key.startsWith("https://"))) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, key).build();
        }
        Resource resource = storage.load(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName(article, file) + "\"")
                .body(resource);
    }

    /** Attach a published PDF to an article by external URL (alternative to uploading a file). */
    @PostMapping("/api/v1/admin/articles/{id}/pdf-url")
    @Operation(summary = "Set an article's published PDF from an external URL")
    public ArticleFile setPdfUrl(@PathVariable Long id, @RequestBody PdfUrlRequest req,
                                 @AuthenticationPrincipal JwtPrincipal principal) {
        articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        if (req.url() == null || req.url().isBlank()) throw ApiException.badRequest("url is required");
        int version = files.findByArticleIdOrderByCreatedAtDesc(id).stream()
                .filter(f -> f.getKind().equals("PUBLISHED_PDF"))
                .map(ArticleFile::getVersion).max(Integer::compareTo).orElse(0) + 1;
        ArticleFile af = new ArticleFile();
        af.setArticleId(id);
        af.setKind("PUBLISHED_PDF");
        af.setOriginalName(req.url().substring(req.url().lastIndexOf('/') + 1));
        af.setStorageKey(req.url());
        af.setContentType("application/pdf");
        af.setVersion(version);
        af.setUploadedBy(principal == null ? null : principal.id());
        return files.save(af);
    }

    public record PdfUrlRequest(String url) {}

    // ---- admin: list & upload ----
    @GetMapping("/api/v1/admin/articles/{id}/files")
    @Operation(summary = "List files attached to an article")
    public List<ArticleFile> list(@PathVariable Long id) {
        return files.findByArticleIdOrderByCreatedAtDesc(id);
    }

    @PostMapping(value = "/api/v1/admin/articles/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file (manuscript / published PDF / …) for an article")
    public ArticleFile upload(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(defaultValue = "PUBLISHED_PDF") String kind,
                              @AuthenticationPrincipal JwtPrincipal principal) {
        articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        if (file.isEmpty()) throw ApiException.badRequest("Empty file");

        int version = files.findByArticleIdOrderByCreatedAtDesc(id).stream()
                .filter(f -> f.getKind().equals(kind))
                .map(ArticleFile::getVersion).max(Integer::compareTo).orElse(0) + 1;

        String original = file.getOriginalFilename() == null ? "upload.pdf" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String key = "articles/" + id + "/" + kind.toLowerCase() + "-v" + version + ext;
        storage.store(file, key);

        ArticleFile af = new ArticleFile();
        af.setArticleId(id);
        af.setKind(kind);
        af.setOriginalName(original);
        af.setStorageKey(key);
        af.setContentType(file.getContentType());
        af.setSizeBytes(file.getSize());
        af.setVersion(version);
        af.setUploadedBy(principal == null ? null : principal.id());
        return files.save(af);
    }

    @DeleteMapping("/api/v1/admin/articles/{id}/files/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable Long fileId) {
        files.deleteById(fileId);
        return ResponseEntity.noContent().build();
    }

    private String safeName(Article a, ArticleFile f) {
        if (a.getDoi() != null) return a.getDoi().replace('/', '_') + ".pdf";
        return "article-" + a.getId() + ".pdf";
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
