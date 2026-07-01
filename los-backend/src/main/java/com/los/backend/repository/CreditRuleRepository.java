package com.los.backend.repository;

import com.los.backend.model.CreditRule;
import com.los.backend.model.enums.RuleType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CreditRuleRepository — data access for the "credit_rules" MongoDB collection.
 *
 * Usage pattern: The BRE (RuleEngineService) calls findByIsActiveTrueOrderByWeightDesc()
 * at the start of each evaluation to load all currently active rules.
 * Admin controllers call other methods to manage the rule catalogue.
 *
 * Performance note:
 * The "credit_rules" collection will typically be small (10–50 rules), so most
 * queries here are inherently fast even without indexes. We still define indexes
 * for is_active and rule_type in the mongosh script for correctness and future-proofing.
 */
@Repository
public interface CreditRuleRepository extends MongoRepository<CreditRule, String> {

    // ── Method 1: findByIsActiveTrueOrderByWeightDesc ─────────────────────────
    /**
     * Load ALL currently active credit rules, sorted by weight (highest first).
     *
     * This is the PRIMARY query called by the BRE at the start of every credit evaluation.
     * It loads the full "rule set" that the engine will apply against the applicant.
     *
     * Why sort by weight descending?
     * The BRE processes rules in weight order so that if an early high-weight rule fails
     * catastrophically (e.g., CREDIT_SCORE = 300), the BRE can short-circuit evaluation
     * and skip less important rules — saving time and database reads for clear rejections.
     * (Short-circuit optimisation is implemented in RuleEngineService, not here.)
     *
     * Generated query:
     *   db.credit_rules.find({ "is_active": true }).sort({ "weight": -1 })
     *
     * @return list of all active CreditRule documents, highest weight first
     */
    List<CreditRule> findByIsActiveTrueOrderByWeightDesc();

    // ── Method 2: findByRuleTypeAndIsActiveTrue ───────────────────────────────
    /**
     * Find all active rules of a specific type.
     *
     * Use case: Admin UI filter — "Show me only active CREDIT_SCORE rules."
     * Also used by the BRE when we want to run ONLY a subset of rules
     * (e.g., a "quick pre-check" that only runs lightweight rules before
     * triggering the full evaluation).
     *
     * Generated query:
     *   db.credit_rules.find({ "rule_type": <ruleType>, "is_active": true })
     *
     * @param ruleType the category of rule to retrieve (e.g., RuleType.CREDIT_SCORE)
     * @return list of active CreditRule documents of that type
     */
    List<CreditRule> findByRuleTypeAndIsActiveTrue(RuleType ruleType);

    // ── Method 3: findByIdAndVersion ─────────────────────────────────────────
    /**
     * Find a specific version of a credit rule by its ID and version number.
     *
     * Use case: DecisionLog audit replay — "Which exact rule parameters were used
     * in the decision made on 2024-06-01?" We stored the rule ID and version in
     * the DecisionLog; this query fetches that exact historical version.
     *
     * Why Optional? — If the rule ID exists but the requested version doesn't
     * (e.g., someone queries v3 when only v1 and v2 were ever created), we return
     * Optional.empty() rather than throwing a NullPointerException.
     *
     * @param id      the custom rule ID (e.g., "rule_credit_score_v1")
     * @param version the integer version number to retrieve
     * @return Optional containing the matching CreditRule, or empty if not found
     */
    Optional<CreditRule> findByIdAndVersion(String id, Integer version);

    // ── Method 4: findByIsActiveFalse ────────────────────────────────────────
    /**
     * Find all disabled (inactive) credit rules.
     *
     * Use case: Admin audit — "What rules have been deactivated and why?"
     * Inactive rules are preserved in the database for historical traceability.
     * This query surfaces them for the rule management admin screen.
     *
     * @return list of all CreditRule documents with is_active = false
     */
    List<CreditRule> findByIsActiveFalse();

    // ── Method 5: Custom @Query — Rules applicable to a product type ──────────
    /**
     * Find all active rules that apply to a given loan product type (or have no
     * product restriction — meaning they apply to all products).
     *
     * MongoDB query explanation:
     *   $or: [
     *     { applicable_product_types: { $in: [<productType>] } }  ← explicitly listed
     *     { applicable_product_types: { $size: 0 } }              ← empty = applies to all
     *     { applicable_product_types: null }                      ← null = applies to all
     *   ]
     * This ensures rules with no product type restriction are always included.
     *
     * Use case: BRE evaluation when different loan products have different rule sets
     * (e.g., HOME_LOAN requires different minimum income than PERSONAL_LOAN).
     *
     * @param productType the loan product type string (e.g., "PERSONAL_LOAN", "HOME_LOAN")
     * @return list of applicable active rules for this product
     */
    @Query("{ 'is_active': true, $or: [ " +
           "{ 'applicable_product_types': { $in: [?0] } }, " +
           "{ 'applicable_product_types': { $size: 0 } }, " +
           "{ 'applicable_product_types': null } ] }")
    List<CreditRule> findActiveRulesForProductType(String productType);

    // ── Method 6: countByIsActiveTrue ────────────────────────────────────────
    /**
     * Count the number of currently active credit rules.
     *
     * Use case: Health check endpoint and admin dashboard — "How many rules is the
     * BRE currently enforcing?" A count of 0 would indicate a misconfiguration
     * (all loans would trivially pass if no rules are loaded).
     *
     * @return count of active CreditRule documents
     */
    long countByIsActiveTrue();
}
