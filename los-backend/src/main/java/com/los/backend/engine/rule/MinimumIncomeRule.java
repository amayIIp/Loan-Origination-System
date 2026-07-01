package com.los.backend.engine.rule;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.model.CreditRule;
import com.los.backend.model.enums.RuleType;
import com.los.backend.repository.CreditRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
public class MinimumIncomeRule implements Rule {

    
    private static final double DEFAULT_MIN_MONTHLY_INCOME = 25000.0;
    private static final double DEFAULT_WEIGHT             = 15.0;

    private final CreditRuleRepository creditRuleRepository;

    public MinimumIncomeRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    @Override
    public RuleType getRuleType() { return RuleType.MINIMUM_INCOME; }

    @Override
    public String getRuleName() { return "Minimum Monthly Income Check"; }

    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[MinIncomeRule] Evaluating applicantId={} income={}",
                  context.getApplicantId(), context.getMonthlyIncomeInr());

        
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.MINIMUM_INCOME);

        double minIncome;
        double weight;
        String configSource;

        if (activeRules.isEmpty()) {
            minIncome    = DEFAULT_MIN_MONTHLY_INCOME;
            weight       = DEFAULT_WEIGHT;
            configSource = "DEFAULT";
            log.warn("[MinIncomeRule] No active CreditRule for MINIMUM_INCOME — using defaults");
        } else {
            Map<String, Object> params = activeRules.get(0).getParameters();
            minIncome    = params.containsKey("minimumMonthlyIncome")
                ? ((Number) params.get("minimumMonthlyIncome")).doubleValue()
                : DEFAULT_MIN_MONTHLY_INCOME;
            weight       = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            configSource = "MONGODB";
        }

        
        BigDecimal income = context.getMonthlyIncomeInr();
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.builder()
                .ruleName(getRuleName())
                .ruleType(getRuleType().name())
                .passed(false)
                .reason(String.format(
                    "Monthly income is zero or not provided. Minimum required: ₹%.0f/month. FAIL.", minIncome))
                .weight(weight)
                .actualValue("₹0")
                .thresholdValue(String.format("≥ ₹%.0f", minIncome))
                .partialRiskScore(weight)
                .configSource(configSource)
                .build();
        }

        double incomeVal = income.doubleValue();
        boolean passed   = incomeVal >= minIncome;

        String reason = passed
            ? String.format("Monthly income ₹%.0f ≥ minimum required ₹%.0f. PASS.", incomeVal, minIncome)
            : String.format("Monthly income ₹%.0f is below the minimum required ₹%.0f. FAIL.", incomeVal, minIncome);

        log.debug("[MinIncomeRule] applicantId={} income={} minIncome={} passed={}",
                  context.getApplicantId(), incomeVal, minIncome, passed);

        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(String.format("₹%.0f", incomeVal))
            .thresholdValue(String.format("≥ ₹%.0f", minIncome))
            .partialRiskScore(passed ? 0.0 : weight)
            .configSource(configSource)
            .build();
    }
}
