package com.los.backend.repository;

import com.los.backend.model.LoanApplication;
import com.los.backend.model.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * LoanApplicationRepository — data access interface for "loan_applications" collection.
 *
 * This is the most heavily queried collection in the LOS:
 * - Loan officers query by status to manage their pipeline
 * - The BRE queries by ID to fetch the application for evaluation
 * - Reporting queries by date ranges and statuses for KPI dashboards
 *
 * All query methods that access large result sets use Pageable to prevent
 * memory exhaustion. The compound index (status, created_at) defined in
 * LoanApplication.java covers the most common filter + sort combinations.
 */
@Repository
public interface LoanApplicationRepository extends MongoRepository<LoanApplication, String> {

    // ── Method 1: findByApplicantId ───────────────────────────────────────────
    /**
     * Find all loan applications submitted by a specific applicant.
     *
     * Use case: Applicant's "My Applications" screen — show all their applications
     * with current status. Also used internally to check if an applicant already
     * has an active application before allowing a new submission.
     *
     * Generated query:
     *   db.loan_applications.find({ "applicant_id": <applicantId> })
     *   sorted by "created_at" descending (newest first)
     *
     * Performance: uses the compound index (applicant_id, status)
     *
     * @param applicantId the Applicant._id whose applications we want
     * @return list of all applications for this applicant (ordered newest first)
     */
    List<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(String applicantId);

    // ── Method 2: findByStatus with Pagination ────────────────────────────────
    /**
     * Find all applications in a given status (e.g., all UNDER_REVIEW applications).
     *
     * Use case: Loan officer's work queue — "Show me all applications waiting for
     * my review." This is the primary daily-use query in the officer dashboard.
     *
     * Performance: uses the compound index (status, created_at)
     * We return Page<> rather than List<> to support infinite scroll / pagination
     * on large queues (e.g., 500 applications in UNDER_REVIEW on a busy day).
     *
     * @param status   the LoanStatus to filter by (e.g., LoanStatus.UNDER_REVIEW)
     * @param pageable page number, page size, and sort direction from the controller
     * @return a Page of LoanApplication documents matching the given status
     */
    Page<LoanApplication> findByStatus(LoanStatus status, Pageable pageable);

    // ── Method 3: findByStatusAndCreatedAtBetween ─────────────────────────────
    /**
     * Find applications in a given status that were created within a date range.
     *
     * Use case: "Show me all SUBMITTED applications from the last 7 days."
     * Used for daily dashboard KPIs and SLA breach monitoring
     * (e.g., any SUBMITTED application older than 2 days is an SLA breach).
     *
     * Generated MongoDB query:
     *   db.loan_applications.find({
     *     "status": <status>,
     *     "created_at": { "$gte": <from>, "$lte": <to> }
     *   })
     *
     * Performance: covered by compound index (status, created_at):
     *   MongoDB can use this index to satisfy BOTH the equality filter on status
     *   AND the range filter on created_at — no full collection scan needed.
     *
     * @param status the loan lifecycle status to filter by
     * @param from   start of the date range (inclusive), in UTC
     * @param to     end of the date range (inclusive), in UTC
     * @return list of matching applications (add Pageable parameter if count can be large)
     */
    List<LoanApplication> findByStatusAndCreatedAtBetween(
        LoanStatus status,
        Instant from,
        Instant to
    );

    // ── Method 4: findByApplicantIdAndStatus ─────────────────────────────────
    /**
     * Find a specific applicant's applications filtered by status.
     *
     * Use case: "Does applicant X already have an active (non-terminal) application?"
     * Before allowing a new application submission, we check if they already have
     * one in SUBMITTED, KYC_PENDING, UNDER_REVIEW, CREDIT_CHECK, or APPROVED status.
     * This prevents parallel application abuse.
     *
     * Performance: covered by the compound index (applicant_id, status)
     *
     * @param applicantId the Applicant._id to check
     * @param status      the specific status to look for (e.g., UNDER_REVIEW)
     * @return Optional<LoanApplication> — empty if no such application exists
     */
    Optional<LoanApplication> findByApplicantIdAndStatus(String applicantId, LoanStatus status);

    // ── Method 5: countByStatus ───────────────────────────────────────────────
    /**
     * Count total applications in each status — for dashboard KPI counters.
     *
     * Use case: Dashboard header tiles:
     *   "Submitted: 42 | Under Review: 18 | Approved: 7 | Rejected: 3"
     * Called once per status for each tile.
     *
     * Generated query: db.loan_applications.countDocuments({ "status": <status> })
     *
     * @param status the loan status to count
     * @return the number of documents with that status
     */
    long countByStatus(LoanStatus status);

