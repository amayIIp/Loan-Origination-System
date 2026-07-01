package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ENTITY: LoanApplication
 * MongoDB Collection: "loan_applications"
 *
 * EMBEDDING vs REFERENCING decisions:
 *
 * 🔗 REFERENCED — Applicant (by applicantId String):
 *    LoanApplication stores ONLY the Applicant's MongoDB _id (a String reference),
 *    NOT the full Applicant document. Reasons:
 *    a) An applicant can have multiple loan applications over their lifetime.
 *       Embedding the applicant would duplicate their entire PII record in every
 *       LoanApplication document — a severe data duplication + consistency problem.
 *    b) If the applicant updates their address or income, we'd have to update
 *       every LoanApplication document too (N writes instead of 1).
 *    c) The Applicant document is large (with KYC docs embedded). Embedding it
 *       in every loan application would bloat loan_applications unnecessarily.
 *    → Reference is the clear winner. In queries needing both, we use a
 *      $lookup aggregation pipeline (equivalent of a SQL JOIN) or a two-step
 *      load in the service layer.
 *
 * ✅ EMBEDDED — DecisionResult:
 *    1-to-1 with this loan application. Always displayed alongside it on the
 *    loan officer's screen. Small, bounded, no independent queries. See
 *    DecisionResult.java header for full rationale.
 *
 * 🔗 REFERENCED → DecisionLog (by loanApplicationId in DecisionLog):
 *    The full verbose BRE audit log is a SEPARATE document in "decision_logs"
 *    collection (DecisionLog references back to LoanApplication by ID).
 *    Reason: the full log can be verbose (many rule outcomes with long reason strings).
 *    Keeping it separate prevents loan_applications from growing too large and
 *    lets us query/archive decision logs independently.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import com.los.backend.model.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * LoanApplication — the central entity of the LOS system.
 * Stored in the "loan_applications" MongoDB collection.
 *
 * Each document represents one request from an Applicant to borrow money.
 * It tracks the full lifecycle (SUBMITTED → DISBURSED or REJECTED) and
 * embeds the BRE decision result once credit evaluation is complete.
 *
 * Indexing strategy (see also: the mongosh index script):
 * ────────────────────────────────────────────────────────
 * We define compound indexes here via @CompoundIndexes so Spring Data
 * can create them automatically via MongoConfig. The mongosh script
 * provides manual equivalents for DBA/ops use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "loan_applications")
// Compound index on (status + created_at) — covers the most common query:
// "Give me all SUBMITTED applications from the last 7 days, ordered by date"
// A compound index is more efficient than two separate single-field indexes
// because MongoDB can satisfy both the equality filter (status) and the
// range filter (created_at) with a single index scan.
@CompoundIndexes({
    @CompoundIndex(
        name = "idx_status_created",
        def = "{'status': 1, 'created_at': -1}",
        // background = true means the index is built without locking the collection
        // — existing reads/writes continue normally during index build (production-safe)
        background = true
    ),
    @CompoundIndex(
        name = "idx_applicant_status",
        def = "{'applicant_id': 1, 'status': 1}",
        background = true
    )
})
public class LoanApplication {

    /**
     * id — MongoDB ObjectId, auto-generated on insert.
     * This ID is shared with the applicant (via applicantId reference) and
     * with DecisionLog (which references this loan by loanApplicationId).
     */
    @Id
    private String id;

    /**
     * applicantId — the MongoDB _id of the Applicant who submitted this loan.
     * This is a manual reference (not a DBRef) — we store the ID string and
     * perform the join in the service layer when needed.
     *
     * Why not @DBRef? — Spring Data's @DBRef creates a cross-collection reference
     * that requires an extra round-trip to resolve. Manually holding the ID and
     * loading the Applicant explicitly in service code gives us more control
     * over when/how the join happens (lazy vs eager, batched, etc.).
     *
     * @NotBlank — every loan application MUST be linked to an applicant.
     * @Indexed — single-field index so "find all loans for applicant X" is fast.
     */
    @NotBlank(message = "Applicant ID is required")
    @Indexed
    @Field("applicant_id")
    private String applicantId;

    // ── Loan Request Details ─────────────────────────────────────────────────

    /**
     * loanAmount — the amount the applicant wants to borrow, in INR.
     * @NotNull — must be specified; no valid loan without an amount.
     * @DecimalMin — must be at least ₹1,000; sub-₹1,000 microloans are handled
     *              by a different product (not this LOS).
     * @DecimalMax — cap at ₹10 crore (₹10,000,000); larger amounts go through
     *              a separate commercial lending process.
     * @Digits — monetary precision: up to 10 integer digits, 2 decimal places.
     */
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Loan amount must be at least ₹1,000")
    @DecimalMax(value = "10000000.00", message = "Loan amount cannot exceed ₹1,00,00,000")
    @Digits(integer = 10, fraction = 2)
    @Field("loan_amount")
    private BigDecimal loanAmount;

