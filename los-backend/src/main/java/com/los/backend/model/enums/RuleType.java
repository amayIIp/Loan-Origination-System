package com.los.backend.model.enums;

/**
 * RuleType — the category of eligibility check a CreditRule performs.
 *
 * What is the Business Rule Engine (BRE)? (beginner explanation)
 * ──────────────────────────────────────────────────────────────
 * The BRE is the automated "brain" that decides whether a loan application is
 * creditworthy. Instead of hardcoding logic like "if (creditScore < 650) reject",
 * we store each check as a CreditRule DOCUMENT in MongoDB. The BRE loads these
 * rules at runtime and evaluates them against the applicant's data.
 *
 * This RuleType enum tells the engine WHAT KIND of check a CreditRule performs,
 * so the engine can route the rule to the correct evaluation logic.
 *
 * Why an enum rather than a string? — The enum ensures the BRE evaluator code
 * and the stored rule documents always use the same set of known values.
 * A typo in a string rule type could silently skip an important check.
 */
public enum RuleType {

    /**
     * Checks the applicant's credit score against a minimum threshold.
     * Credit scores (e.g., CIBIL in India, FICO in the US) summarise an
     * individual's credit history as a number (typically 300–900 or 300–850).
     * Higher = better credit history = lower default risk.
     * Parameters expected: { "minimumScore": 650 }
     */
    CREDIT_SCORE,

    /**
     * Debt-to-Income ratio check — monthly debt obligations as a percentage of
     * gross monthly income. Formula: (total monthly EMIs / monthly income) × 100
     * A ratio above 40–50% indicates the applicant may be over-leveraged.
     * Parameters expected: { "maximumRatioPercent": 40 }
     */
    DEBT_TO_INCOME,

    /**
     * Age eligibility check — ensures the applicant is within the lender's
     * minimum and maximum age band (e.g., 21–65 years old).
     * Parameters expected: { "minimumAge": 21, "maximumAge": 65 }
     */
    AGE,

    /**
     * Loan-to-Income ratio — the requested loan amount as a multiple of
     * the applicant's annual income. If someone earns ₹5L/year, a ₹50L
     * loan request (10× income) is likely unsustainable.
     * Parameters expected: { "maximumMultiple": 5 }
     */
    LOAN_TO_INCOME,

    /**
     * Employment status check — verifies the applicant has an acceptable
     * type of employment (e.g., SALARIED or SELF_EMPLOYED only).
     * Parameters expected: { "allowedTypes": ["SALARIED", "SELF_EMPLOYED"] }
     */
    EMPLOYMENT_STATUS,

    /**
     * Minimum income threshold — ensures the applicant earns at least a
     * specified monthly income before other ratios are even relevant.
     * Parameters expected: { "minimumMonthlyIncome": 25000 }
     */
    MINIMUM_INCOME
}
