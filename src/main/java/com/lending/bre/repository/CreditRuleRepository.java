package com.lending.bre.repository;

import com.lending.bre.model.CreditRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

// Ask Spring Data MongoDB to automatically build the standard database operations (save, find, delete) for us.
public interface CreditRuleRepository extends MongoRepository<CreditRule, String> {
    
    // We add a custom query method to find an active rule by its ruleType name. Spring writes the actual query for us.
    Optional<CreditRule> findByRuleTypeAndActiveTrue(String ruleType);
}