package com.los.backend.dto.request;

import com.los.backend.model.enums.LoanStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoanStatusRequest {

    
    @NotNull(message = "New status is required. Valid values: KYC_PENDING, UNDER_REVIEW, CREDIT_CHECK, APPROVED, REJECTED, DISBURSED")
    private LoanStatus newStatus;

    
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    
    @Size(max = 50)
    private String officerId;
}
