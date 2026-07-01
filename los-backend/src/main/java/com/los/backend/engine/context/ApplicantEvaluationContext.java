package com.los.backend.engine.context;

import com.los.backend.model.Applicant;
import com.los.backend.model.LoanApplication;
import com.los.backend.model.enums.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * ApplicantEvaluationContext — a single, flat object that aggregates all data
 * the BRE rules need to make their decisions.
 *
 * WHY A DEDICATED CONTEXT OBJECT?
 * ─────────────────────────────────
 * 1. Decoupling: Rules are written against this context, NOT against the raw
 *    Applicant/LoanApplication models. If the domain model changes (e.g., we rename
 *    monthlyIncome to grossMonthlyIncome), we update the context builder in ONE place
 *    rather than hunting for model references across 5+ rule classes.
 *
 * 2. Pre-computation: We compute derived values (age, DTI ratio, loan-to-income ratio)
 *    ONCE when building the context, rather than every rule re-computing them.
 *    Each rule simply reads context.getAge() — no rule knows about LocalDate arithmetic.
 *
 * 3. Testability: In unit tests, we construct an ApplicantEvaluationContext directly
 *    with specific values. No need to build a full Applicant model graph to test a rule.
 *    CreditScoreRuleTest can create: context.withCreditScore(580) and test just that.
 *
 * 4. Immutability: The context is built once per evaluation and read-only. Rules cannot
 *    accidentally mutate shared data — no race conditions in concurrent evaluations.
 *
 * 5. Extensibility: Adding data for a new rule type means adding a field here and
 *    setting it in the builder. Zero changes to existing rules.
 *
 * Design Pattern: Value Object / Parameter Object
 * ─────────────────────────────────────────────────
 * This is a Parameter Object — instead of passing 15 parameters to each rule's evaluate()
 * method, we bundle them into one cohesive object. This keeps method signatures clean
 * and makes the "what data does evaluation need?" question answerable by reading one class.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantEvaluationContext {

    // ── Identity (for logging and DecisionLog cross-references) ───────────────

    /** MongoDB _id of the Applicant being evaluated */
    private String applicantId;

    /** MongoDB _id of the LoanApplication being evaluated */
    private String applicationId;

    /** Full name of the applicant — used in log messages, not in rule logic */
    private String applicantName;

    // ── Age data ──────────────────────────────────────────────────────────────

    /**
     * dateOfBirth — stored raw for audit; ageInYears is the derived value rules use.
     * Storing both means the audit log can show "Born 2002-01-15 → Age 22 at eval time"
     */
    private LocalDate dateOfBirth;

    /**
     * ageInYears — pre-computed age at evaluation time.
     * Computed once in the factory method below — rules read this directly.
     * Period.between(dateOfBirth, LocalDate.now()).getYears() is the Java idiom.
     */
    private int ageInYears;

    // ── Credit data ───────────────────────────────────────────────────────────

    /**
     * creditScore — CIBIL/bureau score (300–900).
     * NULL for New-To-Credit (NTC) applicants who have no credit history.
     * Rules must explicitly handle null — the CreditScoreRule applies a conservative
     * NTC penalty score when this is null rather than crashing.
     */
    private Integer creditScore;

    // ── Income and debt data ──────────────────────────────────────────────────

    /**
     * monthlyIncomeInr — gross monthly income in Indian Rupees.
     * NULL if applicant is UNEMPLOYED (we still build the context; rules will fail it).
     */
    private BigDecimal monthlyIncomeInr;

    /**
     * totalMonthlyEmiInr — sum of all existing loan EMI payments per month.
     * BigDecimal.ZERO if no existing EMIs (never null — defaults to zero in builder).
     */
    private BigDecimal totalMonthlyEmiInr;

    /**
     * debtToIncomeRatio — pre-computed: totalMonthlyEmi / monthlyIncome.
     * Example: ₹20,000 EMI on ₹50,000 income = 0.40 (40% DTI).
     * Null if monthlyIncome is null/zero (cannot compute; DTI rule handles the null).
     * Rules read this directly — no arithmetic needed inside the rule class.
     */
    private Double debtToIncomeRatio;

    // ── Loan request data ─────────────────────────────────────────────────────

    /**
     * requestedLoanAmountInr — what the applicant is asking to borrow.
     */
    private BigDecimal requestedLoanAmountInr;

    /**
     * tenureMonths — requested repayment term in months.
     */
    private int tenureMonths;

    /**
     * loanProductType — e.g., "PERSONAL_LOAN", "HOME_LOAN". Used by CreditRuleRepository
     * to load product-specific rules in addition to universal rules.
     */
    private String loanProductType;

    /**
     * loanToIncomeRatio — pre-computed: requestedLoanAmount / (monthlyIncome × 12).
     * Example: ₹500,000 loan on ₹50,000/month income = 500,000 / 600,000 ≈ 0.83 (0.83× annual income).
     * Null if income is null/zero.
     */
    private Double loanToIncomeRatio;

    // ── Employment data ───────────────────────────────────────────────────────

    /**
     * employmentType — SALARIED, SELF_EMPLOYED, BUSINESS_OWNER, RETIRED, UNEMPLOYED.
     * Used by EmploymentStatusRule to check if the applicant's type is in the
     * allowed list from the CreditRule document.
     */
    private EmploymentType employmentType;

    /**
     * yearsOfExperience — total work experience. Used in some lenders' rules
     * (e.g., minimum 1 year at current employer for SALARIED applicants).
     */
    private Integer yearsOfExperience;

    // ── Factory method: build from domain objects ─────────────────────────────

    /**
     * from — static factory that builds a fully populated ApplicantEvaluationContext
     * from raw domain model objects (Applicant + LoanApplication).
     *
     * This is where ALL pre-computation happens:
     *   - age calculation (Period.between)
     *   - DTI ratio calculation
     *   - Loan-to-income ratio calculation
     *   - Null-safety for missing income/score fields
     *
     * Rules themselves contain ZERO arithmetic — all derived values are ready to read.
     *
     * @param applicant   the Applicant model loaded from MongoDB
     * @param application the LoanApplication model loaded from MongoDB
     * @return a fully populated, immutable-intent context for BRE evaluation
     */
    public static ApplicantEvaluationContext from(Applicant applicant, LoanApplication application) {
        // ── Pre-compute age ───────────────────────────────────────────────────
        // Period.between gives an ISO-8601 duration; .getYears() gives the integer year count.
        // This correctly handles leap years and mid-year birthdays.
        int age = 0;
        if (applicant.getDateOfBirth() != null) {
            age = Period.between(applicant.getDateOfBirth(), LocalDate.now()).getYears();
        }

        // ── Extract income and EMI ────────────────────────────────────────────
        BigDecimal income = applicant.getEmploymentInfo() != null
            ? applicant.getEmploymentInfo().getMonthlyIncome()
            : null;

        BigDecimal emi = applicant.getEmploymentInfo() != null
            && applicant.getEmploymentInfo().getTotalMonthlyEmi() != null
            ? applicant.getEmploymentInfo().getTotalMonthlyEmi()
            : BigDecimal.ZERO;

        // ── Pre-compute DTI ratio ─────────────────────────────────────────────
        // DTI = totalMonthlyEMI / monthlyIncome. Avoid division by zero.
        Double dti = null;
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0) {
            // .doubleValue() is safe here because we're doing ratio math,
            // not storing monetary amounts (which need BigDecimal precision).
            dti = emi.doubleValue() / income.doubleValue();
        }

        // ── Pre-compute Loan-to-Income ratio ──────────────────────────────────
        // LTI = requestedLoanAmount / annualIncome = requestedAmount / (monthlyIncome × 12)
        Double lti = null;
        BigDecimal loanAmount = application.getLoanAmount();
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0 && loanAmount != null) {
            double annualIncome = income.doubleValue() * 12.0;
            lti = loanAmount.doubleValue() / annualIncome;
        }

        return ApplicantEvaluationContext.builder()
            .applicantId(applicant.getId())
            .applicationId(application.getId())
            .applicantName(applicant.getFullName())
            .dateOfBirth(applicant.getDateOfBirth())
            .ageInYears(age)
            .creditScore(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getCreditScore() : null)
            .monthlyIncomeInr(income)
            .totalMonthlyEmiInr(emi)
            .debtToIncomeRatio(dti)
            .requestedLoanAmountInr(loanAmount)
            .tenureMonths(application.getTenureMonths() != null ? application.getTenureMonths() : 0)
            .loanProductType(application.getLoanProductType())
            .loanToIncomeRatio(lti)
            .employmentType(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getEmploymentType() : null)
            .yearsOfExperience(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getYearsOfExperience() : null)
            .build();
    }
}
