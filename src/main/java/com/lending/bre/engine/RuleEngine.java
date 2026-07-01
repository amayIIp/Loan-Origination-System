package com.lending.bre.engine;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.DecisionResult;
import com.lending.bre.model.RuleResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;




@Service
public class RuleEngine {

    
    
    private final List<Rule> rules;

    
    private static final double MINIMUM_PASSING_SCORE = 70.0;

    
    public RuleEngine(List<Rule> rules) {
        this.rules = rules;
    }

    
    public DecisionResult evaluate(ApplicantEvaluationContext context) {
        
        List<RuleResult> results = new ArrayList<>();
        
        
        double totalEarnedWeight = 0.0;
        double totalPossibleWeight = 0.0;
        
        
        boolean hardFail = false;

        
        for (Rule rule : rules) {
            
            RuleResult result = rule.evaluate(context);
            
            results.add(result);

            
            totalPossibleWeight += result.getWeight();
            
            
            if (result.isPassed()) {
                totalEarnedWeight += result.getWeight();
            } else {
                
                if (result.getWeight() >= 1.5) {
                    hardFail = true;
                }
            }
        }

        
        
        double riskScore = totalPossibleWeight > 0 ? (totalEarnedWeight / totalPossibleWeight) * 100.0 : 0.0;
        
        
        boolean approved = !hardFail && riskScore >= MINIMUM_PASSING_SCORE;

        
        return new DecisionResult(approved, riskScore, results);
    }
}