package com.los.backend.engine.registry;

import com.los.backend.engine.rule.Rule;
import com.los.backend.model.enums.RuleType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
@Slf4j
public class RuleRegistry {

    
    private final List<Rule> rules;

    
    private Map<RuleType, Rule> rulesByType;

    
    public RuleRegistry(List<Rule> rules) {
        this.rules = rules;
    }

    
    @PostConstruct
    public void init() {
        log.info("[RuleRegistry] Initialising BRE rule registry with {} rule implementations...", rules.size());

        
        
        
        
        rulesByType = rules.stream()
            .collect(Collectors.toMap(
                Rule::getRuleType,    
                Function.identity(),  
                (existing, duplicate) -> {
                    
                    log.error("[RuleRegistry] DUPLICATE RULE TYPE DETECTED: {} — " +
                              "existing={}, duplicate={}. Keeping existing. FIX THIS!",
                              existing.getRuleType(),
                              existing.getClass().getSimpleName(),
                              duplicate.getClass().getSimpleName());
                    return existing; 
                }
            ));

        
        log.info("[RuleRegistry] === Registered BRE Rules ===");
        rules.forEach(rule ->
            log.info("[RuleRegistry]   type={} | name='{}' | class={} | critical={}",
                     rule.getRuleType(),
                     rule.getRuleName(),
                     rule.getClass().getSimpleName(),
                     rule.isCritical())
        );
        log.info("[RuleRegistry] === Total: {} rules registered ===", rules.size());

        if (rules.isEmpty()) {
            
            
            log.error("[RuleRegistry] ⚠️  NO RULES REGISTERED — the BRE will approve all applications trivially! " +
                      "Check that Rule implementations have @Component and are in the component-scan path.");
        }
    }

    
    public List<Rule> getAllRules() {
        return List.copyOf(rules); 
    }

    
    public Rule getRuleByType(RuleType ruleType) {
        return rulesByType.get(ruleType);
    }

    
    public int getRuleCount() {
        return rules.size();
    }
}
