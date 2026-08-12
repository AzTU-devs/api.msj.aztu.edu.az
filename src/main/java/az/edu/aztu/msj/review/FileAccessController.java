package az.edu.aztu.msj.review;

import az.edu.aztu.msj.article.Article;
import az.edu.aztu.msj.article.ArticleFile;
import az.edu.aztu.msj.article.ArticleFileRepository;
import az.edu.aztu.msj.article.ArticleRepository;
import az.edu.aztu.msj.common.ApiException;
import az.edu.aztu.msj.security.JwtPrincipal;
import az.edu.aztu.msj.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Authenticated download of any article file, authorized per resource. */
@RestController
@Tag(name = "Article file access")
public class FileAccessController {

    private static final List<String> STAFF = List.of("ADMIN", "EDITOR_IN_CHIEF", "EDITOR");

    private final ArticleFileRepository files;
    private final ArticleRepository articles;
    private final ReviewAssignmentRepository assignments;
    private final FileStorageService storage;

    public FileAccessController(ArticleFileRepository files, ArticleRepository articles,
                               ReviewAssignmentRepository assignments, FileStorageService storage) {
        this.files = files;
        this.articles = articles;
        this.assignments = assignments;
        this.storage = storage;
    }

    @GetMapping("/api/v1/files/{fileId}/download")
    @Operation(summary = "Download an article file (author / assigned reviewer / editorial staff)")
    public ResponseEntity<Resource> download(@PathVariable Long fileId, @AuthenticationPrincipal JwtPrincipal p) {
        if (p == null) throw ApiException.unauthorized("Not authenticated");
        ArticleFile file = files.findById(fileId).orElseThrow(() -> ApiException.notFound("File"));
        Article article = articles.findById(file.getArticleId()).orElseThrow(() -> ApiException.notFound("Article"));

        boolean staff = p.roles() != null && p.roles().stream().anyMatch(STAFF::contains);
        boolean author = article.getSubmitterId().equals(p.id());
        boolean reviewer = assignments.existsByArticleIdAndReviewerId(article.getId(), p.id());
        if (!staff && !author && !reviewer) {
            throw ApiException.forbidden("Not allowed to access this file");
        }

        String key = file.getStorageKey();
        if (key != null && (key.startsWith("http://") || key.startsWith("https://"))) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, key).build();
        }
        Resource resource = storage.load(key);
        MediaType type = file.getContentType() != null
                ? MediaType.parseMediaType(file.getContentType()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalName() + "\"")
                .body(resource);
    }
}