    // ── Method 6: findByAssignedOfficerIdAndStatus ───────────────────────────
    /**
     * Find all applications assigned to a specific loan officer in a given status.
     *
     * Use case: Personal work queue — when a loan officer logs in, they see only
     * the applications assigned to them, not the entire team's queue.
     * Supports workload management and accountability.
     *
     * @param officerId the User._id of the assigned loan officer
     * @param status    the status to filter (e.g., UNDER_REVIEW)
     * @param pageable  pagination parameters
     * @return a Page of applications assigned to this officer in the given status
     */
    Page<LoanApplication> findByAssignedOfficerIdAndStatus(
        String officerId,
        LoanStatus status,
        Pageable pageable
    );

    // ── Method 7: Custom @Query — Applications stalled in a status ────────────
    /**
     * Find applications that have been in the same status for longer than a given time.
     *
     * Use case: SLA breach monitoring — an application stuck in KYC_PENDING for
     * more than 5 days triggers an escalation alert. This query finds those stalled cases.
     *
     * Why @Query? — The condition checks BOTH status AND status_updated_at in a
     * combined filter. Spring Data's method name convention cannot express "status
     * updated BEFORE a certain time" cleanly — @Query with raw MongoDB syntax is clearer.
     *
     * MongoDB query explanation:
     *   Find all docs where status = <status> AND status_updated_at < <staleBefore>
     *   i.e., the status was last changed before the staleness threshold.
     *
     * @param status      the status to check for staleness (e.g., KYC_PENDING)
     * @param staleBefore applications whose status_updated_at is before this Instant are stale
     * @return list of stalled LoanApplication documents
     */
    @Query("{ 'status': ?0, 'status_updated_at': { $lt: ?1 } }")
    List<LoanApplication> findStalledApplications(LoanStatus status, Instant staleBefore);

    // ── Method 8: MongoDB Aggregation — Total loan amount by status ────────────
    /**
     * Aggregate the total loan amount requested, grouped by status.
     *
     * What is a MongoDB Aggregation Pipeline? (beginner explanation)
     * ──────────────────────────────────────────────────────────────
     * An aggregation pipeline is a sequence of data transformation "stages".
     * Each stage takes documents from the previous stage as input and passes
     * transformed results to the next stage. It's like a Unix pipe for data:
     *   collection → $match → $group → $sort → result
     *
     * This is equivalent to SQL:
     *   SELECT status, SUM(loan_amount) AS total, COUNT(*) AS count
     *   FROM loan_applications
     *   GROUP BY status
     *
     * Use case: Dashboard chart — "Total loan amount requested per status"
     * Tells portfolio managers how much money is at each stage of the funnel.
     *
     * @Aggregation — annotation that lets us write the pipeline as a JSON array
     *               of stage strings directly in the repository method.
     *
     * Returns List<Object[]> — each element is an array: [status, totalAmount, count]
     * In Phase 5, this will be mapped to a typed DTO for the dashboard API.
     *
     * @return list of [status, totalLoanAmount, applicationCount] tuples
     */
    @Aggregation(pipeline = {
        // Stage 1: $group — group all documents by their "status" field.
        // For each group, sum the loan_amount and count the documents.
        // _id: "$status" = use the status field value as the group key
        "{ $group: { _id: '$status', totalAmount: { $sum: '$loan_amount' }, count: { $sum: 1 } } }",
        // Stage 2: $sort — order results by totalAmount descending (largest first)
        "{ $sort: { totalAmount: -1 } }"
    })
    List<Object[]> aggregateTotalLoanAmountByStatus();

    // ── Method 9: existsByApplicantIdAndStatusIn ──────────────────────────────
    /**
     * Check if an applicant has any active (non-terminal) loan application.
     *
     * Use case: Duplicate application prevention — before accepting a new loan
     * application, check if this applicant already has one that hasn't been
     * closed (REJECTED or DISBURSED are terminal; all others are "active").
     *
     * $in operator — checks if the status field matches ANY value in the given list.
     * SQL equivalent: WHERE status IN ('SUBMITTED', 'KYC_PENDING', ...)
     *
     * @param applicantId  the Applicant._id to check
     * @param activeStatuses list of non-terminal LoanStatus values to check against
     * @return true if the applicant has at least one active application
     */
    boolean existsByApplicantIdAndStatusIn(String applicantId, List<LoanStatus> activeStatuses);
}
