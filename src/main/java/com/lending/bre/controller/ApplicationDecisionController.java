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




@RestController

@RequestMapping("/api/applications")
public class ApplicationDecisionController {

    
    private final RuleEngine ruleEngine;
    
    private final LoanApplicationRepository applicationRepository;
    
    private final DecisionLogRepository decisionLogRepository;

    
    public ApplicationDecisionController(RuleEngine ruleEngine, LoanApplicationRepository applicationRepository, DecisionLogRepository decisionLogRepository) {
        this.ruleEngine = ruleEngine;
        this.applicationRepository = applicationRepository;
        this.decisionLogRepository = decisionLogRepository;
    }

    
    
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<DecisionResult> evaluateApplication(@PathVariable String id) {
        
        LoanApplication app = applicationRepository.findById(id)
                
                .orElseThrow(() -> new RuntimeException("Application not found"));

        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext(
                app.getId(),
                app.getCreditScore(),
                app.getAge(),
                app.getMonthlyIncome(),
                app.getMonthlyDebt(),
                app.getRequestedAmount(),
                app.getEmploymentStatus()
        );

        
        DecisionResult result = ruleEngine.evaluate(context);

        
        DecisionLog log = new DecisionLog(app.getId(), result);
        decisionLogRepository.save(log);

        
        app.setStatus(result.isApproved() ? "APPROVED" : "REJECTED");
        
        applicationRepository.save(app);

        
        return ResponseEntity.ok(result);
    }
}