package com.los.backend.engine.result;

import com.los.backend.model.enums.FinalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * EngineDecision — the aggregate output of the RuleEngine after evaluating
 * ALL active rules against one applicant's context.
 *
 * This is the "decision" that the RuleEngine hands back to the service layer.
 * The service layer then:
 *   1. Persists it as an embedded DecisionResult on the LoanApplication document.
 *   2. Persists a full DecisionLog document for the immutable audit trail.
 *   3. Updates the LoanApplication.status to APPROVED or REJECTED via the state machine.
 *
 * Contrast with RuleResult (one rule's verdict) and DecisionResult (the embedded
 * summary stored on LoanApplication). EngineDecision is the in-memory intermediate
 * object that flows between the engine and the service — it is never persisted directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineDecision {

    /**
     * finalDecision — the overall recommendation: APPROVED, REJECTED, or REVIEW.
     *
     * APPROVED  → riskScore ≤ approvalThreshold (default 40) AND no critical-rule failure
     * REJECTED  → riskScore > approvalThreshold OR a critical rule failed
     * REVIEW    → riskScore in a borderline band, OR missing data prevented full evaluation
     */
    private FinalDecision finalDecision;

    /**
     * riskScore — composite score on a 0–100 scale.
     * 0   = lowest possible risk (all rules passed, strong profile)
     * 100 = highest possible risk (all rules failed, very weak profile)
     *
     * Computed as: sum of (rule.weight * (passed ? 0 : 1)) across all evaluated rules.
     * Normalized to 0–100 if total weights don't sum to exactly 100.
     */
    private double riskScore;

    /**
     * roundedRiskScore — riskScore rounded to nearest integer (0–100).
     * Used for storage in MongoDB (DecisionResult.riskScore is Integer).
     * Avoids storing "42.857142857142854" when "43" is more readable.
     */
    private int roundedRiskScore;

    /**
     * approvalThresholdUsed — the riskScore threshold that was used to decide
     * APPROVED vs REJECTED. Stored for audit transparency.
     * Default: 40.0 (applications with riskScore ≤ 40 are APPROVED).
     * This can be configured per loan product via a MongoDB document in a future phase.
     */
    private double approvalThresholdUsed;

    /**
     * ruleResults — the individual verdict of EVERY rule that was evaluated.
     * Stored in DecisionLog for full audit. Also feeds the embedded
     * DecisionResult.ruleOutcomes summary on the LoanApplication.
     */
    private List<RuleResult> ruleResults;

    /**
     * evaluatedAt — when the engine completed evaluation (server UTC time).
     */
    private Instant evaluatedAt;

    /**
     * engineVersion — the version string of this BRE implementation.
     * Hardcoded per release so DecisionLog records are traceable to code versions.
     * Increment when the engine's aggregation logic changes.
     */
    private String engineVersion;

    /**
     * totalRulesEvaluated — how many rules were evaluated.
     * Sanity check: if 0, the engine had no active rules — misconfiguration alert.
     */
    private int totalRulesEvaluated;

    /**
     * rulesPassed — how many rules the applicant satisfied.
     * Displayed in the officer dashboard: "Passed 4 of 5 rules"
     */
    private int rulesPassed;

    /**
     * overallComment — auto-generated summary for the loan officer's review screen.
     * The engine builds this from the failed rule reasons.
     * Example: "REJECTED. Risk score 65/100. Failed: [DTI ratio 52% > 40%, Age 19 < 21]"
     */
    private String overallComment;

    /**
     * missingDataFields — fields that were null/missing at evaluation time.
     * If creditScore is null (NTC applicant), the engine notes it here.
     * Useful for triggering targeted data-gathering workflows.
     */
    private List<String> missingDataFields;
}
