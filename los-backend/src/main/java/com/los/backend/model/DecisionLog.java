package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ENTITY: DecisionLog
 * MongoDB Collection: "decision_logs"
 *
 * WHY A SEPARATE COLLECTION (not embedded in LoanApplication)?
 * ─────────────────────────────────────────────────────────────
 * The DecisionLog is the immutable, verbose audit record of one BRE run.
 * It is kept separate from LoanApplication because:
 *
 * 1. SIZE: A full log includes verbose reasons for every rule (e.g., 6 rules
 *    × 3 fields each = 18 fields + metadata). Over thousands of applications,
 *    embedding would significantly bloat the loan_applications collection,
 *    slowing down the most common queries (list applications, check status).
 *
 * 2. IMMUTABILITY: DecisionLog is a write-once audit record — it must NEVER
 *    be modified after creation. Keeping it in its own collection makes it
 *    easy to enforce write-once semantics at the repository layer (no save()
 *    after initial insert).
 *
 * 3. RE-EVALUATION: The BRE can be re-run on an application (e.g., after an
 *    applicant updates their income or a loan officer requests a re-check).
 *    Each run creates a NEW DecisionLog, giving a complete history of all
 *    evaluations. We find all logs for an application via loanApplicationId.
 *
 * 4. COMPLIANCE REPORTING: Regulators may request "all credit decisions for
 *    applicant X in year 2024". Having decision_logs as a dedicated collection
 *    with proper indexes makes these ad-hoc compliance queries efficient.
 *
 * 5. SEPARATION OF CONCERNS: LoanApplication = workflow state.
 *    DecisionLog = immutable fact record. Different access patterns,
 *    different retention policies, different indexes.
 *
 * REFERENCING STRATEGY:
 *   DecisionLog stores loanApplicationId (references LoanApplication._id).
 *   LoanApplication stores only the embedded DecisionResult summary.
 *   To get the full log for an application: query DecisionLog by loanApplicationId.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import com.los.backend.model.enums.FinalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * DecisionLog — the immutable, complete audit record of one BRE evaluation.
 *
 * WRITE-ONCE CONTRACT:
 * Once persisted, this document must never be updated. The repository
 * exposes only save() (for creation) and find methods — no update methods.
 * This ensures the audit trail is tamper-proof (critical for regulatory compliance).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "decision_logs")
// Compound index: find all logs for one application, newest first
@CompoundIndex(
    name = "idx_app_evaluated",
    def = "{'loan_application_id': 1, 'evaluated_at': -1}",
    background = true
)
public class DecisionLog {

    /**
     * id — MongoDB auto-generated ObjectId for this log entry.
     * Each BRE run creates a new DecisionLog with a new unique ID.
     */
    @Id
    private String id;

    /**
     * loanApplicationId — the _id of the LoanApplication this evaluation was run for.
     * The primary foreign-key reference linking DecisionLog → LoanApplication.
     * @Indexed — we query "all decision logs for this application" very frequently.
     */
    @NotBlank(message = "Loan application ID is required")
    @Indexed
    @Field("loan_application_id")
    private String loanApplicationId;

    /**
     * applicantId — redundant but intentionally stored here for query efficiency.
     * Allows "find all decisions for applicant X" WITHOUT a join through loan applications.
     * This is an intentional denormalisation: we trade a small amount of data
     * redundancy for a significant query performance gain on compliance reports.
     */
    @NotBlank
    @Field("applicant_id")
    private String applicantId;

    /**
     * evaluatedAt — exact UTC timestamp when the BRE completed this evaluation.
     * @NotNull — every log MUST record when the decision was made.
     */
    @NotNull
    @Field("evaluated_at")
    private Instant evaluatedAt;

    /**
     * ruleResults — the individual verdict of every CreditRule that was evaluated.
     * Each entry tells us: which rule ran, did it pass or fail, and exactly why.
     * This is the verbose detail that lets us explain a rejection to a regulator.
     */
    @Field("rule_results")
    private List<RuleResult> ruleResults;

    /**
     * overallRiskScore — the composite risk score (0–100) computed by the BRE.
     * 0 = safest possible profile, 100 = highest risk profile.
     * Computed as a weighted average of individual rule scores.
     */
    @Field("overall_risk_score")
    private Integer overallRiskScore;

