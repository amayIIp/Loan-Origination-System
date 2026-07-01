package com.lending.bre.model;

import java.util.List;


public class DecisionResult {
    
    private boolean approved;
    
    private double riskScore;
    
    private List<RuleResult> ruleResults;

    
    public DecisionResult(boolean approved, double riskScore, List<RuleResult> ruleResults) {
        
        this.approved = approved;
        
        this.riskScore = riskScore;
        
        this.ruleResults = ruleResults;
    }

    
    public boolean isApproved() { return approved; }
    
    public double getRiskScore() { return riskScore; }
    
    public List<RuleResult> getRuleResults() { return ruleResults; }
}