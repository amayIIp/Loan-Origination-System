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


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantEvaluationContext {

    

    
    private String applicantId;

    
    private String applicationId;

    
    private String applicantName;

    

    
    private LocalDate dateOfBirth;

    
    private int ageInYears;

    

    
    private Integer creditScore;

    

    
    private BigDecimal monthlyIncomeInr;

    
    private BigDecimal totalMonthlyEmiInr;

    
    private Double debtToIncomeRatio;

    

    
    private BigDecimal requestedLoanAmountInr;

    
    private int tenureMonths;

    
    private String loanProductType;

    
    private Double loanToIncomeRatio;

    

    
    private EmploymentType employmentType;

    
    private Integer yearsOfExperience;

    

    
    public static ApplicantEvaluationContext from(Applicant applicant, LoanApplication application) {
        
        
        
        int age = 0;
        if (applicant.getDateOfBirth() != null) {
            age = Period.between(applicant.getDateOfBirth(), LocalDate.now()).getYears();
        }

        
        BigDecimal income = applicant.getEmploymentInfo() != null
            ? applicant.getEmploymentInfo().getMonthlyIncome()
            : null;

        BigDecimal emi = applicant.getEmploymentInfo() != null
            && applicant.getEmploymentInfo().getTotalMonthlyEmi() != null
            ? applicant.getEmploymentInfo().getTotalMonthlyEmi()
            : BigDecimal.ZERO;

        
        
        Double dti = null;
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0) {
            
            
            dti = emi.doubleValue() / income.doubleValue();
        }

        
        
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
