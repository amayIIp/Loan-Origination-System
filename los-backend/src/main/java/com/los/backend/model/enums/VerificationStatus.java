package com.los.backend.model.enums;

/**
 * VerificationStatus — the lifecycle of a single KYC document review.
 *
 * A document progresses linearly through these states:
 *   PENDING → VERIFIED   (if the document is authentic and legible)
 *   PENDING → REJECTED   (if the document is blurry, expired, or fraudulent)
 *   REJECTED → PENDING   (if the applicant re-uploads a corrected document)
 *
 * Storing this as an enum rather than a boolean (isVerified) is better because:
 * - "not yet checked" (PENDING) is meaningfully different from "checked and failed" (REJECTED)
 * - A boolean cannot express that three-way distinction
 */
public enum VerificationStatus {

    /**
     * Document has been uploaded by the applicant but not yet reviewed
     * by a loan officer or automated verification system.
     * This is the initial state for every uploaded document.
     */
    PENDING,

    /**
     * Document has been reviewed and confirmed as authentic, legible,
     * and consistent with the applicant's stated information.
     */
    VERIFIED,

    /**
     * Document was reviewed and rejected. Possible reasons:
     * blurry scan, expired document, name mismatch, suspected forgery.
     * The applicant must re-upload a valid document to proceed.
     */
    REJECTED
}
