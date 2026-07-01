package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;

// Tell Spring to manage this class.
@Component
public class AgeEligibilityRule implements Rule {

    // Repository to get rule config from MongoDB.
    private final CreditRuleRepository repository;

    // Inject the repository.
    public AgeEligibilityRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    // Name of the rule.
    @Override
    public String getRuleType() {
        return "AgeEligibilityRule";
    }

    // Evaluate if the applicant is old enough.
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        // Fetch config, defaulting to minimum age of 18, weight 1.0.
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 18.0, 1.0, true));

        // Get the applicant's age.
        int age = context.getAge();
        // Check if they are old enough.
        boolean passed = age >= config.getThreshold();

        // Build explanation string.
        String reason = passed ? 
            "Applicant age " + age + " meets the minimum requirement of " + (int)config.getThreshold() :
            "Applicant age " + age + " is below the minimum requirement of " + (int)config.getThreshold();

        // Return the result.
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}