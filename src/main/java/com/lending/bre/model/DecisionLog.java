package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

// Tell MongoDB to store these audit logs in the "decision_logs" collection.
@Document(collection = "decision_logs")
public class DecisionLog {
    // The unique ID for this log entry in MongoDB.
    @Id
    private String id;
    
    // The ID of the application we evaluated.
    private String applicationId;
    
    // The final decision (approved or not).
    private boolean approved;
    
    // The calculated risk score.
    private double riskScore;
    
    // The detailed breakdown of why the decision was made.
    private List<RuleResult> ruleResults;
    
    // The exact date and time the decision was made.
    private Instant timestamp;

    // Empty constructor for MongoDB.
    public DecisionLog() {}

    // Constructor to easily create a log from a DecisionResult.
    public DecisionLog(String applicationId, DecisionResult result) {
        // Set the associated application ID.
        this.applicationId = applicationId;
        // Copy the approval status from the result.
        this.approved = result.isApproved();
        // Copy the risk score from the result.
        this.riskScore = result.getRiskScore();
        // Copy the detailed rule results.
        this.ruleResults = result.getRuleResults();
        // Record the current time as the timestamp.
        this.timestamp = Instant.now();
    }
    
    // Getters
    public String getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public boolean isApproved() { return approved; }
    public double getRiskScore() { return riskScore; }
    public List<RuleResult> getRuleResults() { return ruleResults; }
    public Instant getTimestamp() { return timestamp; }
}