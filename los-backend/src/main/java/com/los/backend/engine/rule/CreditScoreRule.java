package com.los.backend.engine.rule;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.model.CreditRule;
import com.los.backend.model.enums.RuleType;
import com.los.backend.repository.CreditRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CreditScoreRule — evaluates whether the applicant's CIBIL credit score
 * meets the minimum threshold configured in the MongoDB CreditRule document.
 *
 * DATA-DRIVEN THRESHOLD LOADING PATTERN:
 * ─────────────────────────────────────────────────────────────────────────
 * This rule demonstrates the "data-driven rule engine" design:
 *
 * Step 1: At evaluation time (NOT at startup), this rule calls the repository
 *         to load the active CreditRule document of type CREDIT_SCORE.
 * Step 2: It reads the "minimumScore" key from the document's parameters Map.
 * Step 3: If no active document is found, it uses a hardcoded DEFAULT value.
 *
 * Why load at evaluation time, not at startup?
 *   - Loading at startup = cached value; changing MongoDB doesn't affect the running app.
 *   - Loading at evaluation = always fresh; changing MongoDB takes effect on the next call.
 *   - For a production system with high call volume, Phase 5 would add a short-lived
 *     cache (e.g., Caffeine cache with 5-minute TTL) to avoid a DB round-trip per
 *     evaluation while still picking up changes within 5 minutes.
 *
 * NEW-TO-CREDIT (NTC) HANDLING:
 * ─────────────────────────────────────────────────────────────────────────
 * Some applicants have never borrowed before and have no credit score.
 * An all-or-nothing check (score < minimum → REJECT) would permanently exclude
 * all first-time borrowers — bad for financial inclusion.
 * Instead, NTC applicants receive a CONSERVATIVE PENALTY SCORE (configurable via
 * parameters.ntcPenaltyScore, default 550) which typically causes REVIEW rather
 * than outright rejection. The loan officer can then request additional income
 * verification before deciding.
 */
@Component  // Spring discovers this as a Rule bean — the engine gets it via @Autowired List<Rule>
@Slf4j
public class CreditScoreRule implements Rule {

    // Default threshold used when no active CreditRule document exists for CREDIT_SCORE
    // 650 is a commonly used minimum CIBIL score for personal loans in India
    private static final int DEFAULT_MINIMUM_SCORE = 650;

    // Default weight for this rule — should sum to 100 across all active rules
    private static final double DEFAULT_WEIGHT = 30.0;

    // Score assigned to NTC applicants (no credit history) — conservative but not automatic rejection
    private static final int DEFAULT_NTC_PENALTY_SCORE = 550;

    // Repository to load threshold parameters from MongoDB at evaluation time
    private final CreditRuleRepository creditRuleRepository;

    public CreditScoreRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    // ── Rule interface implementation ─────────────────────────────────────────

    @Override
    public RuleType getRuleType() {
        // This tells the RuleEngine: "I am the CREDIT_SCORE evaluator"
        return RuleType.CREDIT_SCORE;
    }

    @Override
    public String getRuleName() {
        return "Minimum CIBIL Credit Score Check";
    }

    /**
     * evaluate — core rule logic: compare applicant's credit score to the configured minimum.
     *
     * @param context pre-built context object containing the applicant's creditScore field
     * @return RuleResult with pass/fail verdict and human-readable reason
     */
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[CreditScoreRule] Evaluating applicantId={} creditScore={}",
                  context.getApplicantId(), context.getCreditScore());

        // ── Step 1: Load config from MongoDB (data-driven threshold) ──────────
        // findByRuleTypeAndIsActiveTrue returns ALL active rules of this type.
        // We expect exactly one — take the first if multiple exist (shouldn't happen
        // in a well-governed system, but we're defensive here).
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.CREDIT_SCORE);

        int minimumScore;
        double weight;
        int ntcPenaltyScore;
        String configSource;

        if (activeRules.isEmpty()) {
            // No MongoDB config found — use safe hardcoded defaults
            // This should alert an admin (the RuleEngine logs a warning when configSource=DEFAULT)
            minimumScore   = DEFAULT_MINIMUM_SCORE;
            weight         = DEFAULT_WEIGHT;
            ntcPenaltyScore = DEFAULT_NTC_PENALTY_SCORE;
            configSource   = "DEFAULT";
            log.warn("[CreditScoreRule] No active CreditRule found for CREDIT_SCORE — using defaults");
        } else {
            // Extract parameters from the MongoDB document's flexible Map<String, Object>
            Map<String, Object> params = activeRules.get(0).getParameters();
            // ((Number) value).intValue() handles both Integer and Double stored by MongoDB driver
            minimumScore   = params.containsKey("minimumScore")
                ? ((Number) params.get("minimumScore")).intValue() : DEFAULT_MINIMUM_SCORE;
            ntcPenaltyScore = params.containsKey("ntcPenaltyScore")
                ? ((Number) params.get("ntcPenaltyScore")).intValue() : DEFAULT_NTC_PENALTY_SCORE;
            weight         = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            configSource   = "MONGODB";
        }

        // ── Step 2: Handle New-To-Credit (no score available) ─────────────────
        Integer score = context.getCreditScore();
        if (score == null) {
            // NTC applicant — apply penalty score rather than hard-reject
            boolean ntcPasses = ntcPenaltyScore >= minimumScore;
            String reason = String.format(
                "Credit score not available (New-To-Credit applicant). " +
                "Applying NTC penalty score of %d. Required minimum: %d. %s",
                ntcPenaltyScore, minimumScore, ntcPasses ? "BORDERLINE PASS." : "FAIL — manual review required."
            );
            log.debug("[CreditScoreRule] NTC applicant applicantId={} → ntcScore={} threshold={} passed={}",
                      context.getApplicantId(), ntcPenaltyScore, minimumScore, ntcPasses);

            return RuleResult.builder()
                .ruleName(getRuleName())
                .ruleType(getRuleType().name())
                .passed(ntcPasses)
                .reason(reason)
                .weight(weight)
                .actualValue("NTC/" + ntcPenaltyScore)
                .thresholdValue(String.valueOf(minimumScore))
                .partialRiskScore(ntcPasses ? 0 : weight)
                .configSource(configSource)
                .build();
        }

        // ── Step 3: Evaluate the actual score ─────────────────────────────────
        boolean passed = score >= minimumScore;
        String reason = passed
            ? String.format("Credit score %d ≥ minimum threshold %d. PASS.", score, minimumScore)
            : String.format("Credit score %d is below the minimum required score of %d. FAIL.", score, minimumScore);

        log.debug("[CreditScoreRule] applicantId={} score={} threshold={} passed={}",
                  context.getApplicantId(), score, minimumScore, passed);

        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(String.valueOf(score))
            .thresholdValue(String.valueOf(minimumScore))
            .partialRiskScore(passed ? 0.0 : weight)
            .configSource(configSource)
            .build();
    }
}
