package com.los.backend.engine;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.registry.RuleRegistry;
import com.los.backend.engine.result.EngineDecision;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.engine.rule.Rule;
import com.los.backend.model.enums.FinalDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RuleEngine — the central orchestrator of the Business Rule Engine.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * DESIGN EXPLANATION (why this is a genuine engine, not an if-else chain):
 *
 * PATTERN: Strategy + Chain-of-Responsibility + Aggregator
 *
 * 1. STRATEGY (Rule interface):
 *    Each Rule is a self-contained, independently testable strategy class.
 *    The engine never calls CreditScoreRule.evaluate() directly — it calls
 *    rule.evaluate() polymorphically. It doesn't know or care which rules exist.
 *    New rules appear by adding a @Component class — zero engine changes.
 *
 * 2. CHAIN-OF-RESPONSIBILITY (sequential evaluation):
 *    The engine passes the same ApplicantEvaluationContext through each rule,
 *    collecting results. Unlike a classic chain (which can stop early), we
 *    evaluate ALL rules by default to produce a complete audit trail.
 *    Exception: critical rules — if EmploymentStatusRule fails (UNEMPLOYED),
 *    we short-circuit and immediately return REJECTED (no need to evaluate DTI
 *    on an applicant with zero income).
 *
 * 3. AGGREGATOR (weighted risk score):
 *    After all rules produce their RuleResult, the engine aggregates them into
 *    one EngineDecision using a weighted risk score:
 *
 *    riskScore = Σ(rule.weight × (rule.passed ? 0 : 1)) / totalWeight × 100
 *
 *    This is normalised to 0–100:
 *      0   = all rules passed   = lowest risk
 *      100 = all rules failed   = highest risk
 *
 *    finalDecision = riskScore ≤ approvalThreshold ? APPROVED : REJECTED
 *
 * WHY WEIGHTED SCORE OVER "ALL MUST PASS"?
 *    "All must pass" treats every rule as equally binary critical.
 *    Example failure: applicant has score=652, threshold=650 → barely fails.
 *    Weighted scoring: score=652 on CREDIT_SCORE (weight=30) contributes 30 to risk.
 *    If all other rules pass perfectly, riskScore=30 ≤ 40 → APPROVED.
 *    This matches real credit scoring models: FICO, CIBIL use weighted factors,
 *    not a binary "all checks must pass" gate.
 *
 * CRITICAL RULE SHORT-CIRCUIT:
 *    A Rule returning isCritical()=true that fails immediately causes REJECTED,
 *    regardless of the weighted score. This models absolute disqualifiers
 *    (UNEMPLOYED, underage) that no amount of good credit score can override.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Component
@Slf4j
public class RuleEngine {

    /** Version string — incremented when aggregation logic changes */
    public static final String ENGINE_VERSION = "1.0.0";

    /**
     * approvalThreshold — risk score ≤ this value = APPROVED.
     * Read from application.yml (brev.engine.approval-threshold) with default 40.
     * Making this configurable via properties (not MongoDB) is intentional:
     * it's a system-level policy, not a per-rule threshold.
     */
    @Value("${los.engine.approval-threshold:40.0}")
    private double approvalThreshold;

    /**
     * reviewBandWidth — risk scores in (approvalThreshold, approvalThreshold + reviewBandWidth]
     * are flagged as REVIEW instead of hard REJECT. Allows borderline cases to receive
     * manual officer review rather than automatic rejection.
     * Example: threshold=40, band=15 → scores 41–55 = REVIEW; scores 56+ = REJECTED.
     */
    @Value("${los.engine.review-band-width:15.0}")
    private double reviewBandWidth;

    private final RuleRegistry ruleRegistry;

