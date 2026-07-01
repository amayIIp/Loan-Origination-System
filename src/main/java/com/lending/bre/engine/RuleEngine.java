package com.lending.bre.engine;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.DecisionResult;
import com.lending.bre.model.RuleResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 * RULE ENGINE ORCHESTRATOR
 * This class acts as the "boss" (Orchestrator). It doesn't know the exact math for credit scores or DTI.
 * Instead, it takes a list of all active Rules (provided automatically by Spring via Dependency Injection)
 * and runs them one by one.
 * 
 * WHY A WEIGHTED SCORE APPROACH?
 * We use a weighted score approach because not all rules are equally important. Failing a basic age check
 * might be an instant reject, but having a slightly lower credit score could be offset by an excellent 
 * DTI ratio. By assigning weights, we can calculate an overall "Risk Score". If the score passes a 
 * predefined threshold, they are approved.
 */

// Tell Spring this is a business service that should be managed.
@Service
public class RuleEngine {

    // A list holding all the rules that implement the Rule interface.
    // Spring automatically finds all @Component classes implementing Rule and puts them in this list.
    private final List<Rule> rules;

    // We set a minimum score (e.g., 70 out of 100) required to get a loan.
    private static final double MINIMUM_PASSING_SCORE = 70.0;

    // Constructor injection: Spring gives us the list of rules.
    public RuleEngine(List<Rule> rules) {
        this.rules = rules;
    }

    // The main method that triggers the evaluation process for an applicant.
    public DecisionResult evaluate(ApplicantEvaluationContext context) {
        // Create an empty list to store the results from every individual rule.
        List<RuleResult> results = new ArrayList<>();
        
        // Variables to keep track of the math for the weighted score.
        double totalEarnedWeight = 0.0;
        double totalPossibleWeight = 0.0;
        // A flag to immediately reject if a "critical" rule fails.
        // For simplicity, if any rule with weight >= 1.5 fails, it's a hard fail.
        boolean hardFail = false;

        // Loop through every rule in our injected list.
        for (Rule rule : rules) {
            // Run the rule's specific logic using the context data.
            RuleResult result = rule.evaluate(context);
            // Add this rule's result to our list.
            results.add(result);

            // Add this rule's max possible weight to our running total.
            totalPossibleWeight += result.getWeight();
            
            // If they passed the rule, add that weight to their earned score.
            if (result.isPassed()) {
                totalEarnedWeight += result.getWeight();
            } else {
                // If they failed, and the rule is highly important (weight >= 1.5), trigger a hard fail.
                if (result.getWeight() >= 1.5) {
                    hardFail = true;
                }
            }
        }

        // Calculate the final percentage score (from 0 to 100).
        // If there were no rules (possible weight is 0), default to 0 score.
        double riskScore = totalPossibleWeight > 0 ? (totalEarnedWeight / totalPossibleWeight) * 100.0 : 0.0;
        
        // They are approved if they didn't trigger a hard fail AND their score meets the minimum threshold.
        boolean approved = !hardFail && riskScore >= MINIMUM_PASSING_SCORE;

        // Package up the final decision and return it.
        return new DecisionResult(approved, riskScore, results);
    }
}