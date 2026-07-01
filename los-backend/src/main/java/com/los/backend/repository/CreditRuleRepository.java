package com.los.backend.repository;

import com.los.backend.model.CreditRule;
import com.los.backend.model.enums.RuleType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CreditRuleRepository extends MongoRepository<CreditRule, String> {

    
    
    List<CreditRule> findByIsActiveTrueOrderByWeightDesc();

    
    
    List<CreditRule> findByRuleTypeAndIsActiveTrue(RuleType ruleType);

    
    
    Optional<CreditRule> findByIdAndVersion(String id, Integer version);

    
    
    List<CreditRule> findByIsActiveFalse();

    
    
    @Query("{ 'is_active': true, $or: [ " +
           "{ 'applicable_product_types': { $in: [?0] } }, " +
           "{ 'applicable_product_types': { $size: 0 } }, " +
           "{ 'applicable_product_types': null } ] }")
    List<CreditRule> findActiveRulesForProductType(String productType);

    
    
    long countByIsActiveTrue();
}
