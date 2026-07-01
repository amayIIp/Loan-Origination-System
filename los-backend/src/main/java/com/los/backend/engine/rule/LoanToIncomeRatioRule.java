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
 * LoanToIncomeRatioRule — checks whether the requested loan amount is a
 * reasonable multiple of the applicant's annual income.
 *
 * WHAT IS LOAN-TO-INCOME (LTI)?
 * ──────────────────────────────
 * LTI = requestedLoanAmount / annualIncome
 *     = requestedLoanAmount / (monthlyIncome × 12)
 *
 * Example: ₹5,00,000 loan on ₹50,000/month income:
 *          LTI = 5,00,000 / (50,000 × 12) = 5,00,000 / 6,00,000 ≈ 0.83×
 *
 * A higher LTI means the applicant is borrowing a larger fraction of their
 * annual income. Lenders typically cap this at 3–5× annual income for
 * personal loans and up to 7–8× for home loans (backed by property collateral).
 *
 * The pre-computed loanToIncomeRatio field in ApplicantEvaluationContext
 * (computed in the context factory method) means no arithmetic is needed here.
 */
@Component
@Slf4j
public class LoanToIncomeRatioRule implements Rule {

    // 5× annual income is a standard personal loan LTI ceiling in India
    private static final double DEFAULT_MAXIMUM_LTI_MULTIPLE = 5.0;
    private static final double DEFAULT_WEIGHT               = 20.0;

    private final CreditRuleRepository creditRuleRepository;

    public LoanToIncomeRatioRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    @Override
    public RuleType getRuleType() { return RuleType.LOAN_TO_INCOME; }

    @Override
    public String getRuleName() { return "Loan-to-Income Ratio Check"; }

    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[LTIRule] Evaluating applicantId={} lti={}",
                  context.getApplicantId(), context.getLoanToIncomeRatio());

        // Load thresholds from MongoDB
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.LOAN_TO_INCOME);

        double maximumLti;
        double weight;
        String configSource;

        if (activeRules.isEmpty()) {
            maximumLti   = DEFAULT_MAXIMUM_LTI_MULTIPLE;
            weight       = DEFAULT_WEIGHT;
            configSource = "DEFAULT";
            log.warn("[LTIRule] No active CreditRule for LOAN_TO_INCOME — using defaults");
        } else {
            Map<String, Object> params = activeRules.get(0).getParameters();
            maximumLti   = params.containsKey("maximumMultiple")
                ? ((Number) params.get("maximumMultiple")).doubleValue() : DEFAULT_MAXIMUM_LTI_MULTIPLE;
            weight       = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            configSource = "MONGODB";
        }

        // Handle missing income — same defensive pattern as DTI rule
        if (context.getLoanToIncomeRatio() == null) {
            return RuleResult.builder()
                .ruleName(getRuleName())
                .ruleType(getRuleType().name())
                .passed(false)
                .reason("Income data missing — cannot compute loan-to-income ratio. FAIL.")
                .weight(weight)
                .actualValue("UNDEFINED")
                .thresholdValue(String.format("≤ %.1f× annual income", maximumLti))
                .partialRiskScore(weight)
                .configSource(configSource)
                .build();
        }

        double lti    = context.getLoanToIncomeRatio();
        boolean passed = lti <= maximumLti;

        String reason = passed
            ? String.format("Loan-to-income ratio %.2f× ≤ maximum %.1f× annual income. PASS.", lti, maximumLti)
            : String.format("Loan-to-income ratio %.2f× exceeds the maximum %.1f× annual income. " +
                            "The requested loan amount is too large relative to the applicant's income. FAIL.",
                            lti, maximumLti);

        log.debug("[LTIRule] applicantId={} lti={} maxLti={} passed={}",
                  context.getApplicantId(), lti, maximumLti, passed);

        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(String.format("%.2f×", lti))
            .thresholdValue(String.format("≤ %.1f×", maximumLti))
            .partialRiskScore(passed ? 0.0 : weight)
            .configSource(configSource)
            .build();
    }
}
