package com.lending.bre.controller;

import com.lending.bre.engine.RuleEngine;
import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.DecisionLog;
import com.lending.bre.model.DecisionResult;
import com.lending.bre.model.LoanApplication;
import com.lending.bre.repository.DecisionLogRepository;
import com.lending.bre.repository.LoanApplicationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * REST CONTROLLER EXPLANATION:
 * A REST Controller is the "front door" of our backend application. It listens for HTTP requests (like GET, POST) 
 * from the frontend (Angular) or Postman, executes the requested business logic, and sends back a response in JSON format.
 */

// Tell Spring this is a REST controller so it maps HTTP requests to methods here.
@RestController
// Add a base URL prefix to all endpoints in this controller.
@RequestMapping("/api/applications")
public class ApplicationDecisionController {

    // The brain that evaluates our rules.
    private final RuleEngine ruleEngine;
    // Database tool for finding and saving loan applications.
    private final LoanApplicationRepository applicationRepository;
    // Database tool for keeping a permanent record (audit log) of our decisions.
    private final DecisionLogRepository decisionLogRepository;

    // Ask Spring to provide these dependencies via constructor injection.
    public ApplicationDecisionController(RuleEngine ruleEngine, LoanApplicationRepository applicationRepository, DecisionLogRepository decisionLogRepository) {
        this.ruleEngine = ruleEngine;
        this.applicationRepository = applicationRepository;
        this.decisionLogRepository = decisionLogRepository;
    }

    // Map POST requests targeting /api/applications/{id}/evaluate to this method.
    // This allows manual triggering of the engine for a specific application.
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<DecisionResult> evaluateApplication(@PathVariable String id) {
        // Look up the application from the database using the provided ID.
        LoanApplication app = applicationRepository.findById(id)
                // If it's not found, throw an error (which Spring turns into a 404 response).
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Build the Context object containing all the data the RuleEngine needs.
        ApplicantEvaluationContext context = new ApplicantEvaluationContext(
                app.getId(),
                app.getCreditScore(),
                app.getAge(),
                app.getMonthlyIncome(),
                app.getMonthlyDebt(),
                app.getRequestedAmount(),
                app.getEmploymentStatus()
        );

        // Ask the RuleEngine to evaluate the context and give us the final result.
        DecisionResult result = ruleEngine.evaluate(context);

        // Persist the outcome to the DecisionLog for auditing purposes.
        DecisionLog log = new DecisionLog(app.getId(), result);
        decisionLogRepository.save(log);

        // Trigger state-transition by updating the LoanApplication's status in MongoDB.
        app.setStatus(result.isApproved() ? "APPROVED" : "REJECTED");
        // Save the updated application back to the database.
        applicationRepository.save(app);

        // Send the decision result back to the caller wrapped in a standard HTTP 200 (OK) response.
        return ResponseEntity.ok(result);
    }
}