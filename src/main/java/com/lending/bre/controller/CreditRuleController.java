package com.lending.bre.controller;

import com.lending.bre.model.CreditRule;
import com.lending.bre.repository.CreditRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/credit-rules")
public class CreditRuleController {

    
    private final CreditRuleRepository repository;

    
    public CreditRuleController(CreditRuleRepository repository) {
        this.repository = repository;
    }

    
    @GetMapping
    public ResponseEntity<List<CreditRule>> getAllRules() {
        
        List<CreditRule> rules = repository.findAll();
        
        return ResponseEntity.ok(rules);
    }

    
    
    @PostMapping
    public ResponseEntity<CreditRule> saveOrUpdateRule(@RequestBody CreditRule rule) {
        
        CreditRule savedRule = repository.save(rule);
        
        return ResponseEntity.ok(savedRule);
    }
}