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

/**
 * LoanApplicationMapper — converts between LoanApplication model, request DTOs,
 * and response DTOs.
 *
 * Manual join note:
 * ──────────────────
 * MongoDB does not perform relational-style JOINs natively. When the detail
 * endpoint (GET /applications/{id}) needs both the LoanApplication AND the
 * Applicant's details, the service layer:
 * 1. Loads the LoanApplication by id from "loan_applications" collection.
 * 2. Extracts applicantId from the loaded application.
 * 3. Loads the Applicant by that id from "applicants" collection.
 * 4. Calls this mapper's toDetailResponse(application, applicant) which merges
 *    both into a single LoanApplicationResponse with an embedded ApplicantSummary.
 *
 * This is the standard "two-step load" pattern for manual joins in Spring Data MongoDB.
 * It requires 2 database round-trips but gives us full control over what's joined,
 * avoids the complexity of $lookup aggregation pipelines for the simple detail view,
 * and is perfectly fast for single-document fetches.
 */
@Component
public class LoanApplicationMapper {

    // ApplicantMapper is needed to build the ApplicantSummary sub-object
    private final ApplicantMapper applicantMapper;

    /**
     * Constructor injection — Spring provides the ApplicantMapper bean.
     * Constructor injection (vs field injection) is preferred because:
     * - It makes dependencies explicit and visible
     * - Enables easy unit testing (pass mocks through the constructor)
     * - Works well with Spring's immutability best practices
     */
    public LoanApplicationMapper(ApplicantMapper applicantMapper) {
        this.applicantMapper = applicantMapper;
    }

    // ── Request → Model ───────────────────────────────────────────────────────

    /**
     * toModel — converts a SubmitLoanApplicationRequest DTO into a LoanApplication
     * model ready for first save to MongoDB.
     *
     * Initial status is always SUBMITTED — the state machine starts here.
     * decisionResult is null — populated only after BRE evaluation.
     * createdAt/updatedAt set by Spring Data auditing.
     *
     * @param req the validated submission request from the controller
     * @return new LoanApplication model (not yet persisted)
     */
    public LoanApplication toModel(SubmitLoanApplicationRequest req) {
        return LoanApplication.builder()
            .applicantId(req.getApplicantId())
            .loanAmount(req.getLoanAmount())
            .tenureMonths(req.getTenureMonths())
            .purpose(req.getPurpose().trim())
            // Default product type if not provided — most applications are personal loans
            .loanProductType(
                req.getLoanProductType() != null && !req.getLoanProductType().isBlank()
                    ? req.getLoanProductType().toUpperCase()
                    : "PERSONAL_LOAN"
            )
            // Every new application starts in SUBMITTED state — state machine begins here
            .status(LoanStatus.SUBMITTED)
            // No previous status on a brand-new application
            .previousStatus(null)
            // Record when this status was set (SUBMITTED = right now)
            .statusUpdatedAt(Instant.now())
            // decisionResult is null — BRE hasn't run yet
            .decisionResult(null)
            .build();
    }

    // ── Model → Response (List view — no Applicant join) ─────────────────────

    /**
     * toResponse — converts a LoanApplication model into a response DTO suitable
     * for list views (paginated GET /api/applications).
     *
     * In list view, applicantSummary is NOT populated — loading the full Applicant
     * document for EVERY row in a paginated list of 20 applications would be
     * 20 extra MongoDB queries. For lists, we only return applicantId (the reference).
     * The frontend can fetch details on demand when the user clicks a row.
     *
     * @param app the LoanApplication model from MongoDB
     * @return LoanApplicationResponse DTO (applicantSummary = null)
     */
    public LoanApplicationResponse toResponse(LoanApplication app) {
        return buildResponse(app, null);
    }

    /**
     * toDetailResponse — converts a LoanApplication model WITH a pre-loaded Applicant
     * into a fully enriched response DTO for the detail view (GET /applications/{id}).
     *
     * The Applicant has already been loaded by the service layer (the manual join step).
     * This method just merges the two models into one response object.
     *
     * @param app       the LoanApplication model from MongoDB
     * @param applicant the related Applicant model loaded by the service layer
     * @return fully enriched LoanApplicationResponse DTO with applicantSummary populated
     */
    public LoanApplicationResponse toDetailResponse(LoanApplication app, Applicant applicant) {
        // Build the applicant summary using the shared ApplicantMapper helper
        LoanApplicationResponse.ApplicantSummary summary =
            applicantMapper.buildApplicantSummary(applicant);
        return buildResponse(app, summary);
    }

    /**
     * toResponseList — maps a list of LoanApplication models to response DTOs.
     * Used for paginated list results. applicantSummary is null for each item (list view).
     *
     * @param apps list of LoanApplication models from a paginated repository query
     * @return list of LoanApplicationResponse DTOs
     */
    public List<LoanApplicationResponse> toResponseList(List<LoanApplication> apps) {
        if (apps == null) return Collections.emptyList();
        return apps.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // ── Private Core Builder ──────────────────────────────────────────────────

    /**
     * buildResponse — the core mapping logic shared between toResponse() and
     * toDetailResponse(). Extracts all scalar fields, maps the embedded
     * DecisionResult sub-document, and attaches the (possibly null) applicantSummary.
     *
     * @param app     the LoanApplication model
     * @param summary pre-built ApplicantSummary (null for list view)
     * @return complete LoanApplicationResponse
     */
    private LoanApplicationResponse buildResponse(LoanApplication app,
                                                   LoanApplicationResponse.ApplicantSummary summary) {
        return LoanApplicationResponse.builder()
            .id(app.getId())
            .applicantId(app.getApplicantId())
            .applicantSummary(summary)           // null for list view, populated for detail
            .loanAmount(app.getLoanAmount())
            .tenureMonths(app.getTenureMonths())
            .purpose(app.getPurpose())
            .loanProductType(app.getLoanProductType())
            .interestRatePercent(app.getInterestRatePercent())
            .status(app.getStatus())
            .previousStatus(app.getPreviousStatus())
            .statusUpdatedAt(app.getStatusUpdatedAt())
            .assignedOfficerId(app.getAssignedOfficerId())
            // Map the embedded DecisionResult sub-document (or null if BRE hasn't run)
            .decisionResult(mapDecisionResult(app.getDecisionResult()))
            .rejectionReason(app.getRejectionReason())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt())
            .build();
    }

    /**
     * mapDecisionResult — converts the embedded DecisionResult model sub-document
     * into a DecisionResultResponse DTO. Returns null if the BRE hasn't run yet.
     *
     * @param dr the DecisionResult embedded in the LoanApplication (may be null)
     * @return DecisionResultResponse DTO, or null if dr is null
     */
    private LoanApplicationResponse.DecisionResultResponse mapDecisionResult(DecisionResult dr) {
        if (dr == null) return null;

        // Map the list of individual rule outcomes (may be null if BRE didn't record them)
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
