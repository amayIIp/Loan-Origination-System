package com.lending.bre.engine;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.RuleResult;

/*
 * WHY THIS IS A REAL RULE ENGINE (AND NOT JUST A BUNCH OF IF/ELSE STATEMENTS):
 * 
 * 1. Abstraction (The `Rule` Interface): 
 *    By making every rule implement this single interface, the main engine doesn't need to know 
 *    what a "CreditScoreRule" or an "AgeRule" is. It just knows it has a list of "Rules" to run.
 *    If we add a new rule tomorrow, the engine's code doesn't change at all (Open-Closed Principle).
 * 
 * 2. Data-Driven Configuration:
 *    Hardcoded if/else statements require a developer to recompile and redeploy code to change a threshold 
 *    (e.g., from 650 to 680). A true rule engine fetches its logic constraints from a database at runtime, 
 *    allowing business analysts to change behavior instantly.
 * 
 * 3. Separation of Concerns (Strategy/Chain of Responsibility Hybrid):
 *    An if/else block mixes the orchestration (the order of checking) with the business logic (the math). 
 *    Here, the rule classes handle the math, and the engine handles the orchestration and aggregation.
 */

// This interface defines the contract that every specific business rule must follow.
public interface Rule {
    
    // The main method that takes in the applicant's data and returns whether they passed this specific rule.
    RuleResult evaluate(ApplicantEvaluationContext context);
    
    // A helper method to identify the rule's type, used to fetch its specific configuration from the database.
    String getRuleType();
}