    /**
     * tenureMonths — the requested repayment duration in months.
     * @Min / @Max — 6 months minimum (very short loans have high admin overhead),
     *               360 months maximum (30-year home loan is typically the longest).
     */
    @NotNull(message = "Loan tenure is required")
    @Min(value = 6, message = "Minimum loan tenure is 6 months")
    @Max(value = 360, message = "Maximum loan tenure is 360 months (30 years)")
    @Field("tenure_months")
    private Integer tenureMonths;

    /**
     * purpose — free-text description of why the applicant needs the loan.
     * Examples: "Home renovation", "Medical emergency", "Education fees", "Business expansion"
     * @NotBlank — required for compliance reporting and fraud detection.
     * @Size — capped at 500 chars to prevent misuse as a free-text dump.
     */
    @NotBlank(message = "Loan purpose is required")
    @Size(max = 500, message = "Loan purpose must not exceed 500 characters")
    @Field("purpose")
    private String purpose;

    /**
     * interestRatePercent — the annual interest rate offered, as a percentage.
     * Populated by a loan officer or pricing engine AFTER approval.
     * Null during SUBMITTED → CREDIT_CHECK stages.
     * Example: 12.5 means 12.5% per annum.
     */
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    @Field("interest_rate_percent")
    private BigDecimal interestRatePercent;

    // ── Lifecycle Status ─────────────────────────────────────────────────────

    /**
     * status — the current state of this loan application in the lifecycle.
     * @NotNull — every application must have a known status at all times.
     * Transitions are enforced in LoanService — we never update status directly here;
     * the service validates the transition is legal before persisting.
     * @Indexed — single-field index for filtering by status (e.g., "show all APPROVED").
     *            The compound index (status + created_at) covers most use cases,
     *            but a single index on status alone helps "count by status" queries.
     */
    @NotNull(message = "Loan status is required")
    @Indexed
    @Field("status")
    @Builder.Default
    private LoanStatus status = LoanStatus.SUBMITTED;

    /**
     * previousStatus — the status before the most recent transition.
     * Useful for undo operations, audit displays, and debugging transitions.
     * Null for brand-new applications (no previous status exists).
     */
    @Field("previous_status")
    private LoanStatus previousStatus;

    /**
     * statusUpdatedAt — timestamp of the most recent status change.
     * Used for SLA calculations (e.g., "how long did this application stay in KYC_PENDING?")
     * and for escalation alerts if an application stalls in a state too long.
     */
    @Field("status_updated_at")
    private Instant statusUpdatedAt;

    /**
     * assignedOfficerId — the MongoDB User._id of the loan officer currently
     * responsible for reviewing this application. Null until assigned.
     * Used for workload balancing and email notifications.
     */
    @Field("assigned_officer_id")
    private String assignedOfficerId;

    // ── BRE Decision (Embedded) ───────────────────────────────────────────────

    /**
     * decisionResult — the embedded summary of the BRE credit evaluation.
     * Null until the application reaches CREDIT_CHECK status and BRE completes.
     * @Valid — cascades Jakarta validation into the DecisionResult sub-document
     *          if non-null (protects against partial/corrupt decision data).
     */
    @Valid
    @Field("decision_result")
    private DecisionResult decisionResult;

    // ── Rejection / Notes ─────────────────────────────────────────────────────

    /**
     * rejectionReason — free-text reason provided by the loan officer when
     * manually rejecting an application (outside BRE — e.g., during KYC_PENDING).
     * Null for non-rejected applications.
     */
    @Size(max = 1000)
    @Field("rejection_reason")
    private String rejectionReason;

    /**
     * internalNotes — private notes added by loan officers during review.
     * NOT visible to the applicant. Used for inter-officer communication.
     * Example: "Applicant's employer could not be verified via LinkedIn."
     */
    @Size(max = 2000)
    @Field("internal_notes")
    private String internalNotes;

    // ── Audit Timestamps ─────────────────────────────────────────────────────

    /**
     * createdAt — when this loan application was first submitted.
     * @CreatedDate — auto-set by Spring Data on first save(). Immutable after that.
     * @Indexed — standalone index for time-range queries ("applications this month").
     *            Also covered by the compound index (status, created_at) for most use cases.
     */
    @CreatedDate
    @Indexed
    @Field("created_at")
    private Instant createdAt;

    /**
     * updatedAt — timestamp of the most recent change to any field.
     * @LastModifiedDate — Spring Data updates this on every save() automatically.
     */
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
