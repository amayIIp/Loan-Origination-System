package com.lending.bre.model;


public class RuleResult {
    
    private String ruleName;
    
    private boolean passed;
    
    private String reason;
    
    private double weight;

    
    public RuleResult(String ruleName, boolean passed, String reason, double weight) {
        
        this.ruleName = ruleName;
        
        this.passed = passed;
        
        this.reason = reason;
        
        this.weight = weight;
    }

    
    
    
    public String getRuleName() { return ruleName; }
    
    public boolean isPassed() { return passed; }
    
    public String getReason() { return reason; }
    
    public double getWeight() { return weight; }
}