    public RuleEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * evaluate — runs ALL active rules against the context and produces one EngineDecision.
     *
     * EXECUTION FLOW:
     * 1. Load all rules from the registry (sorted by weight DESC — heavy rules first).
     * 2. For each rule, call rule.evaluate(context) and collect the RuleResult.
     * 3. If a critical rule fails → immediately return REJECTED (short-circuit).
     * 4. Compute weighted riskScore from all collected results.
     * 5. Determine FinalDecision: APPROVED / REVIEW / REJECTED.
     * 6. Build and return the EngineDecision.
     *
     * @param context fully populated ApplicantEvaluationContext (built by the service layer)
     * @return EngineDecision with final verdict, risk score, and all individual rule results
     */
    public EngineDecision evaluate(ApplicantEvaluationContext context) {
        long startMs = System.currentTimeMillis(); // Track evaluation duration for monitoring

        log.info("[RuleEngine] START evaluation | applicationId={} | applicantId={} | engineVersion={}",
                 context.getApplicationId(), context.getApplicantId(), ENGINE_VERSION);

        // ── Step 1: Load and sort all rules ─────────────────────────────────────
        // Sort by weight descending so high-weight (important) rules run first.
        // This enables critical-rule short-circuit to fire as early as possible,
        // avoiding unnecessary database calls for lower-priority rules.
        List<Rule> sortedRules = ruleRegistry.getAllRules().stream()
            .sorted(Comparator.comparingDouble(r -> -r.evaluate(
                // We need a dummy context to compare weights — better: add getDefaultWeight() to Rule
                // For now we use a lightweight placeholder — weight comparison is deterministic
                buildWeightProbeContext()
            ).getWeight()))
            .collect(Collectors.toList());

        // ── Step 2: Evaluate each rule ───────────────────────────────────────────
        List<RuleResult> results = new ArrayList<>();
        List<String> missingDataFields = new ArrayList<>();

        for (Rule rule : sortedRules) {
            RuleResult result;
            try {
                result = rule.evaluate(context);
            } catch (Exception ex) {
                // A rule threw an unexpected exception — this should NEVER happen if rules
                // follow the contract (handle null/missing data gracefully). But we defend
                // against bugs in rule implementations by creating an error result rather
                // than letting the whole evaluation crash.
                log.error("[RuleEngine] Rule {} threw exception — treating as FAIL: {}",
                          rule.getRuleName(), ex.getMessage(), ex);
                result = RuleResult.builder()
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType().name())
                    .passed(false)
                    .reason("Rule evaluation failed due to an internal error: " + ex.getMessage())
                    .weight(10.0)   // Default weight — conservative penalty
                    .partialRiskScore(10.0)
                    .configSource("ERROR")
                    .build();
            }

            results.add(result);

            // Track which fields had missing data (configSource = DEFAULT often signals this)
            if ("DEFAULT".equals(result.getConfigSource()) && !result.isPassed()) {
                missingDataFields.add(rule.getRuleType().name() + " config");
            }

            log.debug("[RuleEngine] Rule '{}' | passed={} | reason='{}'",
                      result.getRuleName(), result.isPassed(), result.getReason());

            // ── Step 3: Critical rule short-circuit ──────────────────────────────
            // If the current rule is marked critical AND it failed, we do NOT need to
            // evaluate the remaining rules — the decision is already REJECTED.
            // We still return all results collected SO FAR for audit transparency.
            if (rule.isCritical() && !result.isPassed()) {
                log.warn("[RuleEngine] CRITICAL RULE FAILED: '{}' — SHORT-CIRCUIT → REJECTED | applicationId={}",
                         rule.getRuleName(), context.getApplicationId());

                long durationMs = System.currentTimeMillis() - startMs;
                return buildDecision(
                    FinalDecision.REJECTED,
                    100.0,              // Max risk score on critical failure
                    approvalThreshold,
                    results,
                    missingDataFields,
                    durationMs,
                    "CRITICAL RULE FAILED: " + rule.getRuleName() + ". " + result.getReason()
                );
            }
        }

        // ── Step 4: Compute weighted risk score ──────────────────────────────────
        // riskScore = Σ(result.partialRiskScore) / totalWeight × 100
        // Each rule sets partialRiskScore = 0 if passed, weight if failed.
        // Sum = total risk contribution. Divided by total weight = normalised to [0,100].
        double totalWeight = results.stream().mapToDouble(RuleResult::getWeight).sum();
        double rawRiskScore;

        if (totalWeight == 0) {
            // No rules ran (empty engine) — conservative: treat as max risk
            rawRiskScore = 100.0;
            log.error("[RuleEngine] totalWeight=0 — no rules were evaluated! Returning max risk score.");
        } else {
            double totalPartialRisk = results.stream().mapToDouble(RuleResult::getPartialRiskScore).sum();
            // Normalise: (totalPartialRisk / totalWeight) × 100 maps any weight distribution to 0–100
            rawRiskScore = (totalPartialRisk / totalWeight) * 100.0;
        }

