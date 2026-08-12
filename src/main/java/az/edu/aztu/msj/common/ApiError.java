package az.edu.aztu.msj.common;

import java.time.Instant;
import java.util.List;

/** Uniform error body returned by {@link GlobalExceptionHandler}. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {}
}
