package az.edu.aztu.msj.article;

import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.common.TextSanitizer;
import az.edu.aztu.msj.metric.MetricService;
import az.edu.aztu.msj.security.JwtPrincipal;
import az.edu.aztu.msj.storage.FileStorageService;
import az.edu.aztu.msj.storage.UploadPolicy;
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
import java.util.Locale;
import java.util.Set;

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
        // The public download endpoint redirects to whatever is stored here, so
        // anything that is not a plain http(s) URL is refused.
        String url = TextSanitizer.httpUrl(req.url());
        if (url == null) throw ApiException.badRequest("A valid http(s) URL is required");
        int version = files.findByArticleIdOrderByCreatedAtDesc(id).stream()
                .filter(f -> f.getKind().equals("PUBLISHED_PDF"))
                .map(ArticleFile::getVersion).max(Integer::compareTo).orElse(0) + 1;
        ArticleFile af = new ArticleFile();
        af.setArticleId(id);
        af.setKind("PUBLISHED_PDF");
        af.setOriginalName(TextSanitizer.shortText(url.substring(url.lastIndexOf('/') + 1)));
        af.setStorageKey(url);
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

    /** The kinds an editor may attach. Unvalidated, this reached the storage path. */
    static final Set<String> KINDS = Set.of(
            "PUBLISHED_PDF", "CAMERA_READY", "MANUSCRIPT", "REVISION", "SUPPLEMENTARY", "COVER_LETTER");

    @PostMapping(value = "/api/v1/admin/articles/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file (manuscript / published PDF / …) for an article")
    public ArticleFile upload(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(defaultValue = "PUBLISHED_PDF") String kind,
                              @AuthenticationPrincipal JwtPrincipal principal) {
        articles.findById(id).orElseThrow(() -> ApiException.notFound("Article"));

        String k = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        if (!KINDS.contains(k)) throw ApiException.badRequest("Unknown file kind: " + kind);

        UploadPolicy.Accepted accepted = UploadPolicy.validate(file, UploadPolicy.Kind.DOCUMENT);

        int version = files.findByArticleIdOrderByCreatedAtDesc(id).stream()
                .filter(f -> f.getKind().equals(k))
                .map(ArticleFile::getVersion).max(Integer::compareTo).orElse(0) + 1;

        // Key built entirely from server-side values: a validated kind, a numeric
        // id and version, and an extension derived from the file's own bytes.
        String key = "articles/" + id + "/" + k.toLowerCase(Locale.ROOT) + "-v" + version + accepted.extension();
        storage.store(file, key);

        String original = TextSanitizer.shortText(file.getOriginalFilename());

        ArticleFile af = new ArticleFile();
        af.setArticleId(id);
        af.setKind(k);
        af.setOriginalName(original == null ? "upload" + accepted.extension() : original);
        af.setStorageKey(key);
        af.setContentType(accepted.contentType());
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
