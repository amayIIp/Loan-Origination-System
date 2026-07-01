package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;

// Component for Spring.
@Component
public class LoanToIncomeRatioRule implements Rule {

    // Inject DB repository.
    private final CreditRuleRepository repository;

    // Constructor injection.
    public LoanToIncomeRatioRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    // Return the name.
    @Override
    public String getRuleType() {
        return "LoanToIncomeRatioRule";
    }

    // Logic to evaluate loan vs income.
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        // Default threshold is 12 (i.e. loan shouldn't exceed 12 months of income).
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 12.0, 1.2, true));

        // Prevent division by zero.
        if (context.getMonthlyIncome() <= 0) {
            return new RuleResult(getRuleType(), false, "Monthly income is zero.", config.getWeight());
        }

        // Calculate how many months of income it takes to match the loan amount.
        double lti = context.getRequestedLoanAmount() / context.getMonthlyIncome();
        // The lower the better. Must be less than or equal to the threshold.
        boolean passed = lti <= config.getThreshold();

        // Build reason string.
        String reason = passed ? 
            String.format("LTI ratio %.2f is within the limit of %.2f", lti, config.getThreshold()) :
            String.format("LTI ratio %.2f exceeds the limit of %.2f", lti, config.getThreshold());

        // Return the packaged result.
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}