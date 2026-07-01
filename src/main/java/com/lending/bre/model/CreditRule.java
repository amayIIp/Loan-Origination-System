package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/*
 * MONGODB DOCUMENT EXPLANATION:
 * This class maps to a "document" in our MongoDB NoSQL database. A NoSQL database stores data as flexible,
 * JSON-like records rather than rigid tables with rows and columns. This flexibility lets us easily store 
 * different types of configuration for different rules without constantly changing the database schema.
 */

// Tell Spring Data MongoDB to store objects of this class in a database collection named "credit_rules".
@Document(collection = "credit_rules")
public class CreditRule {
    
    // Tell MongoDB to use this field as the unique identifier (the Primary Key) for each document.
    @Id
    private String id;
    
    // The type of rule this config belongs to (e.g., "CreditScoreRule"). We'll look this up at runtime.
    private String ruleType;
    
    // The threshold value we are checking against (e.g., minimum credit score of 650).
    private double threshold;
    
    // The importance weight of this rule in the final calculation.
    private double weight;
    
    // A flag to quickly disable a rule without deleting it from the database.
    private boolean active;

    // Required empty constructor for Spring Data to use when reading from the database.
    public CreditRule() {}

    // Constructor to easily create a new rule configuration.
    public CreditRule(String ruleType, double threshold, double weight, boolean active) {
        // Set the rule type identifier.
        this.ruleType = ruleType;
        // Set the threshold value.
        this.threshold = threshold;
        // Set the rule's weight.
        this.weight = weight;
        // Set whether the rule is currently active.
        this.active = active;
    }

    // Get the unique database ID.
    public String getId() { return id; }
    // Set the unique database ID.
    public void setId(String id) { this.id = id; }

    // Get the rule type.
    public String getRuleType() { return ruleType; }
    // Set the rule type.
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    // Get the threshold value.
    public double getThreshold() { return threshold; }
    // Set the threshold value.
    public void setThreshold(double threshold) { this.threshold = threshold; }

    // Get the weight of the rule.
    public double getWeight() { return weight; }
    // Set the weight of the rule.
    public void setWeight(double weight) { this.weight = weight; }

    // Check if the rule is active.
    public boolean isActive() { return active; }
    // Update the active status of the rule.
    public void setActive(boolean active) { this.active = active; }
}