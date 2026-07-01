package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;

// Tell Spring to create an instance of this rule and inject it where needed.
@Component
public class CreditScoreRule implements Rule {

    // Ask Spring to hand us a ready-to-use repository so we can talk to MongoDB.
    private final CreditRuleRepository repository;

    // Constructor injection: Spring provides the repository when creating this class.
    public CreditScoreRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    // Return the unique identifier for this rule, used to look up its configuration.
    @Override
    public String getRuleType() {
        return "CreditScoreRule";
    }

    // The actual business logic for evaluating the applicant's credit score.
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        // Ask the database for the current active configuration for this rule type.
        // If it doesn't exist, provide a sensible default rule (e.g., minimum score of 600, weight of 1.0).
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 600.0, 1.0, true));

        // Get the applicant's credit score from the context.
        int score = context.getCreditScore();
        // Check if their score is greater than or equal to the required threshold.
        boolean passed = score >= config.getThreshold();
        
        // Build a human-readable explanation of the outcome.
        String reason = passed ? 
            "Credit score " + score + " meets the minimum requirement of " + config.getThreshold() :
            "Credit score " + score + " is below the minimum requirement of " + config.getThreshold();

        // Return the final packaged result containing the outcome, reason, and importance weight.
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}