package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;


@Component
public class LoanToIncomeRatioRule implements Rule {

    
    private final CreditRuleRepository repository;

    
    public LoanToIncomeRatioRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    
    @Override
    public String getRuleType() {
        return "LoanToIncomeRatioRule";
    }

    
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 12.0, 1.2, true));

        
        if (context.getMonthlyIncome() <= 0) {
            return new RuleResult(getRuleType(), false, "Monthly income is zero.", config.getWeight());
        }

        
        double lti = context.getRequestedLoanAmount() / context.getMonthlyIncome();
        
        boolean passed = lti <= config.getThreshold();

        
        String reason = passed ? 
            String.format("LTI ratio %.2f is within the limit of %.2f", lti, config.getThreshold()) :
            String.format("LTI ratio %.2f exceeds the limit of %.2f", lti, config.getThreshold());

        
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}