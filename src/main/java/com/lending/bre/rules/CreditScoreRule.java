package com.lending.bre.rules;

import com.lending.bre.engine.Rule;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.stereotype.Component;


@Component
public class CreditScoreRule implements Rule {

    
    private final CreditRuleRepository repository;

    
    public CreditScoreRule(CreditRuleRepository repository) {
        this.repository = repository;
    }

    
    @Override
    public String getRuleType() {
        return "CreditScoreRule";
    }

    
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        
        
        CreditRule config = repository.findByRuleTypeAndActiveTrue(getRuleType())
                .orElse(new CreditRule(getRuleType(), 600.0, 1.0, true));

        
        int score = context.getCreditScore();
        
        boolean passed = score >= config.getThreshold();
        
        
        String reason = passed ? 
            "Credit score " + score + " meets the minimum requirement of " + config.getThreshold() :
            "Credit score " + score + " is below the minimum requirement of " + config.getThreshold();

        
        return new RuleResult(getRuleType(), passed, reason, config.getWeight());
    }
}