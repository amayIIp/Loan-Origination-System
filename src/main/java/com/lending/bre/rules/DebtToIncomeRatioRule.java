package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;


@Component
public class DebtToIncomeRatioRule implements Rule {

    
    private final CreditRuleRepository repository;

    
    public DebtToIncomeRatioRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    
    @Override
    public String getRuleType() {
        return "DebtToIncomeRatioRule";
    }

    
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 0.40, 1.5, true));

        
        if (context.getMonthlyIncome() <= 0) {
            
            return new RuleResult(getRuleType(), false, "Monthly income is zero or negative.", config.getWeight());
        }

        
        double dti = context.getMonthlyDebt() / context.getMonthlyIncome();
        
        
        boolean passed = dti <= config.getThreshold();
        
        
        String reason = passed ? 
            String.format("DTI ratio %.2f is within the allowed limit of %.2f", dti, config.getThreshold()) :
            String.format("DTI ratio %.2f exceeds the maximum allowed limit of %.2f", dti, config.getThreshold());

        
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}