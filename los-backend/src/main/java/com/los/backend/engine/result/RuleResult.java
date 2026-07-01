package com.los.backend.engine.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * RuleResult — the verdict of ONE rule evaluated against ONE applicant context.
 *
 * WHY THIS IS NOT AN IF-ELSE CHAIN — INTERVIEW ANSWER:
 * ══════════════════════════════════════════════════════
 * An if-else chain hard-codes every condition and threshold in procedural code:
 *
 *   if (creditScore < 650) { reject("low score"); }
 *   else if (dti > 0.4)    { reject("high DTI");  }
 *   // ... 50 more else-ifs
 *
 * Problems with that:
 *   1. Every threshold change requires a code change + recompile + redeploy.
 *   2. Adding a new rule means editing a giant method — high regression risk.
 *   3. You cannot A/B test rule variants, version rules, or enable/disable
 *      individual rules without code changes.
 *   4. It is untestable in isolation — you cannot unit-test "just the DTI check"
 *      without invoking the entire method.
 *
 * Our design instead uses:
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  Rule interface (Strategy pattern)                      │
 *   │    → each rule is a SEPARATE, independently testable    │
 *   │      class that reads its thresholds from MongoDB       │
 *   │                                                         │
 *   │  RuleEngine (Chain-of-Responsibility + Aggregator)      │
 *   │    → loads rules at runtime via Spring DI               │
 *   │    → evaluates them in sequence                         │
 *   │    → aggregates weighted results into one decision      │
 *   │                                                         │
 *   │  CreditRule MongoDB documents (Data-Driven Config)      │
 *   │    → thresholds live in the DB, not in code             │
 *   │    → changing a threshold = one MongoDB update, no deploy│
 *   └─────────────────────────────────────────────────────────┘
 *
 * This makes it a genuine "rule engine" because:
 *   - Rules are open/closed: add a new rule by creating a new class + DB document.
 *     The engine discovers it via Spring's @Component scanning — no engine changes.
 *   - Rules are data-driven: the DB document is the configuration; code is the executor.
 *   - Rules are composable: the aggregation logic (weighted score) is separate from
 *     individual rule logic — change scoring without touching any rule class.
 *   - Rules are individually testable: each Rule implementation is a clean unit.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * RuleResult — the output of evaluating one Rule against one applicant context.
 *
 * The RuleEngine collects one RuleResult per active Rule and then aggregates
 * them all into a final EngineDecision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    /**
     * ruleName — the human-readable name of the rule that produced this result.
     * Example: "Minimum CIBIL Credit Score Check"
     * Stored in DecisionLog so loan officers can see exactly which rule fired.
     */
    private String ruleName;

    /**
     * ruleType — the category of check (e.g., "CREDIT_SCORE", "DEBT_TO_INCOME").
     * Stored as a String (not the enum) so DecisionLog serialisation is simple
     * and the log stays readable even if the enum class is later renamed.
     */
    private String ruleType;

    /**
     * passed — the binary verdict: did this rule's condition pass or fail?
     *
     * true  = the applicant satisfies this rule's requirement
     * false = the applicant violates this rule's requirement
     *
     * The RuleEngine uses the weighted score of all results to compute the
     * final decision — a single failure does NOT always mean rejection
     * (unless the rule is marked critical — see weight below).
     */
    private boolean passed;

    /**
     * reason — a plain-English explanation of the verdict.
     * This is the key transparency field — it explains the decision to the
     * applicant and satisfies RBI's fair lending disclosure requirements.
     *
     * Examples:
     *   passed:  "Credit score 720 ≥ minimum threshold 650. PASS."
     *   failed:  "DTI ratio 52.3% exceeds maximum allowed 40.0%. FAIL."
     *   missing: "Credit score not available (New-To-Credit). Applying conservative penalty."
     */
    private String reason;

    /**
     * weight — the relative importance of this rule in the composite riskScore (0–100).
     *
     * WHY WEIGHTED SCORE INSTEAD OF "ALL RULES MUST PASS"?
     * ──────────────────────────────────────────────────────
     * An "all-must-pass" engine treats every rule as equally critical. In reality,
     * lending is probabilistic:
     *   - A borderline DTI (41% vs threshold 40%) with excellent credit (800+) is
     *     still a good lending risk — an all-must-pass engine rejects it wrongly.
     *   - A weighted score reflects the nuanced reality: strong performance on
     *     high-weight rules can offset mild failure on low-weight rules.
     *   - This is how real credit scoring models (FICO, CIBIL scoring algorithms)
     *     work — they weight factors by historical predictive power.
     *
     * The weight comes from the CreditRule MongoDB document (configurable per rule).
     * All active rule weights should sum to 100 for a clean percentage interpretation.
     */
    private double weight;

    /**
     * actualValue — the applicant's measured value for this rule's dimension.
     * Stored as String for cross-type flexibility.
     * Examples: "720" (credit score), "38.5" (DTI%), "SALARIED" (employment type)
     */
    private String actualValue;

    /**
     * thresholdValue — the rule's configured threshold from the MongoDB CreditRule doc.
     * Examples: "650" (min score), "40.0" (max DTI%), "21" (min age)
     */
    private String thresholdValue;

    /**
     * partialRiskScore — this rule's contribution to the composite risk score.
     *
     * Computed by the RuleEngine:
     *   partialRiskScore = passed ? 0 : weight
     * (A passed rule contributes 0 risk; a failed rule contributes its full weight as risk)
     *
     * overallRiskScore = sum(partialRiskScore) across all rules
     * A perfect applicant scores 0. The worst possible applicant scores 100.
     */
    private double partialRiskScore;

    /**
     * configSource — where this rule's threshold came from.
     * "MONGODB" = loaded from active CreditRule document (data-driven)
     * "DEFAULT" = no active document found; safe hardcoded fallback was used
     *
     * This field makes the system self-documenting in the audit log:
     * if a decision was made using defaults, a data engineer can investigate
     * why no CreditRule document was active for that rule type.
     */
    private String configSource;
}
