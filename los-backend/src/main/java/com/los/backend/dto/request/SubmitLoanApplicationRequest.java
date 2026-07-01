package com.los.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SubmitLoanApplicationRequest — request body DTO for POST /api/applications.
 *
 * The client provides the applicantId (must reference an existing Applicant),
 * loan amount, tenure, and purpose. The service validates the applicant exists,
 * that the amounts are within policy limits, and that no active application
 * is already open for this applicant before persisting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitLoanApplicationRequest {

    /**
     * applicantId — the MongoDB _id of the Applicant submitting this loan.
     * The service layer verifies this ID exists in the "applicants" collection.
     * @NotBlank — must be provided; we cannot create a loan without an owner.
     */
    @NotBlank(message = "Applicant ID is required — create an applicant first via POST /api/applicants")
    private String applicantId;

    /**
     * loanAmount — the amount requested, in Indian Rupees (INR).
     * @NotNull + @DecimalMin — must be a positive monetary value.
     * @DecimalMax — capped at ₹1 crore via this DTO (service applies product limits).
     */
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is ₹1,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount via this API is ₹1,00,00,000")
    @Digits(integer = 10, fraction = 2, message = "Loan amount must be a valid monetary amount with up to 2 decimal places")
    private BigDecimal loanAmount;

    /**
     * tenureMonths — repayment duration in months.
     * 6 months minimum (sub-6-month loans are a different microfinance product).
     * 360 months maximum (30-year home loan is the longest typical product).
     */
    @NotNull(message = "Loan tenure in months is required")
    @Min(value = 6, message = "Minimum loan tenure is 6 months")
    @Max(value = 360, message = "Maximum loan tenure is 360 months (30 years)")
    private Integer tenureMonths;

    /**
     * purpose — why the applicant needs this loan.
     * @NotBlank — required for compliance and fraud detection heuristics.
     * @Size — at least 10 chars to prevent lazy "loan" or "need money" submissions.
     */
    @NotBlank(message = "Loan purpose is required")
    @Size(min = 10, max = 500,
          message = "Loan purpose must be between 10 and 500 characters — please be descriptive")
    private String purpose;

    /**
     * loanProductType — the type of loan product being applied for.
     * Examples: "PERSONAL_LOAN", "HOME_LOAN", "EDUCATION_LOAN", "VEHICLE_LOAN"
     * Used by the BRE to load product-specific CreditRules.
     * Optional — defaults to "PERSONAL_LOAN" in the service layer if not provided.
     */
    @Size(max = 50)
    private String loanProductType;
}
