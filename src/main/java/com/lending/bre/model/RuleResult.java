package com.lending.bre.model;

// A simple plain old Java object (POJO) to hold the outcome of a single rule's evaluation.
public class RuleResult {
    // The unique name of the rule that was run (e.g., "CreditScoreRule").
    private String ruleName;
    // A true/false flag indicating if the applicant passed this specific rule.
    private boolean passed;
    // A human-readable message explaining why they passed or failed, for transparency.
    private String reason;
    // How much this rule's result matters in the final score calculation (e.g., a weight of 2.0 is twice as important as 1.0).
    private double weight;

    // A constructor to easily create a ready-to-use RuleResult with all fields populated.
    public RuleResult(String ruleName, boolean passed, String reason, double weight) {
        // Assign the passed-in rule name to our internal field.
        this.ruleName = ruleName;
        // Assign the pass/fail status to our internal field.
        this.passed = passed;
        // Assign the explanation to our internal field.
        this.reason = reason;
        // Assign the importance weight to our internal field.
        this.weight = weight;
    }

    // Getters for other classes to read these values safely without modifying them.
    
    // Return the rule's name.
    public String getRuleName() { return ruleName; }
    // Return whether the rule passed.
    public boolean isPassed() { return passed; }
    // Return the human-readable explanation.
    public String getReason() { return reason; }
    // Return the importance weight.
    public double getWeight() { return weight; }
}