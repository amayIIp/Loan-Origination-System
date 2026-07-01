package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * EMBEDDING vs REFERENCING decision for EmploymentInfo:
 *
 * WHY EMBEDDED (inside Applicant)?
 * ─────────────────────────────────────────────────────
 * Employment information is:
 * 1. Tightly bound to one applicant — no other entity ever references it.
 * 2. Always read alongside the applicant (the BRE needs income + employment type
 *    from the same record at the same time).
 * 3. Small and bounded (5–7 fields) — no risk of hitting MongoDB's 16MB doc limit.
 * 4. Updated atomically with the parent applicant during income re-verification.
 *
 * Referencing would add a useless indirection layer with no benefit here.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import com.los.backend.model.enums.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * EmploymentInfo — the applicant's employment and income details.
 * Embedded as a sub-document inside the Applicant MongoDB document.
 *
 * Why BigDecimal for monetary values?
 * ─────────────────────────────────────
 * Java's double and float use binary floating-point arithmetic, which cannot
 * represent many decimal fractions exactly. For example, 0.1 + 0.2 ≠ 0.3 in
 * double arithmetic. For financial amounts (income, loan values), even a tiny
 * rounding error can compound into significant discrepancies at scale.
 * BigDecimal uses arbitrary-precision decimal arithmetic — always exact.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentInfo {

    /**
     * employmentType — how the applicant earns income.
     * Used by BRE EMPLOYMENT_STATUS rule to filter eligible applicant types.
     * @NotNull — employment type must be known; it directly affects risk scoring.
     */
    @NotNull(message = "Employment type is required")
    @Field("employment_type")
    private EmploymentType employmentType;

    /**
     * employerName — name of the company, organisation, or business.
     * Optional for SELF_EMPLOYED (might just be their own name).
     * Stored for manual verification against LinkedIn/MCA records if needed.
     */
    @Size(max = 200, message = "Employer name must not exceed 200 characters")
    @Field("employer_name")
    private String employerName;

    /**
     * monthlyIncome — gross monthly income in Indian Rupees (INR).
     *
     * @NotNull — income is a mandatory field; the BRE cannot calculate
     *            debt-to-income ratio or loan-to-income ratio without it.
     * @DecimalMin — must be ≥ 0. We allow 0 for UNEMPLOYED/RETIRED edge cases,
     *              but MINIMUM_INCOME credit rules will reject those at evaluation time.
     * @Digits — at most 12 integer digits (₹999,999,999,999 max) and 2 decimal places —
     *           enough for any realistic income; prevents malformed inputs.
     */
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.00", message = "Monthly income cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Monthly income must be a valid monetary amount")
    @Field("monthly_income")
    private BigDecimal monthlyIncome;

    /**
     * totalMonthlyEmi — sum of ALL existing loan EMI obligations per month.
     * Used by the BRE DEBT_TO_INCOME rule:
     *   DTI% = (totalMonthlyEmi / monthlyIncome) × 100
     * If null, the BRE assumes 0 (no existing debts) — this is an optimistic
     * assumption flagged for manual review in the UNDER_REVIEW stage.
     */
    @DecimalMin(value = "0.00", message = "Total EMI cannot be negative")
    @Digits(integer = 12, fraction = 2)
    @Field("total_monthly_emi")
    @Builder.Default
    private BigDecimal totalMonthlyEmi = BigDecimal.ZERO;

    /**
     * creditScore — the applicant's credit bureau score (e.g., CIBIL score in India).
     * Range: 300–900 in the Indian CIBIL system.
     * Stored here (on Applicant) because it describes the person, not the loan.
     * The BRE CREDIT_SCORE rule compares this against the rule's minimumScore parameter.
     * Nullable — some first-time borrowers have no credit history (NTC: New To Credit).
     */
    @Field("credit_score")
    private Integer creditScore;

    /**
     * yearsOfExperience — total work experience in years.
     * Some lenders require a minimum work tenure (e.g., at least 1 year at current employer).
     * Stored for use in future CreditRule types (WORK_EXPERIENCE).
     */
    @Field("years_of_experience")
    private Integer yearsOfExperience;
}
