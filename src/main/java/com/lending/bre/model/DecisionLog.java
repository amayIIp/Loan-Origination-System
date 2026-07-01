package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;


@Document(collection = "decision_logs")
public class DecisionLog {
    
    @Id
    private String id;
    
    
    private String applicationId;
    
    
    private boolean approved;
    
    
    private double riskScore;
    
    
    private List<RuleResult> ruleResults;
    
    
    private Instant timestamp;

    
    public DecisionLog() {}

    
    public DecisionLog(String applicationId, DecisionResult result) {
        
        this.applicationId = applicationId;
        
        this.approved = result.isApproved();
        
        this.riskScore = result.getRiskScore();
        
        this.ruleResults = result.getRuleResults();
        
        this.timestamp = Instant.now();
    }
    
    
    public String getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public boolean isApproved() { return approved; }
    public double getRiskScore() { return riskScore; }
    public List<RuleResult> getRuleResults() { return ruleResults; }
    public Instant getTimestamp() { return timestamp; }
}