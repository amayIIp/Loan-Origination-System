package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ENTITY: Applicant
 * MongoDB Collection: "applicants"
 *
 * EMBEDDING vs REFERENCING decisions:
 *
 * ✅ EMBEDDED — Address:
 *    Always loaded with the applicant; tiny, bounded, no independent queries.
 *
 * ✅ EMBEDDED — EmploymentInfo:
 *    Directly used by BRE in every credit evaluation; always needed together
 *    with the Applicant; 1-to-1 relationship.
 *
 * ✅ EMBEDDED — List<KycDocument>:
 *    KYC documents belong exclusively to one applicant; always loaded together
 *    for KYC status checks; bounded count (2–10 per applicant). Full rationale
 *    in KycDocument.java header.
 *
 * 🔗 REFERENCED — LoanApplication → Applicant (by applicantId):
 *    A LoanApplication stores the applicant's MongoDB ObjectId, NOT the full
 *    applicant document. Reason: a single applicant may have MULTIPLE loan
 *    applications over time (e.g., second loan after first is repaid). Embedding
 *    the applicant inside each loan would duplicate PII data, create update
 *    anomalies (address change must update all loan docs), and violate DRY.
 *    Referencing by ID is the correct choice for this many-applications-to-one-
 *    applicant relationship.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Document — tells Spring Data MongoDB: "Map this class to a MongoDB collection."
// collection = the MongoDB collection name. If omitted, Spring uses the class name in
// lowercase. We always set it explicitly for clarity and to prevent surprises on rename.
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Applicant — represents a loan applicant's complete profile stored in MongoDB.
 *
 * This is a top-level MongoDB document stored in the "applicants" collection.
 * Each document = one applicant's record including personal info, employment
 * details, address, and all uploaded KYC documents.
 *
 * PII Sensitivity Notice:
 * ────────────────────────
 * This document contains Personally Identifiable Information (PII):
 * full name, date of birth, PAN card number, phone number, email, income.
 * In production, fields marked as sensitive (panNumber, dateOfBirth) should
 * be encrypted at rest. Never log these values. Access must be role-gated
 * (loan officers only, not applicant-facing APIs).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// @Document — maps this Java class to the "applicants" MongoDB collection
@Document(collection = "applicants")
public class Applicant {

    /**
     * id — MongoDB's primary key for this document.
     * @Id — marks this as the document's _id field in MongoDB.
     * MongoDB auto-generates a 24-character hex ObjectId (e.g., "507f1f77bcf86cd799439011")
     * when we save a new document. We use String here (not ObjectId type) for
     * easier JSON serialisation and REST API exposure.
     */
    @Id
    private String id;

    // ── Personal Information ─────────────────────────────────────────────────

    /**
     * firstName — applicant's legal first name as on government-issued ID.
     * @NotBlank — must be present and non-empty; identity cannot be established without it.
     * @Size — max 100 chars prevents excessive-length inputs (likely data entry errors).
     */
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Field("first_name")
    private String firstName;

    /**
     * lastName — applicant's legal surname. Same constraints as firstName.
     */
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Field("last_name")
    private String lastName;

