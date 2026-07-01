package com.los.backend.model.enums;

/**
 * EmploymentType — how the applicant earns their income.
 *
 * This matters for credit risk assessment (BRE) because:
 * - SALARIED employees have stable, predictable income → lower risk.
 * - SELF_EMPLOYED income can fluctuate seasonally → slightly higher risk.
 * - BUSINESS_OWNER income depends on business health → requires extra scrutiny.
 * - UNEMPLOYED applicants typically cannot qualify for a loan.
 *
 * The BRE CreditRule engine can use this field to apply different
 * income verification requirements or risk multipliers per type.
 */
public enum EmploymentType {

    /** Employed by a company; receives a regular monthly salary via payslip. */
    SALARIED,

    /** Freelancer, consultant, or professional with no single employer. */
    SELF_EMPLOYED,

    /** Owns and operates a registered business entity. */
    BUSINESS_OWNER,

    /** Retired but receiving a pension — may still qualify depending on income. */
    RETIRED,

    /**
     * Currently not earning a regular income.
     * Most loan products will automatically reject this status via a CreditRule,
     * but we capture it so the system records the actual status rather than
     * silently allowing the applicant to submit without employment info.
     */
    UNEMPLOYED
}
