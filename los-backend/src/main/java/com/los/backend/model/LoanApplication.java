package com.los.backend.model;



import com.los.backend.model.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "loan_applications")





@CompoundIndexes({
    @CompoundIndex(
        name = "idx_status_created",
        def = "{'status': 1, 'created_at': -1}",
        
        
        background = true
    ),
    @CompoundIndex(
        name = "idx_applicant_status",
        def = "{'applicant_id': 1, 'status': 1}",
        background = true
    )
})
public class LoanApplication {

    
    @Id
    private String id;

    
    @NotBlank(message = "Applicant ID is required")
    @Indexed
    @Field("applicant_id")
    private String applicantId;

    

    
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Loan amount must be at least ₹1,000")
    @DecimalMax(value = "10000000.00", message = "Loan amount cannot exceed ₹1,00,00,000")
    @Digits(integer = 10, fraction = 2)
    @Field("loan_amount")
    private BigDecimal loanAmount;

    
    @NotNull(message = "Loan tenure is required")
    @Min(value = 6, message = "Minimum loan tenure is 6 months")
    @Max(value = 360, message = "Maximum loan tenure is 360 months (30 years)")
    @Field("tenure_months")
    private Integer tenureMonths;

    
    @NotBlank(message = "Loan purpose is required")
    @Size(max = 500, message = "Loan purpose must not exceed 500 characters")
    @Field("purpose")
    private String purpose;

    
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    @Field("interest_rate_percent")
    private BigDecimal interestRatePercent;

    

    
    @NotNull(message = "Loan status is required")
    @Indexed
    @Field("status")
    @Builder.Default
    private LoanStatus status = LoanStatus.SUBMITTED;

    
    @Field("previous_status")
    private LoanStatus previousStatus;

    
    @Field("status_updated_at")
    private Instant statusUpdatedAt;

    
    @Field("assigned_officer_id")
    private String assignedOfficerId;

    

    
    @Valid
    @Field("decision_result")
    private DecisionResult decisionResult;

    

    
    @Size(max = 1000)
    @Field("rejection_reason")
    private String rejectionReason;

    
    @Size(max = 2000)
    @Field("internal_notes")
    private String internalNotes;

    

    
    @CreatedDate
    @Indexed
    @Field("created_at")
    private Instant createdAt;

    
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
