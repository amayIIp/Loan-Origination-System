package com.los.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — a single class that intercepts ALL exceptions thrown
 * anywhere in the controller or service layer and converts them into a
 * consistent JSON error response.
 *
 * What is @RestControllerAdvice? (beginner explanation)
 * ──────────────────────────────────────────────────────
 * Without this class, unhandled exceptions cause Spring Boot to return its
 * default "Whitelabel Error Page" (HTML) or a plain stack-trace dump — both
 * are terrible for REST API clients. @RestControllerAdvice intercepts exceptions
 * BEFORE the default handler runs and lets us build a proper JSON response.
 *
 * The consistent error shape returned by every handler below:
 * {
 *   "timestamp": "2024-06-01T10:30:00Z",
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "LoanApplication not found with id : 'abc123'",
 *   "path":      "/api/applications/abc123",
 *   "fieldErrors": { ... }   ← only present for validation errors
 * }
 *
 * This contract is stable — the Angular frontend can always parse errors
 * using the same interface regardless of which exception was thrown.
 */
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// It applies globally to all @RestController classes in this application.
@RestControllerAdvice
// @Slf4j — Lombok: generates a static "log" field (SLF4J Logger) for structured logging.
// We use this to log all errors server-side for observability.
@Slf4j
public class GlobalExceptionHandler {

