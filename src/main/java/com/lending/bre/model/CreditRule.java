package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;




@Document(collection = "credit_rules")
public class CreditRule {
    
    
    @Id
    private String id;
    
    
    private String ruleType;
    
    
    private double threshold;
    
    
    private double weight;
    
    
    private boolean active;

    
    public CreditRule() {}

    
    public CreditRule(String ruleType, double threshold, double weight, boolean active) {
        
        this.ruleType = ruleType;
        
        this.threshold = threshold;
        
        this.weight = weight;
        
        this.active = active;
    }

    
    public String getId() { return id; }
    
    public void setId(String id) { this.id = id; }

    
    public String getRuleType() { return ruleType; }
    
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    
    public double getThreshold() { return threshold; }
    
    public void setThreshold(double threshold) { this.threshold = threshold; }

    
    public double getWeight() { return weight; }
    
    public void setWeight(double weight) { this.weight = weight; }

    
    public boolean isActive() { return active; }
    
    public void setActive(boolean active) { this.active = active; }
}