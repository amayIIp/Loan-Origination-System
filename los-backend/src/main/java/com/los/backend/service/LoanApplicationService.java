package com.los.backend.service;

import com.los.backend.dto.request.SubmitLoanApplicationRequest;
import com.los.backend.dto.request.UpdateLoanStatusRequest;
import com.los.backend.dto.response.LoanApplicationResponse;
import com.los.backend.exception.BusinessRuleException;
import com.los.backend.exception.ResourceNotFoundException;
import com.los.backend.mapper.ApplicantMapper;
import com.los.backend.mapper.LoanApplicationMapper;
import com.los.backend.model.Applicant;
import com.los.backend.model.LoanApplication;
import com.los.backend.model.enums.LoanStatus;
import com.los.backend.repository.LoanApplicationRepository;
import com.los.backend.statemachine.LoanStatusStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;


@Service
@Slf4j
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ApplicantService           applicantService;       
    private final ApplicantMapper            applicantMapper;        
    private final LoanApplicationMapper      loanApplicationMapper;  
    private final LoanStatusStateMachine     stateMachine;           

    public LoanApplicationService(
            LoanApplicationRepository loanApplicationRepository,
            ApplicantService applicantService,
            ApplicantMapper applicantMapper,
            LoanApplicationMapper loanApplicationMapper,
            LoanStatusStateMachine stateMachine) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.applicantService          = applicantService;
        this.applicantMapper           = applicantMapper;
        this.loanApplicationMapper     = loanApplicationMapper;
        this.stateMachine              = stateMachine;
    }

    
    
    

    
    public LoanApplicationResponse submitApplication(SubmitLoanApplicationRequest request) {

        
        log.info("[LoanApplicationService] SUBMIT APPLICATION | applicantId={} | amount={} | tenureMonths={} | purpose='{}'",
                 request.getApplicantId(),
                 request.getLoanAmount(),
                 request.getTenureMonths(),
                 
                 request.getPurpose().length() > 50
                     ? request.getPurpose().substring(0, 50) + "..."
                     : request.getPurpose());

        
        
        
        
        if (!applicantService.existsById(request.getApplicantId())) {
            log.warn("[LoanApplicationService] SUBMIT REJECTED — applicant not found | applicantId={}",
                     request.getApplicantId());
            throw new ResourceNotFoundException("Applicant", "id", request.getApplicantId());
        }

        
        
        
        List<LoanStatus> activeStatuses = Arrays.asList(
            LoanStatus.SUBMITTED,
            LoanStatus.KYC_PENDING,
            LoanStatus.UNDER_REVIEW,
            LoanStatus.CREDIT_CHECK,
            LoanStatus.APPROVED
        );

        boolean hasActiveApplication = loanApplicationRepository
            .existsByApplicantIdAndStatusIn(request.getApplicantId(), activeStatuses);

        if (hasActiveApplication) {
            log.warn("[LoanApplicationService] SUBMIT REJECTED — active application exists | applicantId={}",
                     request.getApplicantId());
            
            
            throw new BusinessRuleException(
                "ACTIVE_APPLICATION_EXISTS",
                "Applicant already has an active loan application. " +
                "A new application can only be submitted after the current one reaches " +
                "a terminal state (APPROVED → DISBURSED, or REJECTED)."
            );
        }

        
        
        LoanApplication newApplication = loanApplicationMapper.toModel(request);

        
        LoanApplication saved = loanApplicationRepository.save(newApplication);

        
        log.info("[LoanApplicationService] SUBMIT APPLICATION success | applicationId={} | applicantId={} | status={}",
                 saved.getId(), saved.getApplicantId(), saved.getStatus());

        
        return loanApplicationMapper.toResponse(saved);
    }

    
    
    

    
    public LoanApplicationResponse getApplicationById(String applicationId) {
        log.debug("[LoanApplicationService] GET APPLICATION DETAIL | applicationId={}", applicationId);

        
        LoanApplication application = loanApplicationRepository.findById(applicationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("LoanApplication", "id", applicationId));

        
        
        Applicant applicant = applicantService.getApplicantModelById(application.getApplicantId());

        
        return loanApplicationMapper.toDetailResponse(application, applicant);
    }

    
    
    

    
    public LoanApplicationResponse.PagedResponse listApplications(
            LoanStatus status, int page, int size, String sortBy) {

        log.debug("[LoanApplicationService] LIST APPLICATIONS | status={} | page={} | size={} | sortBy={}",
                  status, page, size, sortBy);

        
        
        
        int safePage = Math.max(0, page);           
        int safeSize = Math.min(Math.max(1, size), 100); 

        
        
        
        
        String sortField = switch (sortBy != null ? sortBy.toLowerCase() : "createdat") {
            case "createdat", "created_at" -> "created_at";
            case "updatedat", "updated_at" -> "updated_at";
            case "loanamount", "loan_amount" -> "loan_amount";
            case "status" -> "status";
            
            default -> {
                log.warn("[LoanApplicationService] Unknown sortBy='{}', defaulting to 'created_at'", sortBy);
                yield "created_at";
            }
        };

        
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortField));

        
        Page<LoanApplication> resultPage;
        if (status != null) {
            
            resultPage = loanApplicationRepository.findByStatus(status, pageable);
        } else {
            
            resultPage = loanApplicationRepository.findAll(pageable);
        }

        
        return LoanApplicationResponse.PagedResponse.builder()
            
            .content(loanApplicationMapper.toResponseList(resultPage.getContent()))
            .pageNumber(resultPage.getNumber())
            .pageSize(resultPage.getSize())
            .totalElements(resultPage.getTotalElements())
            .totalPages(resultPage.getTotalPages())
            .hasNext(resultPage.hasNext())
            .hasPrevious(resultPage.hasPrevious())
            .build();
    }

    
    
    

    
    public LoanApplicationResponse updateApplicationStatus(
            String applicationId, UpdateLoanStatusRequest request) {

        
        log.info("[LoanApplicationService] UPDATE STATUS start | applicationId={} | requestedStatus={} | officerId={}",
                 applicationId, request.getNewStatus(), request.getOfficerId());

        
        LoanApplication application = loanApplicationRepository.findById(applicationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("LoanApplication", "id", applicationId));

        LoanStatus currentStatus = application.getStatus();
        LoanStatus newStatus     = request.getNewStatus();

        
        log.info("[LoanApplicationService] UPDATE STATUS | applicationId={} | FROM={} → TO={} | applicantId={}",
                 applicationId, currentStatus, newStatus, application.getApplicantId());

        
        
        
        stateMachine.validate(currentStatus, newStatus);

        
        if (LoanStatus.REJECTED.equals(newStatus)) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                log.warn("[LoanApplicationService] REJECT without reason attempted | applicationId={}",
                         applicationId);
                throw new BusinessRuleException(
                    "REJECTION_REASON_REQUIRED",
                    "A rejection reason is mandatory when setting status to REJECTED. " +
                    "Please provide a clear reason so the applicant can be informed."
                );
            }
        }

        
        
        application.setPreviousStatus(currentStatus);
        application.setStatus(newStatus);
        
        application.setStatusUpdatedAt(Instant.now());

        
        if (request.getReason() != null && !request.getReason().isBlank()) {
            application.setRejectionReason(request.getReason().trim());
        }

        
        if (request.getOfficerId() != null && !request.getOfficerId().isBlank()) {
            application.setAssignedOfficerId(request.getOfficerId().trim());
        }

        
        
        LoanApplication updated = loanApplicationRepository.save(application);

        
        log.info("[LoanApplicationService] UPDATE STATUS success | applicationId={} | FROM={} → TO={} | applicantId={} | officerId={}",
                 applicationId, currentStatus, newStatus,
                 updated.getApplicantId(), request.getOfficerId());

        
        
        
        return loanApplicationMapper.toResponse(updated);
    }
}
