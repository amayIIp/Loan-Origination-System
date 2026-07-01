package com.los.backend.exception;

import com.los.backend.model.enums.LoanStatus;


public class InvalidStateTransitionException extends RuntimeException {

    
    private final LoanStatus fromStatus;

    
    private final LoanStatus toStatus;

    
    public InvalidStateTransitionException(LoanStatus fromStatus, LoanStatus toStatus) {
        super(String.format(
            "Invalid state transition: '%s' → '%s' is not a legal lifecycle step. " +
            "Review the LoanApplication state machine for valid transitions.",
            fromStatus, toStatus
        ));
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    
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
