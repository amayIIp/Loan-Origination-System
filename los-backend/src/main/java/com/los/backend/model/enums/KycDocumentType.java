package com.los.backend.model.enums;

/**
 * KycDocumentType — the category of a KYC (Know Your Customer) document.
 *
 * What is KYC? (beginner explanation)
 * ─────────────────────────────────────
 * KYC is a legal requirement for financial institutions to verify the identity
 * of their customers before extending credit. Regulators require proof that:
 * 1. The person is who they claim to be (ID_PROOF).
 * 2. They live where they claim (ADDRESS_PROOF).
 * 3. Their stated income is real (INCOME_PROOF).
 *
 * Using an enum (a fixed set of named constants) rather than raw strings prevents
 * typos like "id_proff" or "Income Proof" from entering the database — MongoDB will
 * store the exact enum name as a string (e.g. "ID_PROOF").
 */
public enum KycDocumentType {

    /**
     * Government-issued photo ID — Aadhaar card, PAN card, passport, voter ID, etc.
     * Used to confirm the applicant's full name and photograph.
     */
    ID_PROOF,

    /**
     * Document showing the applicant's current residential address —
     * utility bill, bank statement, rental agreement, etc.
     */
    ADDRESS_PROOF,

    /**
     * Document proving the applicant's stated income —
     * salary slips, bank statements, ITR (Income Tax Return), Form 16, etc.
     * Critical for debt-to-income ratio checks in the Business Rule Engine.
     */
    INCOME_PROOF,

    /**
     * A photograph of the applicant — required by some lenders for
     * anti-fraud, face-match, and liveness verification.
     */
    PHOTOGRAPH,

    /**
     * Any additional supporting document not covered by the above categories.
     * Loan officers can request these during manual review.
     */
    OTHER
}
