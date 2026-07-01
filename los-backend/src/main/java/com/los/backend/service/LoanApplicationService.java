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

/**
 * LoanApplicationService — business logic for all loan application operations.
 *
 * This is the most complex service in the LOS. It:
 * 1. Enforces business rules before saving (applicant exists, no active application).
 * 2. Orchestrates the two-query "manual join" pattern (load application + load applicant).
 * 3. Delegates state transition validation to LoanStatusStateMachine.
 * 4. Provides rich structured logging at every critical decision point.
 *
 * Logging strategy for observability:
 * ─────────────────────────────────────
 * We log at service ENTRY and EXIT for the two most important operations:
 *   - submitApplication: creates financial exposure — must be fully traceable
 *   - updateApplicationStatus: changes the loan lifecycle — audit-critical
 * Both logs include: applicantId, applicationId, amounts, and status transitions
 * so a single log-grep can reconstruct the complete journey of any application.
 */
@Service
@Slf4j
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ApplicantService           applicantService;       // for existence check + model load
    private final ApplicantMapper            applicantMapper;        // for building ApplicantSummary
    private final LoanApplicationMapper      loanApplicationMapper;  // for model ↔ DTO conversion
    private final LoanStatusStateMachine     stateMachine;           // for validating status transitions

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

    // ════════════════════════════════════════════════════════════════════════
    // SUBMIT LOAN APPLICATION — POST /api/applications
    // ════════════════════════════════════════════════════════════════════════

    /**
     * submitApplication — creates a new loan application in SUBMITTED status.
     *
     * Full business rule chain (enforced IN ORDER — early rules short-circuit the rest):
     * 1. Applicant must exist (throws 404 if not).
     * 2. Applicant must not have an active (non-terminal) application already open.
     *    Reason: allowing parallel applications for the same person enables fraud
     *    and makes credit risk assessment inaccurate.
     *
     * Observability logging:
     * Entry log: records who is applying, for how much, and for what term.
     * Exit log: records the generated application ID — allows tracing in log aggregators.
     *
     * @param request the validated SubmitLoanApplicationRequest from the controller
     * @return LoanApplicationResponse DTO for the newly created application
     */
    public LoanApplicationResponse submitApplication(SubmitLoanApplicationRequest request) {

        // ── ENTRY LOG — observable audit trail for this financial operation ────
        log.info("[LoanApplicationService] SUBMIT APPLICATION | applicantId={} | amount={} | tenureMonths={} | purpose='{}'",
                 request.getApplicantId(),
                 request.getLoanAmount(),
                 request.getTenureMonths(),
                 // Trim purpose to 50 chars in logs — prevent log injection via long strings
                 request.getPurpose().length() > 50
                     ? request.getPurpose().substring(0, 50) + "..."
                     : request.getPurpose());

        // ── Rule 1: Applicant must exist ──────────────────────────────────────
        // We call applicantService (not the repository directly) to keep the
        // "applicant existence check" logic in one place. If the check ever needs
        // to include "isActive must be true", we update ApplicantService only.
        if (!applicantService.existsById(request.getApplicantId())) {
            log.warn("[LoanApplicationService] SUBMIT REJECTED — applicant not found | applicantId={}",
                     request.getApplicantId());
            throw new ResourceNotFoundException("Applicant", "id", request.getApplicantId());
        }

        // ── Rule 2: No active application already open ────────────────────────
        // Active = any status that is NOT a terminal state (REJECTED or DISBURSED).
        // We define the "active" statuses explicitly to make the rule self-documenting.
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
            // BusinessRuleException with a specific rule code so the frontend can
            // show a targeted message: "You already have an active loan application"
            throw new BusinessRuleException(
                "ACTIVE_APPLICATION_EXISTS",
                "Applicant already has an active loan application. " +
                "A new application can only be submitted after the current one reaches " +
                "a terminal state (APPROVED → DISBURSED, or REJECTED)."
            );
        }

        // ── Map request DTO → LoanApplication model ───────────────────────────
        // status = SUBMITTED, decisionResult = null, id = null (MongoDB generates it)
        LoanApplication newApplication = loanApplicationMapper.toModel(request);

        // ── Persist to MongoDB ────────────────────────────────────────────────
        LoanApplication saved = loanApplicationRepository.save(newApplication);

        // ── EXIT LOG — record the generated application ID for tracing ─────────
        log.info("[LoanApplicationService] SUBMIT APPLICATION success | applicationId={} | applicantId={} | status={}",
                 saved.getId(), saved.getApplicantId(), saved.getStatus());

        // ── Return DTO (list view — no applicant join needed for create response)
        return loanApplicationMapper.toResponse(saved);
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET APPLICATION BY ID (WITH MANUAL JOIN) — GET /api/applications/{id}
    // ════════════════════════════════════════════════════════════════════════

    /**
     * getApplicationById — loads a single loan application with full applicant details.
     *
     * MANUAL JOIN PATTERN (beginner explanation):
     * ─────────────────────────────────────────────
     * MongoDB does not automatically resolve foreign-key references. When we load
     * a LoanApplication, its "applicantId" field is just a String (the Applicant's _id).
     * MongoDB does NOT automatically load the Applicant document for us.
     *
     * We perform the join ourselves in two steps:
     *   Step 1: Load the LoanApplication from "loan_applications" by id.
     *   Step 2: Use the applicantId from step 1 to load the Applicant from "applicants".
     *   Step 3: Build the detail response by merging both — the mapper handles this.
     *
     * This is 2 database round-trips (vs SQL's single JOIN query). It's acceptable
     * here because:
     * a) This endpoint is for a single record (detail view), not a list.
     * b) Both queries use indexed fields (_id) → O(log n), fast.
     * c) We avoid the complexity of MongoDB's $lookup aggregation pipeline for this case.
     *
     * @param applicationId the MongoDB _id of the LoanApplication to fetch
     * @return fully enriched LoanApplicationResponse with ApplicantSummary populated
     */
    public LoanApplicationResponse getApplicationById(String applicationId) {
        log.debug("[LoanApplicationService] GET APPLICATION DETAIL | applicationId={}", applicationId);

        // Step 1: Load the LoanApplication — throws 404 if not found
        LoanApplication application = loanApplicationRepository.findById(applicationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("LoanApplication", "id", applicationId));

        // Step 2: Load the related Applicant using the applicantId reference
        // This is the second database round-trip — the manual "JOIN"
        Applicant applicant = applicantService.getApplicantModelById(application.getApplicantId());

        // Step 3: Build the detail response — mapper merges both objects
        return loanApplicationMapper.toDetailResponse(application, applicant);
    }

    // ════════════════════════════════════════════════════════════════════════
    // LIST APPLICATIONS (PAGINATED + FILTERED) — GET /api/applications
    // ════════════════════════════════════════════════════════════════════════

    /**
     * listApplications — returns a paginated, optionally filtered list of loan applications.
     *
     * Spring Data Pageable (beginner explanation):
     * ─────────────────────────────────────────────
     * Pageable is an object that encapsulates three things:
     *   1. Page number (0-indexed): which "chunk" of results to return
     *   2. Page size: how many items per chunk (e.g., 20)
     *   3. Sort: which field to sort by and in which direction (asc/desc)
     * We construct it here from the raw query parameters (page, size, sortBy).
     *
     * The repository returns a Page<LoanApplication> — a container that holds:
     *   - The items for this page
     *   - Total count of all matching documents (for "Page 1 of 47" display)
     * We convert this to our custom PagedResponse wrapper for the API response.
     *
     * @param status  optional LoanStatus filter — null means "all statuses"
     * @param page    page number (0-indexed, default 0)
     * @param size    items per page (default 20, max 100 enforced here)
     * @param sortBy  field name to sort by (default "createdAt")
     * @return LoanApplicationResponse.PagedResponse with content + pagination metadata
     */
    public LoanApplicationResponse.PagedResponse listApplications(
            LoanStatus status, int page, int size, String sortBy) {

        log.debug("[LoanApplicationService] LIST APPLICATIONS | status={} | page={} | size={} | sortBy={}",
                  status, page, size, sortBy);

        // ── Input sanitisation ────────────────────────────────────────────────
        // Cap page size at 100 — prevent "give me 10,000 records in one request"
        // which would exhaust server memory. The client should use pagination properly.
        int safePage = Math.max(0, page);           // page cannot be negative
        int safeSize = Math.min(Math.max(1, size), 100); // 1 ≤ size ≤ 100

        // ── Validate and sanitise sortBy ──────────────────────────────────────
        // Only allow sorting by known, indexed fields — prevents sort on a non-indexed
        // field that would cause MongoDB to perform an expensive in-memory sort.
        // Map API-friendly names to MongoDB field names.
        String sortField = switch (sortBy != null ? sortBy.toLowerCase() : "createdat") {
            case "createdat", "created_at" -> "created_at";
            case "updatedat", "updated_at" -> "updated_at";
            case "loanamount", "loan_amount" -> "loan_amount";
            case "status" -> "status";
            // Default to created_at (newest first) for any unknown sort field
            default -> {
                log.warn("[LoanApplicationService] Unknown sortBy='{}', defaulting to 'created_at'", sortBy);
                yield "created_at";
            }
        };

        // Build a Pageable: descending sort (newest first is natural for loan queues)
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortField));

        // ── Query MongoDB ─────────────────────────────────────────────────────
        Page<LoanApplication> resultPage;
        if (status != null) {
            // Status filter provided — use the indexed status field
            resultPage = loanApplicationRepository.findByStatus(status, pageable);
        } else {
            // No filter — return all applications (paginated)
            resultPage = loanApplicationRepository.findAll(pageable);
        }

        // ── Build the PagedResponse wrapper ───────────────────────────────────
        return LoanApplicationResponse.PagedResponse.builder()
            // Map each LoanApplication model in this page to a response DTO (list view — no join)
            .content(loanApplicationMapper.toResponseList(resultPage.getContent()))
            .pageNumber(resultPage.getNumber())
            .pageSize(resultPage.getSize())
            .totalElements(resultPage.getTotalElements())
            .totalPages(resultPage.getTotalPages())
            .hasNext(resultPage.hasNext())
            .hasPrevious(resultPage.hasPrevious())
            .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // UPDATE APPLICATION STATUS — PATCH /api/applications/{id}/status
    // ════════════════════════════════════════════════════════════════════════

    /**
     * updateApplicationStatus — transitions a LoanApplication to a new status.
     *
     * This is the most audit-sensitive operation in the LOS:
     * - It moves money closer to or further from disbursement.
     * - Every status change must be traceable (who did it, when, why).
     * - The state machine must reject any illegal transition.
     *
     * Steps:
     * 1. Load the application (404 if not found).
     * 2. Validate the transition via LoanStatusStateMachine (422 if illegal).
     * 3. Apply business rule: rejection requires a reason.
     * 4. Update the application fields (status, previousStatus, timestamps, reason).
     * 5. Persist to MongoDB.
     * 6. Return the updated response DTO.
     *
     * Logging: both ENTRY and EXIT logs are structured (key=value pairs) so a
     * log-aggregation tool (ELK, Splunk, CloudWatch) can parse and query them:
     *   "Find all transitions to REJECTED in the last 24 hours" is a single query.
     *
     * @param applicationId the MongoDB _id of the application to update
     * @param request       the UpdateLoanStatusRequest containing newStatus + reason
     * @return updated LoanApplicationResponse DTO
     */
    public LoanApplicationResponse updateApplicationStatus(
            String applicationId, UpdateLoanStatusRequest request) {

        // ── ENTRY LOG — status transitions must be fully auditable ─────────────
        log.info("[LoanApplicationService] UPDATE STATUS start | applicationId={} | requestedStatus={} | officerId={}",
                 applicationId, request.getNewStatus(), request.getOfficerId());

        // Step 1: Load the application — throws 404 if it doesn't exist
        LoanApplication application = loanApplicationRepository.findById(applicationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("LoanApplication", "id", applicationId));

        LoanStatus currentStatus = application.getStatus();
        LoanStatus newStatus     = request.getNewStatus();

        // Log the BEFORE state for the audit trail
        log.info("[LoanApplicationService] UPDATE STATUS | applicationId={} | FROM={} → TO={} | applicantId={}",
                 applicationId, currentStatus, newStatus, application.getApplicantId());

        // Step 2: Validate the transition via the state machine
        // This throws InvalidStateTransitionException (mapped to 422) if illegal.
        // We call this BEFORE any other mutation — fail fast without any data changes.
        stateMachine.validate(currentStatus, newStatus);

        // Step 3: Business rule — rejection always requires a reason
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

        // Step 4: Apply the status change
        // Preserve the current status as previousStatus for audit / undo displays
        application.setPreviousStatus(currentStatus);
        application.setStatus(newStatus);
        // Record exactly WHEN this status change happened (server time, not client time)
        application.setStatusUpdatedAt(Instant.now());

        // Set the rejection reason if provided (null for non-rejections)
        if (request.getReason() != null && !request.getReason().isBlank()) {
            application.setRejectionReason(request.getReason().trim());
        }

        // Assign the officer who made this change (for accountability + notification routing)
        if (request.getOfficerId() != null && !request.getOfficerId().isBlank()) {
            application.setAssignedOfficerId(request.getOfficerId().trim());
        }

        // Step 5: Persist the updated document to MongoDB
        // save() here performs an UPDATE because the application already has an _id
        LoanApplication updated = loanApplicationRepository.save(application);

        // ── EXIT LOG — confirms the transition succeeded ───────────────────────
        log.info("[LoanApplicationService] UPDATE STATUS success | applicationId={} | FROM={} → TO={} | applicantId={} | officerId={}",
                 applicationId, currentStatus, newStatus,
                 updated.getApplicantId(), request.getOfficerId());

        // Step 6: Build and return the response DTO
        // We do NOT perform the applicant join here — the status update response
        // only needs the loan application fields (reduces one unnecessary DB query).
        return loanApplicationMapper.toResponse(updated);
    }
}
