package az.edu.aztu.msj.storage;

import az.edu.aztu.msj.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.HexFormat;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image or PDF; returns a public URL to store in any asset field")
    public UploadResult upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(defaultValue = "misc") String folder) {
        if (file.isEmpty()) throw ApiException.badRequest("Empty file");
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        byte[] rnd = new byte[8];
        random.nextBytes(rnd);
        String safeFolder = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        String key = "public/uploads/" + safeFolder + "/" + HexFormat.of().formatHex(rnd) + ext;
        storage.store(file, key);
        // served by the /files/** resource handler (maps to <storage>/public)
        String url = "/files/" + key.substring("public/".length());
        return new UploadResult(url, original, file.getSize(), file.getContentType());
    }
}
