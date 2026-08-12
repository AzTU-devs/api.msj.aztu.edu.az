package az.edu.aztu.msj.storage;

import az.edu.aztu.msj.config.AppProperties;
import az.edu.aztu.msj.common.ApiException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

/**
 * Local-filesystem object storage. Keys are relative paths under the configured
 * root (e.g. {@code articles/42/manuscript.pdf}). Swap for an S3 implementation
 * behind this same interface in production (config: {@code msj.storage.provider}).
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(AppProperties props) {
        this.root = Paths.get(props.storage().localPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage root " + root, e);
        }
    }

    public String store(MultipartFile file, String key) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return key;
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to store file: " + e.getMessage());
        }
    }

    public String store(byte[] bytes, String key) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return key;
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to store bytes: " + e.getMessage());
        }
    }

    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    public Resource load(String key) {
        try {
            Resource res = new UrlResource(resolve(key).toUri());
            if (!res.exists() || !res.isReadable()) {
                throw ApiException.notFound("File");
            }
            return res;
        } catch (Exception e) {
            throw ApiException.notFound("File");
        }
    }

    /** Guard against path traversal — keys must stay under the root. */
    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw ApiException.badRequest("Illegal storage key");
        }
        return p;
    }
}
