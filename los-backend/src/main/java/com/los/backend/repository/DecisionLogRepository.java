package com.los.backend.repository;

import com.los.backend.model.DecisionLog;
import com.los.backend.model.enums.FinalDecision;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * DecisionLogRepository — data access for the "decision_logs" MongoDB collection.
 *
 * WRITE-ONCE ENFORCEMENT:
 * This repository intentionally exposes NO update methods beyond the inherited save()
 * (which we use only for initial creation). Service layer code MUST NOT call save()
 * on an already-persisted DecisionLog — doing so would corrupt the audit trail.
 * In Phase 5, we'll add MongoDB's document-level write protection via change streams.
 *
 * The compound index (loan_application_id + evaluated_at DESC) defined in
 * DecisionLog.java covers all primary query patterns here.
 */
@Repository
public interface DecisionLogRepository extends MongoRepository<DecisionLog, String> {

    // ── Method 1: findByLoanApplicationIdOrderByEvaluatedAtDesc ──────────────
    /**
     * Find all BRE evaluation logs for a specific loan application, newest first.
     *
     * Use case: Loan officer audit view — "Show me the full decision history for
     * application #XYZ." Multiple entries appear if:
     * a) The BRE was re-triggered after income data was updated.
     * b) A loan officer overrode the first decision and re-evaluated.
     * c) A bug caused a failed run and a successful retry.
     *
     * The most recent (index 0) is the current effective decision.
     * All previous entries are preserved for the audit trail.
     *
     * Generated query:
     *   db.decision_logs.find({ "loan_application_id": <id> })
     *                   .sort({ "evaluated_at": -1 })
     * Performance: covered by compound index (loan_application_id, evaluated_at)
     *
     * @param loanApplicationId the LoanApplication._id whose logs we want
     * @return list of DecisionLog documents, most recent first
     */
    List<DecisionLog> findByLoanApplicationIdOrderByEvaluatedAtDesc(String loanApplicationId);

    // ── Method 2: findTopByLoanApplicationIdOrderByEvaluatedAtDesc ────────────
    /**
     * Find the MOST RECENT decision log for a specific loan application.
     *
     * Use case: Quick status display — the API endpoint that returns
     * "the current BRE decision" for a loan application. We want only the
     * latest evaluation, not the full history.
     *
     * "findTop1By..." — Spring Data's keyword for LIMIT 1.
     * Combined with OrderByEvaluatedAtDesc, this returns the single newest record.
     *
     * SQL equivalent: SELECT TOP 1 * FROM decision_logs WHERE loan_application_id = ?
     *                 ORDER BY evaluated_at DESC
     *
     * @param loanApplicationId the LoanApplication._id
     * @return Optional containing the latest DecisionLog, or empty if BRE hasn't run yet
     */
    Optional<DecisionLog> findTopByLoanApplicationIdOrderByEvaluatedAtDesc(String loanApplicationId);

    // ── Method 3: findByApplicantIdAndFinalDecision ───────────────────────────
    /**
     * Find all decision logs for a specific applicant with a given final decision.
     *
     * Use case: Compliance report — "Show all REJECTED decisions for applicant X."
     * Regulators may require lenders to explain every rejection for an applicant.
     * Having applicantId directly on DecisionLog (denormalised) makes this query
     * fast without needing a join through loan_applications.
     *
     * @param applicantId   the Applicant._id
     * @param finalDecision the decision outcome to filter by (APPROVED/REJECTED/REVIEW)
     * @return list of matching DecisionLog documents
     */
    List<DecisionLog> findByApplicantIdAndFinalDecision(String applicantId, FinalDecision finalDecision);

    // ── Method 4: findByEvaluatedAtBetweenAndFinalDecision ───────────────────
    /**
     * Find all decisions of a given type within a date range.
     *
     * Use case: Risk management reporting — "How many REJECTED decisions did the BRE
     * make in June 2024? What was the approval rate this month?"
     *
     * Generated query:
     *   db.decision_logs.find({
     *     "evaluated_at": { "$gte": <from>, "$lte": <to> },
     *     "final_decision": <finalDecision>
     *   })
     *
     * @param from          start of the date range (UTC Instant)
     * @param to            end of the date range (UTC Instant)
     * @param finalDecision the BRE outcome to count (e.g., FinalDecision.REJECTED)
     * @return list of DecisionLog documents matching the date range and decision type
     */
    List<DecisionLog> findByEvaluatedAtBetweenAndFinalDecision(
        Instant from,
        Instant to,
        FinalDecision finalDecision
    );

    // ── Method 5: Custom @Query — Decisions by a specific BRE version ─────────
    /**
     * Find all decision logs produced by a specific BRE evaluator version.
     *
     * Use case: BRE version impact analysis — "After we deployed BRE v1.2.0, did
     * the rejection rate change? Were any decisions made by v1.1.0 incorrect?"
     * This query lets us audit all decisions tied to a specific engine version.
     *
     * @param evaluatorVersion the BRE version string (e.g., "1.0.0", "1.2.0")
     * @return list of DecisionLog documents created by that evaluator version
     */
    @Query("{ 'evaluator_version': ?0 }")
    List<DecisionLog> findByEvaluatorVersion(String evaluatorVersion);

    // ── Method 6: Aggregation — Decision count grouped by final decision type ──
    /**
     * Aggregate a count of decisions grouped by their final decision type.
     *
     * What this pipeline does, step by step:
     * Stage 1 ($group): Group all documents by "final_decision" value.
     *                   For each group, count the number of documents.
     * Stage 2 ($sort):  Sort by count descending (most frequent first).
     *
     * SQL equivalent:
     *   SELECT final_decision, COUNT(*) as count
     *   FROM decision_logs
     *   GROUP BY final_decision
     *   ORDER BY count DESC
     *
     * Use case: Dashboard KPI — "APPROVED: 1,234 | REJECTED: 456 | REVIEW: 23"
     *
     * @return list of [finalDecision, count] tuples as Object arrays
     */
    @Aggregation(pipeline = {
        // Group by final_decision, count occurrences
        "{ $group: { _id: '$final_decision', count: { $sum: 1 } } }",
        // Sort by count descending
        "{ $sort: { count: -1 } }"
    })
    List<Object[]> countDecisionsByFinalDecision();

    // ── Method 7: countByFinalDecisionAndEvaluatedAtBetween ──────────────────
    /**
     * Count decisions of a given type within a date range — for approval rate calculation.
     *
     * Use case: Approval rate KPI = APPROVED count / (APPROVED + REJECTED count).
     * Called twice (once for APPROVED, once for REJECTED) to compute the ratio.
     *
     * @param finalDecision the outcome type to count
     * @param from          start of the period
     * @param to            end of the period
     * @return count of matching DecisionLog documents
     */
    long countByFinalDecisionAndEvaluatedAtBetween(
        FinalDecision finalDecision,
        Instant from,
        Instant to
    );
}