    /**
     * finalDecision — the BRE's overall recommendation: APPROVED, REJECTED, or REVIEW.
     * @NotNull — every completed evaluation must have a final decision.
     */
    @NotNull
    @Field("final_decision")
    private FinalDecision finalDecision;

    /**
     * evaluatorVersion — the version string of the BRE engine that ran this evaluation.
     * Example: "1.0.0". Critical for post-hoc analysis:
     * "Did BRE version 1.0.0 produce systematically biased decisions?"
     */
    @NotBlank
    @Field("evaluator_version")
    private String evaluatorVersion;

    /**
     * rulesSnapshotVersion — a hash or version tag of the complete set of active
     * CreditRules used during this evaluation. If rules change after this log was created,
     * we can still reconstruct which exact rules produced this decision.
     * Example: "RULESET_20240601_v3"
     */
    @Field("rules_snapshot_version")
    private String rulesSnapshotVersion;

    /**
     * triggeredByUserId — the MongoDB User._id of the loan officer who triggered
     * this BRE run. Null if triggered automatically by the system.
     * Important for accountability: which officer initiated the credit check?
     */
    @Field("triggered_by_user_id")
    private String triggeredByUserId;

    /**
     * durationMs — how long the BRE took to complete this evaluation, in milliseconds.
     * Used for performance monitoring: alerts if BRE consistently takes > 2000ms.
     */
    @Field("duration_ms")
    private Long durationMs;

    /**
     * decisionComment — auto-generated human-readable summary of the decision.
     * Example: "Application REJECTED. Failure reasons: [Credit score 580 < 650, DTI 52% > 40%]"
     * Displayed to loan officers on the review screen.
     */
    @Field("decision_comment")
    private String decisionComment;

    // ── Inner class: RuleResult ───────────────────────────────────────────────
    // Embedded as a sub-array in this DecisionLog document.
    // One RuleResult per CreditRule evaluated during this BRE run.

    /**
     * RuleResult — the full verbose outcome of evaluating one CreditRule.
     * More detailed than DecisionResult.RuleOutcomeSummary (which is just for UI display).
     * This record is for audit and compliance — must be complete and unambiguous.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResult {

        /** The MongoDB _id of the CreditRule document that was evaluated */
        @Field("rule_id")
        private String ruleId;

        /** Human-readable rule name (snapshot at evaluation time, not a live reference) */
        @Field("rule_name")
        private String ruleName;

        /** The RuleType enum value — tells us what kind of check this was */
        @Field("rule_type")
        private String ruleType;

        /** The version of the CreditRule document used — for historical accuracy */
        @Field("rule_version")
        private Integer ruleVersion;

        /** true = this rule's condition was satisfied; false = rule condition failed */
        @Field("passed")
        private boolean passed;

        /**
         * actualValue — the applicant's actual data point evaluated by this rule.
         * Stored as String for flexibility across numeric, boolean, and enum types.
         * Examples: "580" (credit score), "52.3" (DTI %), "SALARIED" (employment type)
         */
        @Field("actual_value")
        private String actualValue;

        /**
         * thresholdValue — the rule's configured threshold that was compared against.
         * Examples: "650" (minimum score), "40" (max DTI %), "21" (minimum age)
         */
        @Field("threshold_value")
        private String thresholdValue;

        /**
         * reason — full, human-readable explanation of the rule outcome.
         * Examples:
         *   passed:  "Credit score 720 satisfies minimum threshold of 650. PASS."
         *   failed:  "DTI ratio 52.3% exceeds maximum allowed 40.0%. FAIL."
         * This is the key field for explaining decisions to regulators and applicants.
         */
        @Field("reason")
        private String reason;

        /**
         * ruleWeight — the weight of this rule (copied from CreditRule at eval time).
         * Stored here so the risk score can be audited and recalculated if needed,
         * even if the rule's weight changes after this log was created.
         */
        @Field("rule_weight")
        private Integer ruleWeight;

        /**
         * partialRiskScore — this rule's contribution to the overall risk score.
         * overallRiskScore = sum of all (ruleWeight × partialScore) / totalWeight
         */
        @Field("partial_risk_score")
        private Integer partialRiskScore;
    }
}