    // ── Error body builder ────────────────────────────────────────────────────
    /**
     * buildBody — creates the standard error response map used by all handlers.
     * Keeping it centralised means changing the error format requires editing ONE place.
     *
     * @param status  the HTTP status code (e.g., 404)
     * @param error   short HTTP reason phrase (e.g., "Not Found")
     * @param message detailed explanation of the error
     * @param path    the URI that triggered the error (from HttpServletRequest)
     * @return a LinkedHashMap preserving field insertion order for readable JSON
     */
    private Map<String, Object> buildBody(int status, String error,
                                          String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        // ISO-8601 UTC timestamp — when the error occurred on the server
        body.put("timestamp", Instant.now().toString());
        // HTTP numeric status code — matches the response HTTP status header
        body.put("status", status);
        // Short HTTP reason phrase — client can use this for i18n lookup
        body.put("error", error);
        // Full human-readable explanation — safe to show in developer logs, NOT to end users
        body.put("message", message);
        // The request path — helps the client know which endpoint caused the error
        body.put("path", path);
        return body;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 1: ResourceNotFoundException → HTTP 404 Not Found
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: lookups for Applicant, LoanApplication, etc. that return no document.
     * Returns HTTP 404 with the entity name and missing ID clearly identified.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        // Log at WARN level — 404s are expected behaviour (user looked up wrong id),
        // not application errors. We don't want them polluting ERROR log streams.
        log.warn("[404] Resource not found: {} | Path: {}",
                 ex.getMessage(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        // Add extra fields to help the client identify what was missing
        body.put("resourceName", ex.getResourceName());
        body.put("fieldName", ex.getFieldName());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 2: MethodArgumentNotValidException → HTTP 400 Bad Request
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: Spring Validation failures triggered by @Valid on request bodies.
     * When a required field is missing or a @Min/@Email/@Pattern constraint fails,
     * Spring throws MethodArgumentNotValidException before the controller method runs.
     *
     * We extract ALL field-level errors and return them in a "fieldErrors" map so
     * the Angular frontend can highlight each invalid form field individually.
     *
     * Example response:
     * {
     *   "status": 400,
     *   "message": "Validation failed for 3 field(s)",
     *   "fieldErrors": {
     *     "email": "must be a valid email address",
     *     "loanAmount": "must be greater than 0",
     *     "panNumber": "PAN must be in the format AAAAA9999A"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Collect all per-field error messages into a map: fieldName → errorMessage
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // If a field has multiple violations, the last one wins — acceptable for UX
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("[400] Validation failed on {} field(s) | Path: {} | Errors: {}",
                 fieldErrors.size(), request.getRequestURI(), fieldErrors);

        Map<String, Object> body = buildBody(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            String.format("Validation failed for %d field(s). Check 'fieldErrors' for details.",
                          fieldErrors.size()),
            request.getRequestURI()
        );
        // Attach the per-field error detail map
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 3: InvalidStateTransitionException → HTTP 422 Unprocessable Entity
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: attempts to move a LoanApplication to an illegal next status.
     * The error body includes both the from-status and to-status so the caller
     * can display a precise, actionable error message.
     */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
            InvalidStateTransitionException ex,
            HttpServletRequest request) {

        log.warn("[422] Invalid state transition: {} → {} | Path: {}",
                 ex.getFromStatus(), ex.getToStatus(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Unprocessable Entity",
            ex.getMessage(),
            request.getRequestURI()
        );
        body.put("fromStatus", ex.getFromStatus());
        body.put("toStatus",   ex.getToStatus());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 4: DuplicateResourceException → HTTP 409 Conflict
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: attempts to create an Applicant with an already-registered email or PAN.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.warn("[409] Duplicate resource: {} | Path: {}",
                 ex.getMessage(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI()
        );
        body.put("duplicateField", ex.getFieldName());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 5: BusinessRuleException → HTTP 422 Unprocessable Entity
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: generic business rule violations (active application already exists, etc.).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request) {

        log.warn("[422] Business rule violation: {} | Code: {} | Path: {}",
                 ex.getMessage(), ex.getRuleCode(), request.getRequestURI());

        Map<String, Object> body = buildBody(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "Unprocessable Entity",
            ex.getMessage(),
            request.getRequestURI()
        );
        body.put("ruleCode", ex.getRuleCode());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 6: MongoDB DuplicateKeyException → HTTP 409 Conflict
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: MongoDB-level unique index violations that slip past our
     * application-level duplicate checks (race condition: two near-simultaneous
     * POSTs with the same email, for example).
     *
     * This is the "belt AND suspenders" safety net at the database layer.
     * The application-level check (existsByEmail) runs first, but between
     * that check and the save(), another thread could insert the same email.
     * MongoDB's unique index catch that race condition and throw this exception.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleMongoUniqueConstraint(
            DuplicateKeyException ex,
            HttpServletRequest request) {

        // Log at ERROR because this indicates a concurrency issue worth investigating
        log.error("[409] MongoDB unique key violation — possible race condition | Path: {} | {}",
                  request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = buildBody(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "A resource with the same unique identifier already exists. " +
            "This may indicate a concurrent submission — please retry.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 7: MethodArgumentTypeMismatchException → HTTP 400 Bad Request
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: passing the wrong type for a path variable or query parameter.
     * Example: GET /api/applications?status=INVALID_STATUS_VALUE
     * Spring tries to convert "INVALID_STATUS_VALUE" to LoanStatus enum → fails.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format(
            "Parameter '%s' has invalid value '%s'. Expected type: %s",
            ex.getName(),
            ex.getValue(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("[400] Type mismatch: {} | Path: {}", message, request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(buildBody(400, "Bad Request", message, request.getRequestURI()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler 8: Catch-all → HTTP 500 Internal Server Error
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Handles: any exception not caught by the more specific handlers above.
     * This is the last line of defence — ensures the client always gets
     * a structured JSON response, never a raw stack trace.
     *
     * SECURITY NOTE: We deliberately omit the exception detail (stack trace,
     * internal class names) from the response body — that information could
     * aid an attacker. We log the full stack trace server-side for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllUncaught(
            Exception ex,
            HttpServletRequest request) {

        // Log at ERROR level with full stack trace — essential for debugging
        log.error("[500] Unhandled exception at path: {} | Type: {} | Message: {}",
                  request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        Map<String, Object> body = buildBody(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            // Generic message to end user — never expose internal details
            "An unexpected error occurred. The engineering team has been notified. " +
            "Please try again later or contact support.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
