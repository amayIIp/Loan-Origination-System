package com.lending.bre.controller;

import com.lending.bre.model.CreditRule;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// This controller allows administrators to read and update rule thresholds on the fly.
@RestController
@RequestMapping("/api/credit-rules")
public class CreditRuleController {

    // Database interaction tool for rules.
    private final CreditRuleRepository repository;

    // Constructor injection.
    public CreditRuleController(CreditRuleRepository repository) {
        this.repository = repository;
    }

    // Map GET requests to fetch all rules from the database.
    @GetMapping
    public ResponseEntity<List<CreditRule>> getAllRules() {
        // Ask the repository to give us every rule it has.
        List<CreditRule> rules = repository.findAll();
        // Return them to the client.
        return ResponseEntity.ok(rules);
    }

    // Map POST requests to create or update a rule.
    // Notice how updating this MongoDB document instantly changes engine behavior without recompiling Java code.
    @PostMapping
    public ResponseEntity<CreditRule> saveOrUpdateRule(@RequestBody CreditRule rule) {
        // Save the rule to the database (inserts if ID is new, updates if ID exists).
        CreditRule savedRule = repository.save(rule);
        // Return the saved rule.
        return ResponseEntity.ok(savedRule);
    }
}