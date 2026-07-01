package com.los.backend.engine.rule;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.model.enums.RuleType;

/**
 * Rule — the core Strategy interface of the Business Rule Engine.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERN EXPLANATION (for interviews):
 *
 * This interface is the cornerstone of the "genuine rule engine" design.
 * It applies TWO classic Gang-of-Four patterns simultaneously:
 *
 * 1. STRATEGY PATTERN:
 *    Each concrete Rule class (CreditScoreRule, DebtToIncomeRatioRule, etc.)
 *    is an interchangeable "strategy" for one specific type of eligibility check.
 *    The RuleEngine is the "context" that selects and executes strategies at runtime.
 *    Adding a new rule = creating a new class that implements this interface.
 *    The engine code NEVER changes — it doesn't know or care which concrete rules exist.
 *
 * 2. CHAIN-OF-RESPONSIBILITY PATTERN:
 *    The RuleEngine passes the same ApplicantEvaluationContext through each Rule in sequence.
 *    Each rule independently decides to pass or fail based on its own criteria.
 *    Unlike a classic Chain-of-Responsibility (where a handler can STOP the chain),
 *    our engine evaluates ALL rules to produce a complete audit trail —
 *    "rejected because of rules X AND Y" is more informative than "rejected because of X."
 *
 * The data-driven dimension:
 *    Each Rule reads its threshold from a MongoDB CreditRule document at evaluation time.
 *    This means the engine behaviour changes when you update MongoDB — zero code change.
 *    This is what separates it from an if-else chain: the logic (code) and the
 *    configuration (data) are deliberately kept separate.
 *
 * Open/Closed Principle:
 *    The engine is OPEN for extension (add new Rule implementations) but CLOSED
 *    for modification (adding rules requires no changes to the engine itself).
 *    This is only possible because rules are polymorphic — the engine calls
 *    rule.evaluate() without knowing what rule it is.
 * ═══════════════════════════════════════════════════════════════════════════
 */
public interface Rule {

    /**
     * evaluate — the single method every Rule must implement.
     *
     * Contract:
     *   - MUST return a RuleResult (never return null — use a "data unavailable" result instead)
     *   - MUST NOT throw exceptions for missing data (handle it gracefully with a result)
     *   - MUST NOT mutate the context object (the context is shared read-only input)
     *   - SHOULD read thresholds from MongoDB via CreditRuleRepository with fallback defaults
     *
     * @param context all applicant and loan data needed for evaluation (pre-computed)
     * @return RuleResult containing pass/fail verdict, reason, weight, and actual/threshold values
     */
    RuleResult evaluate(ApplicantEvaluationContext context);

    /**
     * getRuleType — identifies which RuleType enum value this rule implements.
     *
     * The RuleEngine uses this to look up the corresponding CreditRule MongoDB document.
     * It also enables the registry to detect duplicate rule types at startup
     * (two beans implementing CREDIT_SCORE is a configuration error).
     *
     * @return the RuleType enum constant that identifies this rule's dimension
     */
    RuleType getRuleType();

    /**
     * getRuleName — the human-readable name stored in DecisionLog and displayed in the UI.
     * Default implementation derives the name from the class name.
     * Override in each concrete class for a more descriptive name.
     *
     * @return human-readable rule name string
     */
    default String getRuleName() {
        return this.getClass().getSimpleName();
    }

    /**
     * isCritical — whether a single failure of this rule should IMMEDIATELY cause REJECTED,
     * regardless of the weighted risk score.
     *
     * Most rules contribute to the weighted score (non-critical).
     * A few rules are absolute: EMPLOYMENT_STATUS = UNEMPLOYED should always reject,
     * even if all other rules pass with flying colors.
     *
     * Default: false (non-critical — participates in weighted scoring).
     * Override to return true for hard-block rules.
     *
     * @return true if a failure of this rule instantly causes rejection
     */
    default boolean isCritical() {
        return false;
    }
}
