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

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Guard against path traversal.
     *
     * <p>A {@code startsWith(root)} check alone is not enough: it accepts
     * {@code articles/1/../../public/uploads/x}, which normalises back under the
     * root while landing in a subtree the caller had no business writing to — the
     * publicly served one, for instance. So the key's <em>shape</em> is validated
     * before it is resolved, and traversal segments are refused outright.
     */
    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("Illegal storage key");
        }
        if (key.indexOf('\0') >= 0 || key.indexOf('\\') >= 0) {
            throw ApiException.badRequest("Illegal storage key");
        }
        if (key.startsWith("/") || key.contains("//")) {
            throw ApiException.badRequest("Illegal storage key");
        }
        for (String segment : key.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw ApiException.badRequest("Illegal storage key");
            }
            if (!SAFE_SEGMENT.matcher(segment).matches()) {
                throw ApiException.badRequest("Illegal storage key");
            }
        }
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw ApiException.badRequest("Illegal storage key");
        }
        return p;
    }

    /** Conservative on purpose — every key we generate is machine-built. */
    private static final java.util.regex.Pattern SAFE_SEGMENT =
            java.util.regex.Pattern.compile("[A-Za-z0-9._-]{1,128}");
}
