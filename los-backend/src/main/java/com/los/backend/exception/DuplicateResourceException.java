package com.los.backend.exception;

/**
 * DuplicateResourceException — thrown when creating a resource that would
 * violate a uniqueness constraint (email, PAN number, etc.).
 *
 * Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 *
 * Why 409 Conflict instead of 400 Bad Request?
 * ──────────────────────────────────────────────
 * HTTP 409 specifically means "the request could not be completed because of a
 * conflict with the current state of the target resource." A duplicate email is
 * a conflict with an existing document — semantically precise and correct.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format(
            "%s already exists with %s : '%s'",
            resourceName, fieldName, fieldValue
        ));
        this.resourceName = resourceName;
        this.fieldName    = fieldName;
        this.fieldValue   = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
