package com.los.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitLoanApplicationRequest {

    
    @NotBlank(message = "Applicant ID is required — create an applicant first via POST /api/applicants")
    private String applicantId;

    
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is ₹1,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount via this API is ₹1,00,00,000")
    @Digits(integer = 10, fraction = 2, message = "Loan amount must be a valid monetary amount with up to 2 decimal places")
    private BigDecimal loanAmount;

    
    @NotNull(message = "Loan tenure in months is required")
    @Min(value = 6, message = "Minimum loan tenure is 6 months")
    @Max(value = 360, message = "Maximum loan tenure is 360 months (30 years)")
    private Integer tenureMonths;

    
    @NotBlank(message = "Loan purpose is required")
    @Size(min = 10, max = 500,
          message = "Loan purpose must be between 10 and 500 characters — please be descriptive")
    private String purpose;

    
    @Size(max = 50)
    private String loanProductType;
}
