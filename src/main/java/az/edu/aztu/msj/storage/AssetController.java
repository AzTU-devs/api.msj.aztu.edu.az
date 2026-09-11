package az.edu.aztu.msj.storage;

import az.edu.aztu.msj.common.TextSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * Generic asset upload for the CMS (images, issue PDFs, logos …). Files land in
 * the public subtree and are served back at {@code /files/**}. Admin-only.
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
@Tag(name = "Admin — asset uploads")
public class AssetController {

    private final FileStorageService storage;
    private final SecureRandom random = new SecureRandom();

    public AssetController(FileStorageService storage) {
        this.storage = storage;
    }

    public record UploadResult(String url, String name, long size, String contentType) {}

    /**
     * Where the CMS is allowed to file an asset. An allowlist rather than a
     * character filter, because this segment names a directory in the publicly
     * served subtree and "sanitised" free text has a way of becoming "" or "..".
     */
    private static final Set<String> FOLDERS = Set.of(
            "misc", "covers", "logos", "board", "slides", "issues", "announcements", "pages");

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image or PDF; returns a public URL to store in any asset field")
    public UploadResult upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(defaultValue = "misc") String folder) {
        // Contents decide the type, and the stored name is built from that — the
        // client's filename is kept for display only and never touches the path.
        UploadPolicy.Accepted accepted = UploadPolicy.validate(file, UploadPolicy.Kind.ASSET);

        String safeFolder = folder == null ? "misc" : folder.trim().toLowerCase(Locale.ROOT);
        if (!FOLDERS.contains(safeFolder)) safeFolder = "misc";

        byte[] rnd = new byte[16];
        random.nextBytes(rnd);
        String key = "public/uploads/" + safeFolder + "/"
                + HexFormat.of().formatHex(rnd) + accepted.extension();
        storage.store(file, key);

        // served by the /files/** resource handler (maps to <storage>/public)
        String url = "/files/" + key.substring("public/".length());
        String original = TextSanitizer.shortText(file.getOriginalFilename());
        return new UploadResult(url, original == null ? "file" : original,
                file.getSize(), accepted.contentType());
    }
}
