package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;


@Component
public class EmploymentStatusRule implements Rule {

    
    private final CreditRuleRepository repository;

    
    public EmploymentStatusRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    
    @Override
    public String getRuleType() {
        return "EmploymentStatusRule";
    }

    
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 1.0, 1.5, true));

        
        String status = context.getEmploymentStatus();
        
        
        boolean passed = status != null && (status.equalsIgnoreCase("EMPLOYED") || status.equalsIgnoreCase("SELF_EMPLOYED"));

        
        String reason = passed ? 
            "Applicant employment status is acceptable: " + status :
            "Applicant is not employed or status is unknown: " + status;

        
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}