    /**
     * email — applicant's email address. Used for notifications and as a
     * secondary deduplication key (alongside PAN).
     *
     * @Email — Jakarta validation ensures it matches the standard email format (x@y.z).
     * @Indexed(unique=true) — MongoDB creates a unique index on this field.
     *   This means: no two Applicant documents can have the same email address.
     *   Why unique? An email uniquely identifies a person in our system. Duplicate
     *   emails usually indicate a re-submission or data entry error — we surface
     *   it as a business error rather than silently creating duplicate profiles.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Indexed(unique = true)
    @Field("email")
    private String email;

    /**
     * phone — 10-digit Indian mobile number (without country code prefix).
     * @Pattern — regex \\d{10} = exactly 10 digit characters.
     * We store without the +91 prefix; the presentation layer adds it as needed.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be a 10-digit number")
    @Field("phone")
    private String phone;

    /**
     * dateOfBirth — applicant's date of birth for age eligibility check.
     * We use LocalDate (not LocalDateTime) because only the calendar date matters —
     * time of day is irrelevant for age calculation.
     * The BRE AGE rule will compute age from this field at evaluation time.
     *
     * @Past — Jakarta validation ensures the date is in the past (you can't be
     *         born in the future). Guards against "2099-01-01" data entry errors.
     */
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Field("date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * panNumber — the applicant's PAN (Permanent Account Number) — a 10-character
     * alphanumeric Indian national ID used for all financial transactions.
     * Format: AAAAA9999A (5 letters, 4 digits, 1 letter — all uppercase).
     *
     * @Pattern — regex enforces the exact PAN format to prevent invalid numbers
     *            from reaching the KYC verification step.
     * @Indexed(unique=true) — PAN uniquely identifies an Indian taxpayer.
     *   Duplicate PANs indicate either duplicate profiles or identity fraud —
     *   both must be caught and flagged for investigation.
     *
     * ⚠️ PAN is sensitive PII. In production, encrypt this field at rest
     *    and mask it in API responses (show only last 4 chars: "XXXXX1234A").
     */
    @NotBlank(message = "PAN number is required")
    // PAN regex: exactly 5 uppercase letters, 4 digits, 1 uppercase letter
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "PAN must be in the format AAAAA9999A")
    @Indexed(unique = true)
    @Field("pan_number")
    private String panNumber;

    // ── Embedded Sub-Documents ───────────────────────────────────────────────

    /**
     * address — the applicant's current residential address.
     * Embedded as a sub-document (see embedding rationale in Address.java).
     * @Valid — tells Jakarta Validation to also validate the nested Address object's
     *          own @NotBlank, @Pattern etc. annotations recursively.
     *          Without @Valid, the nested fields are NOT validated.
     */
    @NotNull(message = "Address is required")
    @Valid
    @Field("address")
    private Address address;

    /**
     * employmentInfo — the applicant's employment and income details.
     * Embedded sub-document. @Valid cascades validation into the nested object.
     */
    @NotNull(message = "Employment information is required")
    @Valid
    @Field("employment_info")
    private EmploymentInfo employmentInfo;

    /**
     * kycDocuments — the list of all KYC documents uploaded by this applicant.
     * Embedded as a sub-array within the Applicant document.
     * @Builder.Default initialises to an empty list — prevents NullPointerException
     *                  when a new applicant is created before any documents are uploaded.
     *
     * MongoDB stores this as a JSON array of objects inside the applicant document:
     * "kyc_documents": [{ "document_type": "ID_PROOF", ... }, { ... }]
     */
    @Builder.Default
    @Field("kyc_documents")
    private List<@Valid KycDocument> kycDocuments = new ArrayList<>();

    // ── Audit Fields ─────────────────────────────────────────────────────────
    // These fields are automatically managed by Spring Data MongoDB's auditing
    // feature (configured with @EnableMongoAuditing in a config class).

    /**
     * createdAt — the UTC timestamp when this applicant record was first created.
     * @CreatedDate — Spring Data sets this automatically on the first save().
     *                Never updated after that, making it an immutable creation marker.
     */
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    /**
     * updatedAt — the UTC timestamp of the most recent update to this record.
     * @LastModifiedDate — Spring Data updates this automatically on every save().
     * Useful for cache invalidation and detecting stale records.
     */
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    /**
     * isActive — soft-delete flag. When false, the applicant record is logically
     * deleted but physically retained in MongoDB for audit/compliance purposes.
     * Financial regulations (RBI guidelines) typically require PII retention for 5+ years.
     * Hard-deleting records would violate these requirements.
     */
    @Builder.Default
    @Field("is_active")
    private boolean isActive = true;

    // ── Computed helper (not stored in MongoDB) ───────────────────────────────

    /**
     * getFullName — convenience method for display purposes.
     * Not stored in MongoDB (no @Field annotation) — computed on demand.
     * Avoids storing redundant derived data that can become stale.
     */
    public String getFullName() {
        // Concatenate first and last name with a space separator
        return firstName + " " + lastName;
    }
}
