package com.los.backend.model.enums;

/**
 * LoanStatus — the complete lifecycle of a loan application, matching
 * the state diagram defined in the Phase 0 architecture document.
 *
 * Valid state transitions (only these transitions are legal):
 * ──────────────────────────────────────────────────────────────
 *   SUBMITTED    → KYC_PENDING      (system: after basic intake validation)
 *   KYC_PENDING  → UNDER_REVIEW     (system: after all KYC docs are VERIFIED)
 *   KYC_PENDING  → REJECTED         (loan officer: fraudulent or insufficient docs)
 *   UNDER_REVIEW → CREDIT_CHECK     (system: triggers Business Rule Engine)
 *   UNDER_REVIEW → REJECTED         (loan officer: manual rejection during review)
 *   CREDIT_CHECK → APPROVED         (system: BRE passes all credit rules)
 *   CREDIT_CHECK → REJECTED         (system: BRE fails one or more credit rules)
 *   APPROVED     → DISBURSED        (loan officer: funds sent to applicant)
 *   APPROVED     → REJECTED         (loan officer: applicant withdrew or fraud found)
 *
 * Storing these as an enum prevents invalid status strings from ever reaching
 * the database — MongoDB will store the enum name exactly as written here.
 */
public enum LoanStatus {

    /**
     * Initial state — the applicant has submitted the loan application form.
     * Basic intake validation has passed (required fields present, format valid),
     * but KYC documents have not yet been requested or uploaded.
     */
    SUBMITTED,

    /**
     * The system has requested KYC documents from the applicant.
     * The loan cannot proceed until all required documents are uploaded
     * and verified. The applicant is notified via email/SMS (Phase 2).
     */
    KYC_PENDING,

    /**
     * All required KYC documents have been verified.
     * A loan officer is manually reviewing the full application for
     * completeness, consistency, and any red flags before triggering the BRE.
     */
    UNDER_REVIEW,

    /**
     * The loan officer has triggered the automated credit check.
     * The Business Rule Engine (BRE) is currently evaluating all CreditRules
     * against this applicant's profile (credit score, DTI ratio, income, age, etc.).
     * This state is typically short-lived (seconds), but we track it for
     * audit purposes and to prevent concurrent duplicate evaluations.
     */
    CREDIT_CHECK,

    /**
     * The BRE has evaluated all rules and the application passed.
     * A loan officer must still formally approve disbursement — the "four-eyes"
     * principle ensures no single automated system disburses funds without human sign-off.
     */
    APPROVED,

    /**
     * The application has been permanently rejected.
     * Triggered by: KYC failure, BRE rule failure, or manual loan officer decision.
     * The DecisionLog contains the specific reason(s) for rejection.
     * This is a terminal state — the applicant must submit a new application.
     */
    REJECTED,

    /**
     * Terminal state — the loan amount has been transferred to the applicant's
     * bank account. The loan is now live and repayment schedule is activated.
     * Further state changes go to the Loan Servicing system (out of scope for LOS).
     */
    DISBURSED
}
