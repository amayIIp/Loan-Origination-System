package com.los.backend.service;

import com.los.backend.dto.request.SubmitLoanApplicationRequest;
import com.los.backend.dto.request.UpdateLoanStatusRequest;
import com.los.backend.dto.response.LoanApplicationResponse;
import com.los.backend.exception.BusinessRuleException;
import com.los.backend.exception.InvalidStateTransitionException;
import com.los.backend.exception.ResourceNotFoundException;
import com.los.backend.mapper.ApplicantMapper;
import com.los.backend.mapper.LoanApplicationMapper;
import com.los.backend.model.LoanApplication;
import com.los.backend.model.enums.LoanStatus;
import com.los.backend.repository.LoanApplicationRepository;
import com.los.backend.statemachine.LoanStatusStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    
    
    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private ApplicantService           applicantService;
    @Mock private ApplicantMapper            applicantMapper;
    @Mock private LoanApplicationMapper      loanApplicationMapper;

    
    
    private LoanStatusStateMachine stateMachine;

    
    @InjectMocks
    private LoanApplicationService loanApplicationService;

    

    
    @BeforeEach
    void setUp() {
        
        stateMachine = new LoanStatusStateMachine();
        
        loanApplicationService = new LoanApplicationService(
            loanApplicationRepository,
            applicantService,
            applicantMapper,
            loanApplicationMapper,
            stateMachine
        );
    }

    
    private SubmitLoanApplicationRequest buildSubmitRequest() {
        return SubmitLoanApplicationRequest.builder()
            .applicantId("applicant-001")
            .loanAmount(new BigDecimal("500000.00"))
            .tenureMonths(24)
            .purpose("Home renovation — kitchen and bathroom upgrade")
            .loanProductType("PERSONAL_LOAN")
            .build();
    }

    
    private LoanApplication buildSavedApplication(String id, LoanStatus status) {
        return LoanApplication.builder()
            .id(id)
            .applicantId("applicant-001")
            .loanAmount(new BigDecimal("500000.00"))
            .tenureMonths(24)
            .purpose("Home renovation")
            .status(status)
            .statusUpdatedAt(Instant.now())
            .build();
    }

    
    private LoanApplicationResponse buildApplicationResponse(String id, LoanStatus status) {
        return LoanApplicationResponse.builder()
            .id(id)
            .applicantId("applicant-001")
            .status(status)
            .build();
    }

    
    
    

    
    @Nested
    @DisplayName("submitApplication()")
    class SubmitApplicationTests {

        @Test
        @DisplayName("should create application when applicant exists and no active application")
        void shouldCreateApplicationSuccessfully() {
            
            SubmitLoanApplicationRequest request = buildSubmitRequest();
            LoanApplication savedApp = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            LoanApplicationResponse expectedResponse = buildApplicationResponse("app-001", LoanStatus.SUBMITTED);

            
            when(applicantService.existsById("applicant-001")).thenReturn(true);

            
            when(loanApplicationRepository.existsByApplicantIdAndStatusIn(
                eq("applicant-001"), anyList())).thenReturn(false);

            
            when(loanApplicationMapper.toModel(request)).thenReturn(savedApp);

            
            when(loanApplicationRepository.save(savedApp)).thenReturn(savedApp);

            
            when(loanApplicationMapper.toResponse(savedApp)).thenReturn(expectedResponse);

            
            LoanApplicationResponse result = loanApplicationService.submitApplication(request);

            
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("app-001");
            assertThat(result.getStatus()).isEqualTo(LoanStatus.SUBMITTED);

            
            verify(loanApplicationRepository, times(1)).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when applicant does not exist")
        void shouldThrowWhenApplicantNotFound() {
            
            SubmitLoanApplicationRequest request = buildSubmitRequest();
            when(applicantService.existsById("applicant-001")).thenReturn(false);

            
            assertThatThrownBy(() -> loanApplicationService.submitApplication(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant")
                .hasMessageContaining("applicant-001");

            
            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when applicant already has active application")
        void shouldThrowWhenActiveApplicationExists() {
            
            SubmitLoanApplicationRequest request = buildSubmitRequest();
            when(applicantService.existsById("applicant-001")).thenReturn(true);
            when(loanApplicationRepository.existsByApplicantIdAndStatusIn(
                eq("applicant-001"), anyList())).thenReturn(true);

            
            assertThatThrownBy(() -> loanApplicationService.submitApplication(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active loan application");

            
            verify(loanApplicationRepository, never()).save(any());
        }
    }

    
    
    

    @Nested
    @DisplayName("updateApplicationStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should transition from SUBMITTED to KYC_PENDING successfully")
        void shouldTransitionFromSubmittedToKycPending() {
            
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            LoanApplication updatedApp = buildSavedApplication("app-001", LoanStatus.KYC_PENDING);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.KYC_PENDING)
                .officerId("officer-001")
                .build();
            LoanApplicationResponse expectedResponse = buildApplicationResponse("app-001", LoanStatus.KYC_PENDING);

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));
            when(loanApplicationRepository.save(any())).thenReturn(updatedApp);
            when(loanApplicationMapper.toResponse(updatedApp)).thenReturn(expectedResponse);

            
            LoanApplicationResponse result =
                loanApplicationService.updateApplicationStatus("app-001", request);

            
            assertThat(result.getStatus()).isEqualTo(LoanStatus.KYC_PENDING);
            verify(loanApplicationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException for illegal transition SUBMITTED → DISBURSED")
        void shouldThrowForIllegalTransition() {
            
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.DISBURSED)
                .build();

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));

            
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("app-001", request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("SUBMITTED")
                .hasMessageContaining("DISBURSED");

            
            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when rejecting without a reason")
        void shouldThrowWhenRejectingWithoutReason() {
            
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.REJECTED)
                .reason(null)   
                .build();

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));

            
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("app-001", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rejection reason is mandatory");

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when application does not exist")
        void shouldThrowWhenApplicationNotFound() {
            
            when(loanApplicationRepository.findById("nonexistent-id"))
                .thenReturn(Optional.empty());

            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.KYC_PENDING)
                .build();

            
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("nonexistent-id", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LoanApplication")
                .hasMessageContaining("nonexistent-id");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when transitioning from terminal REJECTED state")
        void shouldThrowWhenTransitioningFromTerminalState() {
            
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.REJECTED);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.UNDER_REVIEW)  
                .build();

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));

            
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("app-001", request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("REJECTED");
        }
    }

    
    
    

    @Nested
    @DisplayName("LoanStatusStateMachine")
    class StateMachineTests {

        private LoanStatusStateMachine sm;

        @BeforeEach
        void init() {
            sm = new LoanStatusStateMachine();
        }

        @Test
        @DisplayName("should allow SUBMITTED → KYC_PENDING")
        void allowSubmittedToKycPending() {
            assertThatCode(() -> sm.validate(LoanStatus.SUBMITTED, LoanStatus.KYC_PENDING))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should allow CREDIT_CHECK → APPROVED")
        void allowCreditCheckToApproved() {
            assertThatCode(() -> sm.validate(LoanStatus.CREDIT_CHECK, LoanStatus.APPROVED))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject DISBURSED → any (terminal state)")
        void rejectFromDisbursed() {
            for (LoanStatus target : LoanStatus.values()) {
                if (target != LoanStatus.DISBURSED) {
                    assertThatThrownBy(() -> sm.validate(LoanStatus.DISBURSED, target))
                        .isInstanceOf(InvalidStateTransitionException.class);
                }
            }
        }

        @Test
        @DisplayName("should correctly identify REJECTED and DISBURSED as terminal")
        void identifyTerminalStates() {
            assertThat(sm.isTerminal(LoanStatus.REJECTED)).isTrue();
            assertThat(sm.isTerminal(LoanStatus.DISBURSED)).isTrue();
            assertThat(sm.isTerminal(LoanStatus.SUBMITTED)).isFalse();
            assertThat(sm.isTerminal(LoanStatus.APPROVED)).isFalse();
        }
    }
}
