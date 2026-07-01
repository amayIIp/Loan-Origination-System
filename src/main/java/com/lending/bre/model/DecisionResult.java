package com.lending.bre.model;

import java.util.List;

// This object represents the final, aggregated outcome after all rules have been run.
public class DecisionResult {
    // The overall pass/fail status of the entire loan application based on our rules.
    private boolean approved;
    // A calculated score from 0 to 100 indicating the applicant's risk level.
    private double riskScore;
    // A list of the individual results from every rule that ran, so we can show the applicant exactly why they were approved or denied.
    private List<RuleResult> ruleResults;

    // Constructor to create the final decision outcome.
    public DecisionResult(boolean approved, double riskScore, List<RuleResult> ruleResults) {
        // Save the overall approval status.
        this.approved = approved;
        // Save the calculated risk score.
        this.riskScore = riskScore;
        // Save the list of individual rule results.
        this.ruleResults = ruleResults;
    }

    // Get the final approval status.
    public boolean isApproved() { return approved; }
    // Get the final risk score.
    public double getRiskScore() { return riskScore; }
    // Get the detailed breakdown of every rule's decision.
    public List<RuleResult> getRuleResults() { return ruleResults; }
}