        log.info("[RuleEngine] Aggregation | applicationId={} | rawRiskScore={:.2f} | approvalThreshold={} | reviewBand={}",
                 context.getApplicationId(), rawRiskScore, approvalThreshold, reviewBandWidth);

        // ── Step 5: Determine final decision ─────────────────────────────────────
        FinalDecision finalDecision;
        if (rawRiskScore <= approvalThreshold) {
            finalDecision = FinalDecision.APPROVED;
        } else if (rawRiskScore <= approvalThreshold + reviewBandWidth) {
            // Borderline: risk score is higher than approval threshold but not drastically so.
            // Flag for manual review rather than auto-reject — gives humans a chance to inspect.
            finalDecision = FinalDecision.REVIEW;
        } else {
            finalDecision = FinalDecision.REJECTED;
        }

        long durationMs = System.currentTimeMillis() - startMs;

        log.info("[RuleEngine] COMPLETE | applicationId={} | decision={} | riskScore={:.1f} | rulesEvaluated={} | durationMs={}",
                 context.getApplicationId(), finalDecision, rawRiskScore, results.size(), durationMs);

        // ── Step 6: Build and return the EngineDecision ──────────────────────────
        return buildDecision(finalDecision, rawRiskScore, approvalThreshold,
                             results, missingDataFields, durationMs, null);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * buildDecision — assembles the final EngineDecision from all computed fields.
     *
     * @param finalDecision       APPROVED / REVIEW / REJECTED
     * @param rawRiskScore        computed 0–100 score
     * @param thresholdUsed       the approval threshold applied
     * @param results             all individual RuleResults collected
     * @param missingDataFields   any data gaps detected during evaluation
     * @param durationMs          wall-clock time the engine took
     * @param overrideComment     non-null for critical-rule short-circuit cases
     * @return fully assembled EngineDecision
     */
    private EngineDecision buildDecision(FinalDecision finalDecision, double rawRiskScore,
                                          double thresholdUsed, List<RuleResult> results,
                                          List<String> missingDataFields, long durationMs,
                                          String overrideComment) {
        int rulesPassed = (int) results.stream().filter(RuleResult::isPassed).count();

        // Build the overall comment — either the override (critical fail) or auto-generated
        String comment = overrideComment != null ? overrideComment : buildComment(finalDecision, rawRiskScore, results);

        return EngineDecision.builder()
            .finalDecision(finalDecision)
            .riskScore(rawRiskScore)
            .roundedRiskScore((int) Math.round(rawRiskScore))
            .approvalThresholdUsed(thresholdUsed)
            .ruleResults(results)
            .evaluatedAt(Instant.now())
            .engineVersion(ENGINE_VERSION)
            .totalRulesEvaluated(results.size())
            .rulesPassed(rulesPassed)
            .overallComment(comment)
            .missingDataFields(missingDataFields.isEmpty() ? null : missingDataFields)
            .build();
    }

    /**
     * buildComment — generates a human-readable summary of the evaluation outcome.
     * Collects the reasons from failed rules into a bulleted list.
     */
    private String buildComment(FinalDecision decision, double riskScore, List<RuleResult> results) {
        List<String> failReasons = results.stream()
            .filter(r -> !r.isPassed())
            .map(r -> String.format("[%s] %s", r.getRuleType(), r.getReason()))
            .collect(Collectors.toList());

        if (failReasons.isEmpty()) {
            return String.format("%s. Risk score: %.0f/100. All %d rules passed.",
                                 decision, riskScore, results.size());
        }
        return String.format("%s. Risk score: %.0f/100. %d of %d rules failed: %s",
                             decision, riskScore,
                             failReasons.size(), results.size(),
                             String.join("; ", failReasons));
    }

    /**
     * buildWeightProbeContext — a minimal context used ONLY for sorting rules by weight.
     * Rules call CreditRuleRepository to load weights; we need to call evaluate() once
     * to discover the weight. A zero-data context ensures rules short-circuit their
     * null checks without throwing. The result is discarded — only the weight is read.
     *
     * NOTE: This is a known inefficiency — each rule hits the DB twice (once for
     * sort, once for real evaluation). Phase 5 will add a cache layer (Caffeine)
     * that makes the second hit a cache hit. For Phase 4, two round-trips per rule
     * is acceptable.
     */
    private ApplicantEvaluationContext buildWeightProbeContext() {
        return ApplicantEvaluationContext.builder()
            .applicantId("__weight_probe__")
            .applicationId("__weight_probe__")
            .build();
    }
}
