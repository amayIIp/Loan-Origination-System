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


@Component
@Slf4j
public class AgeEligibilityRule implements Rule {

    private static final int DEFAULT_MINIMUM_AGE = 21;
    private static final int DEFAULT_MAXIMUM_AGE = 65;
    private static final double DEFAULT_WEIGHT    = 15.0;

    private final CreditRuleRepository creditRuleRepository;

    public AgeEligibilityRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    @Override
    public RuleType getRuleType() { return RuleType.AGE; }

    @Override
    public String getRuleName() { return "Age Eligibility Check"; }

    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[AgeRule] Evaluating applicantId={} age={}",
                  context.getApplicantId(), context.getAgeInYears());

        
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.AGE);

        int minAge, maxAge;
        double weight;
        boolean enforceAgeAtMaturity;
        String configSource;

        if (activeRules.isEmpty()) {
            minAge               = DEFAULT_MINIMUM_AGE;
            maxAge               = DEFAULT_MAXIMUM_AGE;
            weight               = DEFAULT_WEIGHT;
            enforceAgeAtMaturity = false;
            configSource         = "DEFAULT";
            log.warn("[AgeRule] No active CreditRule found for AGE — using defaults");
        } else {
            Map<String, Object> params = activeRules.get(0).getParameters();
            minAge   = params.containsKey("minimumAge")
                ? ((Number) params.get("minimumAge")).intValue() : DEFAULT_MINIMUM_AGE;
            maxAge   = params.containsKey("maximumAge")
                ? ((Number) params.get("maximumAge")).intValue() : DEFAULT_MAXIMUM_AGE;
            weight   = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            
            enforceAgeAtMaturity = params.containsKey("enforceAgeAtMaturity")
                && Boolean.TRUE.equals(params.get("enforceAgeAtMaturity"));
            configSource = "MONGODB";
        }

        int age = context.getAgeInYears();

        
        if (age == 0 && context.getDateOfBirth() == null) {
            return RuleResult.builder()
                .ruleName(getRuleName())
                .ruleType(getRuleType().name())
                .passed(false)
                .reason("Date of birth not provided — age cannot be verified. FAIL.")
                .weight(weight)
                .actualValue("UNKNOWN")
                .thresholdValue(minAge + "–" + maxAge)
                .partialRiskScore(weight)
                .configSource(configSource)
                .build();
        }

        
        boolean aboveMin = age >= minAge;
        boolean belowMax = age <= maxAge;

        
        boolean maturityCheckPassed = true;
        String maturityNote = "";
        if (enforceAgeAtMaturity && context.getTenureMonths() > 0) {
            int ageAtMaturity = age + (context.getTenureMonths() / 12);
            maturityCheckPassed = ageAtMaturity <= maxAge;
            if (!maturityCheckPassed) {
                maturityNote = String.format(
                    " Additionally, age at loan maturity (%d years) would exceed maximum retirement age %d. FAIL.",
                    ageAtMaturity, maxAge);
            }
        }

        boolean passed = aboveMin && belowMax && maturityCheckPassed;

        String reason;
        if (!aboveMin) {
            reason = String.format("Applicant age %d is below the minimum required age of %d. FAIL.", age, minAge);
        } else if (!belowMax) {
            reason = String.format("Applicant age %d exceeds the maximum allowed age of %d. FAIL.", age, maxAge);
        } else if (!maturityCheckPassed) {
            reason = String.format("Applicant age %d is within the allowed band (%d–%d).%s", age, minAge, maxAge, maturityNote);
        } else {
            reason = String.format("Applicant age %d is within the eligible band (%d–%d). PASS.", age, minAge, maxAge);
        }

        log.debug("[AgeRule] applicantId={} age={} min={} max={} passed={}",
                  context.getApplicantId(), age, minAge, maxAge, passed);

        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(String.valueOf(age))
            .thresholdValue(minAge + "–" + maxAge)
            .partialRiskScore(passed ? 0.0 : weight)
            .configSource(configSource)
            .build();
    }
}
