package com.los.backend.model.enums;

/**
 * FinalDecision — the overall outcome produced by the Business Rule Engine
 * after evaluating ALL active CreditRules against a loan application.
 *
 * Kept as a separate enum from LoanStatus intentionally:
 * - LoanStatus lives on LoanApplication and tracks the full workflow lifecycle.
 * - FinalDecision lives on DecisionLog and records only the BRE verdict.
 * This separation allows us to store the BRE decision immutably in the audit
 * log even if the LoanApplication status is later manually overridden.
 */
public enum FinalDecision {

    /**
     * All active CreditRules passed — the BRE recommends approval.
     * The LoanApplication status will be transitioned to APPROVED after
     * a loan officer confirms the BRE recommendation (four-eyes principle).
     */
    APPROVED,

    /**
     * One or more CreditRules failed — the BRE recommends rejection.
     * The individual RuleResult list in DecisionLog explains which rules
     * failed and why, providing a clear audit trail for the decision.
     */
    REJECTED,

    /**
     * The BRE could not complete evaluation — e.g., a required data field
     * (credit score, income) was missing or a third-party service was unavailable.
     * The application is flagged for manual review by a loan officer.
     * A REVIEW decision does NOT count as approval or rejection.
     */
    REVIEW
}
