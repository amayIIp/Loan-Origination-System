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

/**
 * LoanApplicationServiceTest — unit tests for LoanApplicationService business logic.
 *
 * What is a Unit Test? (beginner explanation)
 * ────────────────────────────────────────────
 * A unit test tests ONE class in ISOLATION. We "mock" (fake) all its dependencies
 * so the test only verifies the logic inside the class being tested.
 *
 * For example: when testing submitApplication(), we don't talk to a real MongoDB.
 * Instead, Mockito creates a fake "loanApplicationRepository" that we configure
 * to return specific values. This makes tests:
 *   - Fast (no network/DB calls)
 *   - Deterministic (no random data from a real DB)
 *   - Focused (if the test fails, the problem is in this service, not the DB)
 *
 * Framework choices:
 *   @ExtendWith(MockitoExtension.class) — enables Mockito's JUnit 5 integration
 *   @Mock — creates a fake (mock) instance of the annotated type
 *   @InjectMocks — creates the REAL instance of the class under test, injecting mocks
 *   AssertJ — fluent assertion library (assertThat(...).isEqualTo(...))
 */
@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    // ── Mocks (fakes) of all dependencies ────────────────────────────────────
    // Mockito creates these automatically — no real DB or Spring context needed
    @Mock private LoanApplicationRepository loanApplicationRepository;
    @Mock private ApplicantService           applicantService;
    @Mock private ApplicantMapper            applicantMapper;
    @Mock private LoanApplicationMapper      loanApplicationMapper;

    // We use the REAL state machine, not a mock — it has no external dependencies
    // and we want to test real transition logic. We inject it manually below.
    private LoanStatusStateMachine stateMachine;

    // The class under test — @InjectMocks injects all @Mock fields into it
    @InjectMocks
    private LoanApplicationService loanApplicationService;

    // ── Test data builders ────────────────────────────────────────────────────

    /**
     * @BeforeEach — runs before EVERY test method.
     * Re-initialises the real state machine so each test gets a clean instance.
     * We also inject it into the service manually (since @InjectMocks doesn't
     * know we want the real one).
     */
    @BeforeEach
    void setUp() {
        // Use the REAL state machine to test real transition validation
        stateMachine = new LoanStatusStateMachine();
        // Recreate the service with the real state machine + all mocks
        loanApplicationService = new LoanApplicationService(
            loanApplicationRepository,
            applicantService,
            applicantMapper,
            loanApplicationMapper,
            stateMachine
        );
    }

    /** Helper: build a SubmitLoanApplicationRequest with sensible test defaults */
    private SubmitLoanApplicationRequest buildSubmitRequest() {
        return SubmitLoanApplicationRequest.builder()
            .applicantId("applicant-001")
            .loanAmount(new BigDecimal("500000.00"))
            .tenureMonths(24)
            .purpose("Home renovation — kitchen and bathroom upgrade")
            .loanProductType("PERSONAL_LOAN")
            .build();
    }

    /** Helper: build a saved LoanApplication with a generated id */
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

    /** Helper: build a minimal LoanApplicationResponse for mock return values */
    private LoanApplicationResponse buildApplicationResponse(String id, LoanStatus status) {
        return LoanApplicationResponse.builder()
            .id(id)
            .applicantId("applicant-001")
            .status(status)
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NESTED TEST CLASS: submitApplication()
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * @Nested — groups related tests under a descriptive class name.
     * JUnit 5 runs these as a sub-group in the test report:
     *   LoanApplicationServiceTest > submitApplication > should create...
     */
    @Nested
    @DisplayName("submitApplication()")
    class SubmitApplicationTests {

        @Test
        @DisplayName("should create application when applicant exists and no active application")
        void shouldCreateApplicationSuccessfully() {
            // ── Arrange (set up fakes and expected behavior) ──────────────────
            SubmitLoanApplicationRequest request = buildSubmitRequest();
            LoanApplication savedApp = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            LoanApplicationResponse expectedResponse = buildApplicationResponse("app-001", LoanStatus.SUBMITTED);

            // Fake: applicantService.existsById() returns true — applicant exists
            when(applicantService.existsById("applicant-001")).thenReturn(true);

            // Fake: no existing active application
            when(loanApplicationRepository.existsByApplicantIdAndStatusIn(
                eq("applicant-001"), anyList())).thenReturn(false);

            // Fake: mapper creates the model from the request
            when(loanApplicationMapper.toModel(request)).thenReturn(savedApp);

            // Fake: repository saves and returns the saved entity (with generated ID)
            when(loanApplicationRepository.save(savedApp)).thenReturn(savedApp);

            // Fake: mapper converts model to response DTO
            when(loanApplicationMapper.toResponse(savedApp)).thenReturn(expectedResponse);

            // ── Act (call the method being tested) ────────────────────────────
            LoanApplicationResponse result = loanApplicationService.submitApplication(request);

            // ── Assert (verify the result) ────────────────────────────────────
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("app-001");
            assertThat(result.getStatus()).isEqualTo(LoanStatus.SUBMITTED);

            // Verify that the repository's save() was called exactly once
            verify(loanApplicationRepository, times(1)).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when applicant does not exist")
        void shouldThrowWhenApplicantNotFound() {
            // Arrange: applicant doesn't exist
            SubmitLoanApplicationRequest request = buildSubmitRequest();
            when(applicantService.existsById("applicant-001")).thenReturn(false);

            // Act + Assert: expect ResourceNotFoundException to be thrown
            assertThatThrownBy(() -> loanApplicationService.submitApplication(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant")
                .hasMessageContaining("applicant-001");

            // Verify: save() was NEVER called — we failed before reaching the DB
            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when applicant already has active application")
        void shouldThrowWhenActiveApplicationExists() {
            // Arrange: applicant exists BUT already has an active application
            SubmitLoanApplicationRequest request = buildSubmitRequest();
            when(applicantService.existsById("applicant-001")).thenReturn(true);
            when(loanApplicationRepository.existsByApplicantIdAndStatusIn(
                eq("applicant-001"), anyList())).thenReturn(true);

            // Act + Assert: expect BusinessRuleException
            assertThatThrownBy(() -> loanApplicationService.submitApplication(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active loan application");

            // Verify: save() was never called
            verify(loanApplicationRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NESTED TEST CLASS: updateApplicationStatus()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateApplicationStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should transition from SUBMITTED to KYC_PENDING successfully")
        void shouldTransitionFromSubmittedToKycPending() {
            // Arrange
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

            // Act
            LoanApplicationResponse result =
                loanApplicationService.updateApplicationStatus("app-001", request);

            // Assert
            assertThat(result.getStatus()).isEqualTo(LoanStatus.KYC_PENDING);
            verify(loanApplicationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException for illegal transition SUBMITTED → DISBURSED")
        void shouldThrowForIllegalTransition() {
            // Arrange: application is SUBMITTED; client requests DISBURSED (illegal skip)
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.DISBURSED)
                .build();

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));

            // Act + Assert: state machine throws InvalidStateTransitionException
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("app-001", request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("SUBMITTED")
                .hasMessageContaining("DISBURSED");

            // Verify: save() was never called — transition was rejected before any mutation
            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when rejecting without a reason")
        void shouldThrowWhenRejectingWithoutReason() {
            // Arrange: application is SUBMITTED; valid transition to REJECTED, but no reason
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.SUBMITTED);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.REJECTED)
                .reason(null)   // Missing reason — violates business rule
                .build();

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));

            // Act + Assert
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("app-001", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rejection reason is mandatory");

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when application does not exist")
        void shouldThrowWhenApplicationNotFound() {
            // Arrange: repository returns empty Optional (no document found)
            when(loanApplicationRepository.findById("nonexistent-id"))
                .thenReturn(Optional.empty());

            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.KYC_PENDING)
                .build();

            // Act + Assert
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("nonexistent-id", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LoanApplication")
                .hasMessageContaining("nonexistent-id");
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when transitioning from terminal REJECTED state")
        void shouldThrowWhenTransitioningFromTerminalState() {
            // Arrange: application is already REJECTED (terminal)
            LoanApplication app = buildSavedApplication("app-001", LoanStatus.REJECTED);
            UpdateLoanStatusRequest request = UpdateLoanStatusRequest.builder()
                .newStatus(LoanStatus.UNDER_REVIEW)  // Any transition from terminal fails
                .build();

            when(loanApplicationRepository.findById("app-001")).thenReturn(Optional.of(app));

            // Act + Assert: state machine blocks transition from terminal state
            assertThatThrownBy(() ->
                loanApplicationService.updateApplicationStatus("app-001", request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("REJECTED");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NESTED TEST CLASS: LoanStatusStateMachine (standalone unit tests)
    // ═══════════════════════════════════════════════════════════════════════

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
