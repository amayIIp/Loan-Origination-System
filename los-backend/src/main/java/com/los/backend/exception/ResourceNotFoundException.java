package com.los.backend.exception;

/**
 * ResourceNotFoundException — thrown when a requested MongoDB document does not exist.
 *
 * Examples:
 *   - GET /api/applications/abc123 → no LoanApplication with id "abc123"
 *   - POST /api/applications with applicantId "xyz" → no Applicant with that id
 *
 * Mapped to HTTP 404 Not Found by GlobalExceptionHandler.
 *
 * Why a custom exception instead of throwing RuntimeException directly?
 * ─────────────────────────────────────────────────────────────────────
 * A custom exception type lets GlobalExceptionHandler detect it by class
 * and return a precise HTTP 404 response. A bare RuntimeException would be
 * caught by the generic 500 handler instead, giving the caller no useful info.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * resourceName — the type of resource that was not found (e.g., "LoanApplication").
     * Included in the error response body so the caller knows what was missing.
     */
    private final String resourceName;

    /**
     * fieldName — the field used to search (e.g., "id", "email", "panNumber").
     */
    private final String fieldName;

    /**
     * fieldValue — the actual value that was looked up (e.g., "abc123").
     * Helps the caller verify they sent the correct identifier.
     */
    private final Object fieldValue;

    /**
     * Constructor — builds a descriptive message automatically from the three parts.
     *
     * @param resourceName the entity type (e.g., "Applicant")
     * @param fieldName    the field searched (e.g., "id")
     * @param fieldValue   the value that was not found (e.g., "abc123")
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Build a human-readable message: "Applicant not found with id : 'abc123'"
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName    = fieldName;
        this.fieldValue   = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
