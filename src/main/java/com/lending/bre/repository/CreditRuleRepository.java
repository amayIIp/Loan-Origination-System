package com.lending.bre.repository;

import com.lending.bre.model.CreditRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;


public interface CreditRuleRepository extends MongoRepository<CreditRule, String> {
    
    
    Optional<CreditRule> findByRuleTypeAndActiveTrue(String ruleType);
}