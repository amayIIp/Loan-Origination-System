package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * EMBEDDING vs REFERENCING decision for KycDocument:
 *
 * WHY EMBEDDED (inside Applicant document)?
 * ──────────────────────────────────────────
 * 1. Ownership & lifecycle coupling: KYC documents belong exclusively to ONE
 *    applicant. They are never shared between applicants and have no independent
 *    existence — deleting an applicant should delete their documents too.
 *    Embedding enforces this ownership at the data model level.
 *
 * 2. Read performance: Every time we load an applicant to evaluate their
 *    loan application, we ALWAYS need their KYC document statuses (to check
 *    if all are VERIFIED before moving to UNDER_REVIEW). Embedding means one
 *    MongoDB read — no secondary lookup needed. With referencing, we'd need
 *    two round-trips to the database on every applicant load.
 *
 * 3. Document count is bounded: An applicant will never have thousands of KYC
 *    documents — realistically 2–10 per applicant. MongoDB's 16MB document size
 *    limit is not a concern here. If we expected thousands of sub-documents,
 *    referencing would be preferable.
 *
 * 4. Atomic updates: When a loan officer updates a document's verificationStatus,
 *    Spring Data MongoDB updates it in a single atomic operation on the parent
 *    Applicant document — no cross-collection transactions needed.
 *
 * TRADE-OFF ACKNOWLEDGED:
 * If we ever needed to query "all documents of type ID_PROOF across all applicants",
 * we'd have to scan every applicant document. For that reporting use case,
 * a separate collection + MongoDB aggregation pipeline is the right tool
 * (built in the reporting phase). For the primary KYC workflow, embedding wins.
 * ─────────────────────────────────────────────────────────────────────────────
 */

// Import our enum types defined in the enums sub-package
import com.los.backend.model.enums.KycDocumentType;
import com.los.backend.model.enums.VerificationStatus;

// Lombok annotations — the compiler generates getters, setters, equals,
// hashCode, toString, a no-args constructor, and an all-args constructor + builder
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Spring Data MongoDB annotation — maps this Java field to a MongoDB document field
import org.springframework.data.mongodb.core.mapping.Field;

// Jakarta Bean Validation annotations — enforce data integrity before saving to MongoDB
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Java 8+ time — timezone-aware timestamp, always stored/compared in UTC
import java.time.Instant;

/**
 * KycDocument — represents one uploaded KYC document belonging to an Applicant.
 *
 * This class has NO @Document annotation because it is NOT stored in its own
 * MongoDB collection. It is an "embedded" sub-document stored as a nested
 * object INSIDE the "applicants" collection document. See embedding rationale above.
 *
 * In JSON (as stored in MongoDB), it looks like:
 * {
 *   "documentType": "ID_PROOF",
 *   "file_url": "https://storage.los.com/kyc/pan_card_abc123.pdf",
 *   "uploaded_at": "2024-06-01T10:30:00Z",
 *   "verification_status": "PENDING",
 *   "rejectionReason": null
 * }
 */
// @Data — Lombok: generates getters, setters, equals(), hashCode(), toString()
@Data
// @Builder — Lombok: generates a fluent builder so we can construct instances like:
//   KycDocument.builder().documentType(ID_PROOF).fileUrl("...").build()
@Builder
// @NoArgsConstructor — Lombok: generates a no-args constructor (required by Spring Data
//   MongoDB's deserialisation — it needs to create blank objects, then set fields)
@NoArgsConstructor
// @AllArgsConstructor — Lombok: generates a constructor that takes every field as a param
//   (required by @Builder when @NoArgsConstructor is also present)
@AllArgsConstructor
public class KycDocument {

    /**
     * documentType — what kind of document this is (ID, address, or income proof).
     * Stored as the enum name string in MongoDB (e.g., "ID_PROOF").
     *
     * @NotNull — validation: the document type MUST be provided; we cannot accept
     *            an upload without knowing what kind of document it is.
     */
    @NotNull(message = "Document type is required")
    // @Field — maps this Java field to a specific MongoDB field name.
    // We use snake_case in MongoDB (NoSQL convention) and camelCase in Java (Java convention).
    @Field("document_type")
    private KycDocumentType documentType;

    /**
     * fileUrl — the URL where the actual document file is stored.
     * In production this points to cloud storage (AWS S3, GCS, Azure Blob).
     * We store ONLY the URL here — never the binary file content in MongoDB.
     * Storing files in MongoDB gridFS or as base64 is an anti-pattern for large files.
     *
     * @NotBlank — validation: must be a non-null, non-empty, non-whitespace string.
     */
    @NotBlank(message = "File URL is required")
    @Field("file_url")
    private String fileUrl;

    /**
     * uploadedAt — the UTC timestamp when the applicant uploaded this document.
     * Instant is timezone-agnostic (always UTC) — safer than LocalDateTime
     * for a system that may have servers in multiple regions.
     */
    @Field("uploaded_at")
    // @Builder.Default ensures the builder initialises this to "now" even when
    // the caller doesn't explicitly set it — prevents null timestamps on creation.
    @Builder.Default
    private Instant uploadedAt = Instant.now();

    /**
     * verificationStatus — the current review state of this document.
     * Starts as PENDING, transitions to VERIFIED or REJECTED after officer review.
     * @NotNull — a document must always have a known status; null would be ambiguous.
     */
    @NotNull(message = "Verification status is required")
    @Field("verification_status")
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /**
     * rejectionReason — free-text explanation provided by the loan officer when
     * setting verificationStatus = REJECTED. Null when not rejected.
     * Examples: "Document expired", "Name does not match application", "Image unclear"
     * This is displayed to the applicant so they know what to correct and re-upload.
     */
    @Field("rejection_reason")
    private String rejectionReason;

    /**
     * verifiedAt — timestamp of when the document was marked VERIFIED or REJECTED.
     * Null until the first officer review action. Used for SLA tracking
     * (how long does KYC verification take on average?).
     */
    @Field("verified_at")
    private Instant verifiedAt;
}
