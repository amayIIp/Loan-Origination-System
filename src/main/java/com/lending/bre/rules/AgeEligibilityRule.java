package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;


@Component
public class AgeEligibilityRule implements Rule {

    
    private final CreditRuleRepository repository;

    
    public AgeEligibilityRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    
    @Override
    public String getRuleType() {
        return "AgeEligibilityRule";
    }

    
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 18.0, 1.0, true));

        
        int age = context.getAge();
        
        boolean passed = age >= config.getThreshold();

        
        String reason = passed ? 
            "Applicant age " + age + " meets the minimum requirement of " + (int)config.getThreshold() :
            "Applicant age " + age + " is below the minimum requirement of " + (int)config.getThreshold();

        
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}