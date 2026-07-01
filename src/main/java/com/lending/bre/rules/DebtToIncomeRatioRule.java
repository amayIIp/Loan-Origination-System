package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;

// Tell Spring to manage this class as a component.
@Component
public class DebtToIncomeRatioRule implements Rule {

    // Inject the database repository.
    private final CreditRuleRepository repository;

    // Constructor injection.
    public DebtToIncomeRatioRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    // Unique name for this rule.
    @Override
    public String getRuleType() {
        return "DebtToIncomeRatioRule";
    }

    // Evaluate the applicant's Debt-to-Income (DTI) ratio.
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        // Fetch config from MongoDB, default to a max DTI of 0.40 (40%) and weight 1.5 if not found.
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 0.40, 1.5, true));

        // Prevent division by zero if the applicant has no income.
        if (context.getMonthlyIncome() <= 0) {
            // Fail immediately if there's no income, since they can't afford debt.
            return new RuleResult(getRuleType(), false, "Monthly income is zero or negative.", config.getWeight());
        }

        // Calculate the ratio: how much of their income goes to paying off debt.
        double dti = context.getMonthlyDebt() / context.getMonthlyIncome();
        
        // For DTI, a LOWER number is better. Check if it's below the maximum allowed threshold.
        boolean passed = dti <= config.getThreshold();
        
        // Format the explanation.
        String reason = passed ? 
            String.format("DTI ratio %.2f is within the allowed limit of %.2f", dti, config.getThreshold()) :
            String.format("DTI ratio %.2f exceeds the maximum allowed limit of %.2f", dti, config.getThreshold());

        // Return the packaged result.
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}