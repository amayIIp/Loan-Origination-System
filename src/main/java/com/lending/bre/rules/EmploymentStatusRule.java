package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;

// Inform Spring this is a managed component.
@Component
public class EmploymentStatusRule implements Rule {

    // Database interaction tool.
    private final CreditRuleRepository repository;

    // Ask Spring for the repository.
    public EmploymentStatusRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    // Give it a name.
    @Override
    public String getRuleType() {
        return "EmploymentStatusRule";
    }

    // Determine if the applicant's employment status is acceptable.
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        // Here, a threshold of 1.0 means employed. Default weight 1.5.
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 1.0, 1.5, true));

        // Get the status.
        String status = context.getEmploymentStatus();
        
        // We only pass if they are currently employed (EMPLOYED or SELF_EMPLOYED).
        boolean passed = status != null && (status.equalsIgnoreCase("EMPLOYED") || status.equalsIgnoreCase("SELF_EMPLOYED"));

        // Build explanation.
        String reason = passed ? 
            "Applicant employment status is acceptable: " + status :
            "Applicant is not employed or status is unknown: " + status;

        // Return the object.
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}