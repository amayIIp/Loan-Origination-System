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

/**
 * LoanApplicationResponse — DTO returned by all loan-application endpoints.
 *
 * Two variants are used:
 * 1. Standard (list view): applicantDetails is null — just IDs for table rows.
 * 2. Detail (GET /{id}): applicantDetails is populated — the "manual join"
 *    performed in LoanApplicationService to enrich the response.
 *
 * This single class handles both by making applicantDetails nullable.
 * The mapper populates it only when the caller needs the full detail view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    /** MongoDB ObjectId — the loan application's unique identifier */
    private String id;

    /** Reference to the applicant — always present */
    private String applicantId;

    /**
     * applicantSummary — populated ONLY in the GET /{id} detail view.
     * Contains the full applicant name, email, credit score, etc.
     * Null in list views to keep pagination responses compact.
     *
     * This is the "manual join" pattern:
     *   Service loads LoanApplication → extracts applicantId →
     *   loads Applicant → builds ApplicantSummary → attaches to response.
     */
    private ApplicantSummary applicantSummary;

    // ── Loan Request Details ──────────────────────────────────────────────────

    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String purpose;
    private String loanProductType;
    private BigDecimal interestRatePercent;

    // ── Lifecycle Status ──────────────────────────────────────────────────────

    private LoanStatus status;
    private LoanStatus previousStatus;
    private Instant statusUpdatedAt;
    private String assignedOfficerId;

    // ── BRE Decision Summary ──────────────────────────────────────────────────

    /**
     * decisionResult — embedded summary of the BRE evaluation.
     * Null until the application has been through CREDIT_CHECK status.
     */
    private DecisionResultResponse decisionResult;

    // ── Rejection / Notes ─────────────────────────────────────────────────────

    /** Populated when status = REJECTED — explains why the application was rejected */
    private String rejectionReason;

    // ── Audit ─────────────────────────────────────────────────────────────────

    private Instant createdAt;
    private Instant updatedAt;

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    /**
     * ApplicantSummary — a lightweight view of the applicant's key details.
     * Populated in the detail endpoint (GET /applications/{id}) via manual join.
     * Includes credit score and income for the loan officer's one-page review view.
     */
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
        /** Number of verified KYC documents — quick eligibility indicator */
        private long verifiedKycDocumentCount;
        /** Number of pending or rejected KYC docs — flags for the officer */
        private long pendingOrRejectedKycCount;
    }

    /**
     * DecisionResultResponse — compact view of the BRE's credit decision.
     * Mirrors DecisionResult (embedded model) but as a response DTO.
     */
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
        /** Individual rule pass/fail details */
        private List<RuleOutcomeResponse> ruleOutcomes;
    }

    /**
     * RuleOutcomeResponse — one row in the rule-by-rule decision breakdown table.
     */
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

    // ── Paginated List Wrapper ────────────────────────────────────────────────

    /**
     * PagedResponse — wraps a paginated list of LoanApplicationResponse objects.
     * Returned by GET /api/applications with pagination metadata so the frontend
     * can render "Page 1 of 47 — Showing 1–20 of 930 results".
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse {
        /** The list of loan applications for the current page */
        private List<LoanApplicationResponse> content;
        /** Which page this is (0-indexed) */
        private int pageNumber;
        /** How many items per page was requested */
        private int pageSize;
        /** Total number of matching documents across ALL pages */
        private long totalElements;
        /** Total number of pages (ceil(totalElements / pageSize)) */
        private int totalPages;
        /** Whether there is a next page available */
        private boolean hasNext;
        /** Whether there is a previous page available */
        private boolean hasPrevious;
    }
}
