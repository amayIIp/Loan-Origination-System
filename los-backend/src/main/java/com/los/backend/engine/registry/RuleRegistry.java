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

/**
 * RuleRegistry — a startup-validated registry of all Rule beans in the application.
 *
 * WHAT IS THE REGISTRY FOR?
 * ─────────────────────────────────────────────────────────────────────────
 * Spring's @Autowired List<Rule> injection already gives us all Rule beans.
 * The registry adds two important capabilities on top:
 *
 * 1. STARTUP VALIDATION:
 *    @PostConstruct runs immediately after Spring finishes wiring all beans.
 *    We detect configuration errors at startup, not at 2AM when a credit
 *    check fails in production:
 *      - Duplicate rule types: two beans both implementing CREDIT_SCORE
 *      - Missing rules: no bean implementing MINIMUM_INCOME
 *
 * 2. O(1) LOOKUP BY RULE TYPE:
 *    The RuleEngine can look up "give me the CREDIT_SCORE rule" in O(1) time
 *    using the rulesByType map, rather than iterating the full list each time.
 *
 * EXTENSIBILITY:
 *    Adding a new Rule class with @Component automatically registers it here.
 *    No changes to this registry, no changes to the engine.
 *    This is the Open/Closed Principle in action.
 */
@Component
@Slf4j
public class RuleRegistry {

    /**
     * rules — all Rule beans discovered by Spring component scanning.
     * Spring automatically finds every class annotated with @Component that
     * implements the Rule interface and injects them into this list.
     * The order is non-deterministic — the engine uses the weight field to sort.
     */
    private final List<Rule> rules;

    /**
     * rulesByType — a Map from RuleType to the Rule bean for that type.
     * Built at startup; enables O(1) lookup by rule type.
     */
    private Map<RuleType, Rule> rulesByType;

    /**
     * Constructor injection of all Rule implementations.
     * Spring resolves List<Rule> by finding every @Component bean that
     * implements the Rule interface — completely automatic discovery.
     *
     * @param rules all Rule implementations found by Spring component scanning
     */
    public RuleRegistry(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * init — runs immediately after all beans are wired (before any HTTP request is served).
     *
     * @PostConstruct (beginner explanation):
     * This annotation tells Spring: "Call this method once, right after the bean
     * is fully constructed and all @Autowired dependencies are injected."
     * It's the right place for one-time initialisation that needs injected fields.
     * Unlike a constructor, @PostConstruct can safely use "this.rules".
     */
    @PostConstruct
    public void init() {
        log.info("[RuleRegistry] Initialising BRE rule registry with {} rule implementations...", rules.size());

        // ── Build the type → rule map, checking for duplicates ────────────────
        // toMap() with a merge function: if two rules have the same RuleType,
        // the merge function is called. We log an error and keep the first one —
        // but this situation should never happen in a well-structured project.
        rulesByType = rules.stream()
            .collect(Collectors.toMap(
                Rule::getRuleType,    // key extractor: each rule's RuleType
                Function.identity(),  // value: the rule itself
                (existing, duplicate) -> {
                    // CONFIGURATION ERROR: two @Component classes claim the same RuleType
                    log.error("[RuleRegistry] DUPLICATE RULE TYPE DETECTED: {} — " +
                              "existing={}, duplicate={}. Keeping existing. FIX THIS!",
                              existing.getRuleType(),
                              existing.getClass().getSimpleName(),
                              duplicate.getClass().getSimpleName());
                    return existing; // Keep the first one; the duplicate is ignored
                }
            ));

        // ── Log all registered rules (visible at startup for operational awareness) ─
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
            // This is a CRITICAL misconfiguration: the BRE has nothing to evaluate.
            // Every application would pass trivially. Alert loudly.
            log.error("[RuleRegistry] ⚠️  NO RULES REGISTERED — the BRE will approve all applications trivially! " +
                      "Check that Rule implementations have @Component and are in the component-scan path.");
        }
    }

    /**
     * getAllRules — returns all registered rule implementations.
     * The engine calls this to get the full evaluation set.
     *
     * @return unmodifiable view of all registered Rule beans
     */
    public List<Rule> getAllRules() {
        return List.copyOf(rules); // Defensive copy — caller cannot mutate the registry list
    }

    /**
     * getRuleByType — O(1) lookup of a Rule by its RuleType.
     *
     * @param ruleType the category of rule to retrieve
     * @return the Rule bean for that type, or null if none is registered
     */
    public Rule getRuleByType(RuleType ruleType) {
        return rulesByType.get(ruleType);
    }

    /**
     * getRuleCount — how many rules are registered.
     * Used by the health check endpoint to verify engine is configured correctly.
     *
     * @return number of registered rules
     */
    public int getRuleCount() {
        return rules.size();
    }
}
