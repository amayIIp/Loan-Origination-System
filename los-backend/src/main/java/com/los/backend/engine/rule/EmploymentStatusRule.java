package com.los.backend.engine.rule;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.model.CreditRule;
import com.los.backend.model.enums.EmploymentType;
import com.los.backend.model.enums.RuleType;
import com.los.backend.repository.CreditRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@Slf4j
public class EmploymentStatusRule implements Rule {

    
    private static final List<String> DEFAULT_ALLOWED_TYPES = Arrays.asList(
        "SALARIED", "SELF_EMPLOYED", "BUSINESS_OWNER", "RETIRED"
    );
    private static final double DEFAULT_WEIGHT = 10.0;

    private final CreditRuleRepository creditRuleRepository;

    public EmploymentStatusRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    @Override
    public RuleType getRuleType() { return RuleType.EMPLOYMENT_STATUS; }

    @Override
    public String getRuleName() { return "Employment Status Eligibility Check"; }

    
    @Override
    public boolean isCritical() { return true; }

    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[EmploymentRule] Evaluating applicantId={} employmentType={}",
                  context.getApplicantId(), context.getEmploymentType());

        
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.EMPLOYMENT_STATUS);

        List<String> allowedTypes;
        double weight;
        String configSource;

        if (activeRules.isEmpty()) {
            allowedTypes = DEFAULT_ALLOWED_TYPES;
            weight       = DEFAULT_WEIGHT;
            configSource = "DEFAULT";
            log.warn("[EmploymentRule] No active CreditRule for EMPLOYMENT_STATUS — using defaults");
        } else {
            Map<String, Object> params = activeRules.get(0).getParameters();
            
            @SuppressWarnings("unchecked")
            List<String> rawTypes = params.containsKey("allowedTypes")
                ? (List<String>) params.get("allowedTypes")
                : DEFAULT_ALLOWED_TYPES;
            
            allowedTypes = rawTypes.stream().map(String::toUpperCase).collect(Collectors.toList());
            weight       = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            configSource = "MONGODB";
        }

        
        if (context.getEmploymentType() == null) {
            return RuleResult.builder()
                .ruleName(getRuleName())
                .ruleType(getRuleType().name())
                .passed(false)
                .reason("Employment type not provided — cannot verify eligibility. CRITICAL FAIL.")
                .weight(weight)
                .actualValue("NOT_PROVIDED")
                .thresholdValue("One of: " + String.join(", ", allowedTypes))
                .partialRiskScore(weight)
                .configSource(configSource)
                .build();
        }

        EmploymentType empType = context.getEmploymentType();
        boolean passed = allowedTypes.contains(empType.name());

        String reason = passed
            ? String.format("Employment type '%s' is in the list of accepted types %s. PASS.",
                            empType, allowedTypes)
            : String.format("Employment type '%s' is NOT accepted. Only these types are eligible: %s. CRITICAL FAIL.",
                            empType, allowedTypes);

        log.debug("[EmploymentRule] applicantId={} empType={} passed={}",
                  context.getApplicantId(), empType, passed);

        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(empType.name())
            .thresholdValue("One of: " + String.join(", ", allowedTypes))
            .partialRiskScore(passed ? 0.0 : weight)
            .configSource(configSource)
            .build();
    }
}
