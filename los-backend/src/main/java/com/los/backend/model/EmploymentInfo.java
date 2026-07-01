package com.los.backend.model;



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


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentInfo {

    
    @NotNull(message = "Employment type is required")
    @Field("employment_type")
    private EmploymentType employmentType;

    
    @Size(max = 200, message = "Employer name must not exceed 200 characters")
    @Field("employer_name")
    private String employerName;

    
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.00", message = "Monthly income cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Monthly income must be a valid monetary amount")
    @Field("monthly_income")
    private BigDecimal monthlyIncome;

    
    @DecimalMin(value = "0.00", message = "Total EMI cannot be negative")
    @Digits(integer = 12, fraction = 2)
    @Field("total_monthly_emi")
    @Builder.Default
    private BigDecimal totalMonthlyEmi = BigDecimal.ZERO;

    
    @Field("credit_score")
    private Integer creditScore;

    
    @Field("years_of_experience")
    private Integer yearsOfExperience;
}
