package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ENTITY: CreditRule
 * MongoDB Collection: "credit_rules"
 *
 * WHY DATA-DRIVEN RULES (stored in MongoDB) vs HARDCODED logic in Java?
 * ───────────────────────────────────────────────────────────────────────
 * Hardcoded approach (DON'T DO THIS):
 *   if (creditScore < 650) { reject("Low credit score"); }
 *   if (dti > 0.40) { reject("High DTI"); }
 *
 * Problems with hardcoding:
 * 1. Every rule change requires a code change → recompile → test → redeploy.
 *    In a lending business, risk thresholds change frequently (RBI circulars,
 *    market conditions, product updates). A deployment for every threshold
 *    tweak is expensive and slow.
 * 2. Cannot A/B test different rule sets without code branches.
 * 3. Audit trail is in Git history, not in the system itself.
 * 4. Non-technical product managers cannot manage rules without engineering.
 *
 * Data-driven approach (THIS IMPLEMENTATION):
 *   Rules are JSON documents in MongoDB. The BRE loads active rules at runtime,
 *   evaluates them against the applicant, and persists the decision.
 *   Changing a threshold = update ONE MongoDB document. No code change. No deploy.
 *
 * Benefits:
 * ✅ Business users can update thresholds via an admin UI (Phase 5) without devs.
 * ✅ Full version history: every rule has a version field; old versions are preserved.
 * ✅ Rule activation/deactivation is instant (flip isActive = false).
 * ✅ A/B testing: run two rule sets simultaneously with different isActive subsets.
 * ✅ Complete audit trail: DecisionLog records WHICH rule version was used.
 *
 * PARAMETERS DESIGN CHOICE — Map<String, Object> vs typed structure:
 * ────────────────────────────────────────────────────────────────────
 * We use Map<String, Object> for the parameters field.
 * Why not a typed DTO per rule type?
 * - Each RuleType has different parameters (minimumScore vs maximumRatioPercent vs
 *   allowedTypes list). A single typed POJO would need all fields nullable,
 *   which is semantically confusing and hard to validate.
 * - Map<String, Object> + the RuleType enum lets the BRE evaluator KNOW which
 *   keys to expect for each type and extract them with explicit casts.
 *   This is the accepted trade-off: schema flexibility in the data layer,
 *   strict validation in the application layer (BRE evaluator code).
 * - Alternative: JSON Schema validation on write (Phase 5). For now, the BRE
 *   evaluator throws a clear RuleConfigurationException if a key is missing.
 *
 * Example parameter maps by RuleType:
 *   CREDIT_SCORE:     { "minimumScore": 650 }
 *   DEBT_TO_INCOME:   { "maximumRatioPercent": 40 }
 *   AGE:              { "minimumAge": 21, "maximumAge": 65 }
 *   LOAN_TO_INCOME:   { "maximumMultiple": 5 }
 *   EMPLOYMENT_STATUS:{ "allowedTypes": ["SALARIED", "SELF_EMPLOYED"] }
 *   MINIMUM_INCOME:   { "minimumMonthlyIncome": 25000 }
 * ─────────────────────────────────────────────────────────────────────────────
 */

import com.los.backend.model.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

/**
 * CreditRule — a single, configurable eligibility rule stored in MongoDB.
 * The BRE loads all isActive=true rules at evaluation time and applies each one.
 *
 * Document-level example (as stored in MongoDB):
 * {
 *   "_id": "rule_credit_score_v1",
 *   "rule_name": "Minimum CIBIL Credit Score",
 *   "rule_type": "CREDIT_SCORE",
 *   "parameters": { "minimumScore": 650 },
 *   "is_active": true,
 *   "version": 1,
 *   "weight": 30,
 *   "created_at": "2024-01-01T00:00:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_rules")
public class CreditRule {

    /**
     * id — custom rule ID (not auto-generated).
     * We use a human-readable string ID (e.g., "rule_credit_score_v1") instead of
     * a random ObjectId because:
     * 1. DecisionLog can reference the rule by a meaningful ID in its audit trail.
     * 2. Admin UIs and seed scripts can use predictable IDs.
     * 3. Version changes create a new document with a new ID (e.g., "rule_credit_score_v2"),
     *    preserving the old version for historical DecisionLog lookups.
     */
    @Id
    @NotBlank(message = "Rule ID is required")
    private String id;

    /**
     * ruleName — human-readable name displayed in the loan officer UI and DecisionLog.
     * Example: "Minimum CIBIL Credit Score", "Maximum Debt-to-Income Ratio"
     */
    @NotBlank(message = "Rule name is required")
    @Size(max = 200)
    @Field("rule_name")
    private String ruleName;

    /**
     * ruleType — which evaluation logic the BRE should apply for this rule.
     * The BRE uses this enum value to route the rule to the correct evaluator.
     * @Indexed — commonly queried: "get all active CREDIT_SCORE rules", etc.
     */
    @NotNull(message = "Rule type is required")
    @Indexed
    @Field("rule_type")
    private RuleType ruleType;

    /**
     * description — plain-language explanation of what this rule checks and why.
     * Shown to loan officers in the admin UI so they understand the rule's intent
     * before modifying its parameters. Good documentation inside the data itself.
     */
    @Size(max = 1000)
    @Field("description")
    private String description;

    /**
     * parameters — a flexible key-value map of the rule's configuration thresholds.
     * The BRE evaluator for each RuleType knows which keys to extract.
     * Example for CREDIT_SCORE: { "minimumScore": 650 }
     *
     * Why Map<String, Object>? See the class-level comment block above for the full
     * rationale. Short answer: each RuleType has structurally different parameters;
     * a typed POJO per type would create a large, messy class hierarchy for minimal gain.
     *
     * @NotNull — every rule MUST have parameters; a rule with no parameters cannot
     *            do anything and would silently pass all applicants.
     */
    @NotNull(message = "Rule parameters are required")
    @Field("parameters")
    private Map<String, Object> parameters;

    /**
     * weight — an integer (1–100) representing this rule's relative importance
     * in the composite riskScore calculation.
     * Higher weight = this rule's outcome affects the final score more strongly.
     * Example: CREDIT_SCORE might have weight=40, MINIMUM_INCOME weight=20.
     * All active rule weights should sum to 100 for a clean percentage score —
     * enforced by the BRE service, not here (validation is complex for a sum constraint).
     */
    @Min(value = 1, message = "Rule weight must be at least 1")
    @Max(value = 100, message = "Rule weight cannot exceed 100")
    @NotNull
    @Field("weight")
    @Builder.Default
    private Integer weight = 10;

    /**
     * isActive — controls whether the BRE evaluates this rule.
     * false = rule is disabled (ignored during evaluation).
     * This is the "feature flag" for rules — no code deployment needed to disable a rule.
     *
     * @Indexed — the BRE always queries: "WHERE is_active = true", so an index here
     *            makes rule loading significantly faster as the rule set grows.
     */
    @Indexed
    @Field("is_active")
    @Builder.Default
    private boolean isActive = true;

    /**
     * version — integer version number of this rule document.
     * Starts at 1 and increments with each significant parameter change.
     * Old versions are NEVER deleted — they are set isActive=false and a new
     * document with a new ID and incremented version is created instead.
     * This preserves the exact rule version referenced in historical DecisionLogs.
     */
    @Min(value = 1)
    @NotNull
    @Field("version")
    @Builder.Default
    private Integer version = 1;

    /**
     * applicableProductTypes — list of loan product codes this rule applies to.
     * Example: ["PERSONAL_LOAN", "HOME_LOAN"] — this rule only runs for those products.
     * If null/empty, the rule applies to ALL loan products.
     * Enables product-specific rule sets without separate collections per product.
     */
    @Field("applicable_product_types")
    private java.util.List<String> applicableProductTypes;

    // ── Audit ─────────────────────────────────────────────────────────────────

    /** Auto-set by Spring Data on first save — when was this rule created? */
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    /** Auto-set by Spring Data on every save — when was this rule last modified? */
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    /** Who created or last modified this rule — userId of an admin user. */
    @Field("modified_by")
    private String modifiedBy;
}
