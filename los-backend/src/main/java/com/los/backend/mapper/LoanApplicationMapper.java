package com.los.backend.mapper;

import com.los.backend.dto.request.SubmitLoanApplicationRequest;
import com.los.backend.dto.response.LoanApplicationResponse;
import com.los.backend.model.Applicant;
import com.los.backend.model.DecisionResult;
import com.los.backend.model.LoanApplication;
import com.los.backend.model.enums.LoanStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class LoanApplicationMapper {

    
    private final ApplicantMapper applicantMapper;

    
    public LoanApplicationMapper(ApplicantMapper applicantMapper) {
        this.applicantMapper = applicantMapper;
    }

    

    
    public LoanApplication toModel(SubmitLoanApplicationRequest req) {
        return LoanApplication.builder()
            .applicantId(req.getApplicantId())
            .loanAmount(req.getLoanAmount())
            .tenureMonths(req.getTenureMonths())
            .purpose(req.getPurpose().trim())
            
            .loanProductType(
                req.getLoanProductType() != null && !req.getLoanProductType().isBlank()
                    ? req.getLoanProductType().toUpperCase()
                    : "PERSONAL_LOAN"
            )
            
            .status(LoanStatus.SUBMITTED)
            
            .previousStatus(null)
            
            .statusUpdatedAt(Instant.now())
            
            .decisionResult(null)
            .build();
    }

    

    
    public LoanApplicationResponse toResponse(LoanApplication app) {
        return buildResponse(app, null);
    }

    
    public LoanApplicationResponse toDetailResponse(LoanApplication app, Applicant applicant) {
        
        LoanApplicationResponse.ApplicantSummary summary =
            applicantMapper.buildApplicantSummary(applicant);
        return buildResponse(app, summary);
    }

    
    public List<LoanApplicationResponse> toResponseList(List<LoanApplication> apps) {
        if (apps == null) return Collections.emptyList();
        return apps.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    

    
    private LoanApplicationResponse buildResponse(LoanApplication app,
                                                   LoanApplicationResponse.ApplicantSummary summary) {
        return LoanApplicationResponse.builder()
            .id(app.getId())
            .applicantId(app.getApplicantId())
            .applicantSummary(summary)           
            .loanAmount(app.getLoanAmount())
            .tenureMonths(app.getTenureMonths())
            .purpose(app.getPurpose())
            .loanProductType(app.getLoanProductType())
            .interestRatePercent(app.getInterestRatePercent())
            .status(app.getStatus())
            .previousStatus(app.getPreviousStatus())
            .statusUpdatedAt(app.getStatusUpdatedAt())
            .assignedOfficerId(app.getAssignedOfficerId())
            
            .decisionResult(mapDecisionResult(app.getDecisionResult()))
            .rejectionReason(app.getRejectionReason())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt())
            .build();
    }

    
    private LoanApplicationResponse.DecisionResultResponse mapDecisionResult(DecisionResult dr) {
        if (dr == null) return null;

        
        List<LoanApplicationResponse.RuleOutcomeResponse> outcomes =
            dr.getRuleOutcomes() == null ? Collections.emptyList() :
            dr.getRuleOutcomes().stream()
                .map(ro -> LoanApplicationResponse.RuleOutcomeResponse.builder()
                    .ruleName(ro.getRuleName())
                    .passed(ro.isPassed())
                    .reason(ro.getReason())
                    .actualValue(ro.getActualValue())
                    .thresholdValue(ro.getThresholdValue())
                    .build())
                .collect(Collectors.toList());

        return LoanApplicationResponse.DecisionResultResponse.builder()
            .finalDecision(dr.getFinalDecision())
            .riskScore(dr.getRiskScore())
            .evaluatedAt(dr.getEvaluatedAt())
            .evaluatorVersion(dr.getEvaluatorVersion())
            .overallComment(dr.getOverallComment())
            .ruleOutcomes(outcomes)
            .build();
    }
}
