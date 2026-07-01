package com.los.backend.exception;

import com.los.backend.model.enums.LoanStatus;

/**
 * InvalidStateTransitionException — thrown when code attempts to move a
 * LoanApplication to a status that is not a legal next step in the lifecycle.
 *
 * Example illegal transitions:
 *   SUBMITTED  → DISBURSED   (skipped KYC, review, credit check, approval)
 *   DISBURSED  → APPROVED    (already terminal — cannot go backwards)
 *   REJECTED   → UNDER_REVIEW (already terminal — a new application must be filed)
 *
 * Mapped to HTTP 422 Unprocessable Entity by GlobalExceptionHandler.
 *
 * Why 422 instead of 400 Bad Request?
 * ─────────────────────────────────────
 * HTTP 400 = the request is syntactically malformed (invalid JSON, missing field).
 * HTTP 422 = the request is syntactically valid but semantically wrong — the fields
 * are present and correctly typed, but the BUSINESS RULE rejects the combination.
 * An invalid state transition is a business rule violation, not a format error.
 */
public class InvalidStateTransitionException extends RuntimeException {

    /** The status the application was in before the attempted transition */
    private final LoanStatus fromStatus;

    /** The status that was requested (the illegal target) */
    private final LoanStatus toStatus;

    /**
     * Constructor — builds a clear message explaining which transition was rejected.
     *
     * @param fromStatus the current (source) status of the application
     * @param toStatus   the requested (target) status that was rejected
     */
    public InvalidStateTransitionException(LoanStatus fromStatus, LoanStatus toStatus) {
        super(String.format(
            "Invalid state transition: '%s' → '%s' is not a legal lifecycle step. " +
            "Review the LoanApplication state machine for valid transitions.",
            fromStatus, toStatus
        ));
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    /**
     * Constructor with a custom message — used when additional context is needed,
     * e.g., "Cannot transition to DISBURSED: application is not yet APPROVED."
     *
     * @param fromStatus the current status
     * @param toStatus   the illegal target status
     * @param reason     extra explanation appended to the message
     */
    public InvalidStateTransitionException(LoanStatus fromStatus, LoanStatus toStatus, String reason) {
        super(String.format(
            "Invalid state transition: '%s' → '%s'. Reason: %s",
            fromStatus, toStatus, reason
        ));
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    public LoanStatus getFromStatus() { return fromStatus; }
    public LoanStatus getToStatus()   { return toStatus; }
}
