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
 * DebtToIncomeRatioRule — evaluates whether the applicant's monthly debt burden
 * is within an acceptable fraction of their monthly income.
 *
 * WHAT IS DTI? (Debt-to-Income Ratio)
 * ─────────────────────────────────────
 * DTI = totalMonthlyEMI / monthlyGrossIncome
 * Example: If you earn ₹50,000/month and pay ₹18,000/month in existing EMIs:
 *          DTI = 18,000 / 50,000 = 0.36 = 36%
 *
 * Why DTI matters:
 *   A high DTI means most of an applicant's income is already committed to debt.
 *   Adding a new loan EMI could push them into financial distress — they may default.
 *   Most Indian lenders cap DTI at 40–50% (RBI's informal guidance is ≤40% for retail loans).
 *
 * The pre-computed debtToIncomeRatio field in ApplicantEvaluationContext means
 * this rule only needs to compare one double — no arithmetic here.
 */
@Component
@Slf4j
public class DebtToIncomeRatioRule implements Rule {

    private static final double DEFAULT_MAXIMUM_DTI = 0.40;  // 40% maximum DTI
    private static final double DEFAULT_WEIGHT       = 25.0; // 25 out of 100 total weight

    private final CreditRuleRepository creditRuleRepository;

    public DebtToIncomeRatioRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    @Override
    public RuleType getRuleType() { return RuleType.DEBT_TO_INCOME; }

    @Override
    public String getRuleName() { return "Debt-to-Income Ratio Check"; }

    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[DTIRule] Evaluating applicantId={} dti={}",
                  context.getApplicantId(), context.getDebtToIncomeRatio());

        // Load config from MongoDB — same pattern as CreditScoreRule
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.DEBT_TO_INCOME);

        double maximumDti;
        double weight;
        String configSource;

        if (activeRules.isEmpty()) {
            maximumDti   = DEFAULT_MAXIMUM_DTI;
            weight       = DEFAULT_WEIGHT;
            configSource = "DEFAULT";
            log.warn("[DTIRule] No active CreditRule found for DEBT_TO_INCOME — using defaults");
        } else {
            Map<String, Object> params = activeRules.get(0).getParameters();
            // MongoDB stores numbers as Integer or Double depending on how they were inserted.
            // We retrieve as Number and call doubleValue() to handle both transparently.
            double rawMax = params.containsKey("maximumRatioPercent")
                ? ((Number) params.get("maximumRatioPercent")).doubleValue() : 40.0;
            // Convert "40" (a percentage) to "0.40" (a ratio) if the config uses percentage form
            // We detect which form by checking if the value is > 1 (0.40 vs 40.0)
            maximumDti   = rawMax > 1.0 ? rawMax / 100.0 : rawMax;
            weight       = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            configSource = "MONGODB";
        }

        // Handle missing income — cannot compute DTI
        if (context.getDebtToIncomeRatio() == null) {
            String reason = "Monthly income is zero or not provided — DTI ratio cannot be computed. " +
                            "Cannot approve a loan without verifiable income. FAIL.";
            log.warn("[DTIRule] Missing income for applicantId={}", context.getApplicantId());
            return buildResult(false, reason, weight, "UNDEFINED", formatPct(maximumDti), weight, configSource);
        }

        double dti = context.getDebtToIncomeRatio();
        boolean passed = dti <= maximumDti;

        String reason = passed
            ? String.format("DTI ratio %.1f%% ≤ maximum allowed %.1f%%. PASS.", dti * 100, maximumDti * 100)
            : String.format("DTI ratio %.1f%% exceeds the maximum allowed %.1f%%. " +
                            "Applicant's existing debt obligations are too high relative to income. FAIL.",
                            dti * 100, maximumDti * 100);

        log.debug("[DTIRule] applicantId={} dti={} maxDti={} passed={}",
                  context.getApplicantId(), dti, maximumDti, passed);

        return buildResult(passed, reason, weight, formatPct(dti), formatPct(maximumDti),
                           passed ? 0.0 : weight, configSource);
    }

    // ── Helper: format ratio as percentage string ─────────────────────────────
    private String formatPct(double ratio) {
        return String.format("%.1f%%", ratio * 100);
    }

    // ── Helper: build RuleResult without repeating all fields ─────────────────
    private RuleResult buildResult(boolean passed, String reason, double weight,
                                   String actual, String threshold, double partial, String src) {
        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(actual)
            .thresholdValue(threshold)
            .partialRiskScore(partial)
            .configSource(src)
            .build();
    }
}
