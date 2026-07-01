package com.los.backend.dto.response;

import com.los.backend.model.enums.FinalDecision;
import com.los.backend.model.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    
    private String id;

    
    private String applicantId;

    
    private ApplicantSummary applicantSummary;

    

    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String purpose;
    private String loanProductType;
    private BigDecimal interestRatePercent;

    

    private LoanStatus status;
    private LoanStatus previousStatus;
    private Instant statusUpdatedAt;
    private String assignedOfficerId;

    

    
    private DecisionResultResponse decisionResult;

    

    
    private String rejectionReason;

    

    private Instant createdAt;
    private Instant updatedAt;

    

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantSummary {
        private String id;
        private String fullName;
        private String email;
        private String phone;
        private String maskedPanNumber;
        private Integer creditScore;
        private BigDecimal monthlyIncome;
        private BigDecimal totalMonthlyEmi;
        private String employmentType;
        
        private long verifiedKycDocumentCount;
        
        private long pendingOrRejectedKycCount;
    }

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionResultResponse {
        private FinalDecision finalDecision;
        private Integer riskScore;
        private Instant evaluatedAt;
        private String evaluatorVersion;
        private String overallComment;
        
        private List<RuleOutcomeResponse> ruleOutcomes;
    }

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleOutcomeResponse {
        private String ruleName;
        private boolean passed;
        private String reason;
        private String actualValue;
        private String thresholdValue;
    }

    

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse {
        
        private List<LoanApplicationResponse> content;
        
        private int pageNumber;
        
        private int pageSize;
        
        private long totalElements;
        
        private int totalPages;
        
        private boolean hasNext;
        
        private boolean hasPrevious;
    }
}
