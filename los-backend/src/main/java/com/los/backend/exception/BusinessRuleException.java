package com.los.backend.exception;

/**
 * BusinessRuleException — thrown when a business rule is violated that isn't
 * covered by the more specific exception types above.
 *
 * Examples:
 *   - Applicant already has an active loan application (cannot submit another)
 *   - Loan amount exceeds the applicant's eligible limit
 *   - KYC documents not fully verified before moving to UNDER_REVIEW
 *
 * Mapped to HTTP 422 Unprocessable Entity by GlobalExceptionHandler.
 */
public class BusinessRuleException extends RuntimeException {

    /** A short code identifying which rule was violated — useful for frontend i18n */
    private final String ruleCode;

    public BusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public BusinessRuleException(String message) {
        super(message);
        this.ruleCode = "BUSINESS_RULE_VIOLATION";
    }

    public String getRuleCode() { return ruleCode; }
}
