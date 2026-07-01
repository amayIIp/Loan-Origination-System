package com.los.backend.controller;

import com.los.backend.dto.request.SubmitLoanApplicationRequest;
import com.los.backend.dto.request.UpdateLoanStatusRequest;
import com.los.backend.dto.response.LoanApplicationResponse;
import com.los.backend.model.enums.LoanStatus;
import com.los.backend.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * LoanApplicationController — HTTP boundary for all loan application endpoints.
 *
 * Endpoints:
 *   POST   /api/applications              → submit a new loan application
 *   GET    /api/applications              → paginated + filtered list
 *   GET    /api/applications/{id}         → single application detail (with applicant join)
 *   PATCH  /api/applications/{id}/status  → update application status (state machine)
 *
 * This controller is intentionally thin:
 *   - Parse HTTP input (path vars, query params, request body)
 *   - Call exactly one service method
 *   - Return the result with the correct HTTP status code
 *   - No if/else business logic here — all decisions are in the service layer
 */
@RestController
@RequestMapping("/applications")
@Slf4j
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    // ══════════════════════════════════════════════════════════════════════
    // POST /api/applications — Submit a new loan application
    // ══════════════════════════════════════════════════════════════════════

    /**
     * submitApplication — creates a new loan application in SUBMITTED status.
     *
     * HTTP semantics:
     *   Method: POST — creating a new resource
     *   Success: 201 Created with Location header pointing to GET /api/applications/{id}
     *   Errors:
     *     400 — validation failed (missing/invalid fields)
     *     404 — applicantId references a non-existent applicant
     *     422 — active application already exists for this applicant
     *
     * Why include a Location header on 201?
     * ───────────────────────────────────────
     * REST standard (RFC 7231) says: a 201 response SHOULD include a Location header
     * that identifies the newly created resource. This lets clients:
     * a) Confirm where the resource was created
     * b) Navigate to the detail view without a separate GET-all call
     * c) Implement "follow redirects on create" patterns in client code
     *
     * @param request the validated loan application submission (JSON body)
     * @return 201 Created + Location: /api/applications/{newId} + body
     */
    @PostMapping
    public ResponseEntity<LoanApplicationResponse> submitApplication(
            @Valid @RequestBody SubmitLoanApplicationRequest request) {

        LoanApplicationResponse response = loanApplicationService.submitApplication(request);

        // Build Location header: current base URL + /{id}
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET /api/applications — Paginated + filtered list of applications
    // ══════════════════════════════════════════════════════════════════════

    /**
     * listApplications — returns a paginated list of loan applications.
     *
     * Query parameters (all optional):
     *   status  — filter by LoanStatus enum value (e.g., ?status=UNDER_REVIEW)
     *   page    — page number, 0-indexed (default: 0)
     *   size    — items per page (default: 20, max: 100)
     *   sortBy  — field to sort by (default: createdAt descending)
     *
     * @RequestParam with defaultValue — provides sensible defaults when the
     * client doesn't specify the parameter. The method is still called even if
     * no query params are provided — it just uses the defaults.
     *
     * @RequestParam(required = false) for status — makes it nullable so the
     * service can detect "no filter" and query all statuses.
     *
     * HTTP semantics:
     *   Method: GET — pure read, no side effects
     *   Success: 200 OK with PagedResponse body containing items + pagination metadata
     *
     * @param status optional filter for LoanStatus (null = all statuses)
     * @param page   0-indexed page number (default 0)
     * @param size   items per page (default 20)
     * @param sortBy sort field name (default "createdAt")
     * @return 200 OK with paged response body
     */
    @GetMapping
    public ResponseEntity<LoanApplicationResponse.PagedResponse> listApplications(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        return ResponseEntity.ok(
            loanApplicationService.listApplications(status, page, size, sortBy)
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET /api/applications/{id} — Single application detail with manual join
    // ══════════════════════════════════════════════════════════════════════

    /**
     * getApplication — fetches one loan application with full applicant details.
     *
     * The service performs a two-query "manual join":
     *   1. Load LoanApplication by id
     *   2. Load the related Applicant by applicantId
     *   3. Merge into one enriched response DTO
     *
     * This is the detail view — returns applicantSummary populated.
     * The list endpoint (above) does NOT load applicant details.
     *
     * HTTP semantics:
     *   Method: GET
     *   Success: 200 OK with full LoanApplicationResponse (applicantSummary populated)
     *   Errors: 404 if applicationId or the related applicantId don't exist
     *
     * @param id the MongoDB _id of the loan application
     * @return 200 OK with enriched response body
     */
    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponse> getApplication(@PathVariable String id) {
        return ResponseEntity.ok(loanApplicationService.getApplicationById(id));
    }

    // ══════════════════════════════════════════════════════════════════════
    // PATCH /api/applications/{id}/status — Update application status
    // ══════════════════════════════════════════════════════════════════════

    /**
     * updateApplicationStatus — transitions a loan application to a new status.
     *
     * PATCH (not PUT) because we're updating ONE specific field (status),
     * not replacing the entire resource. Using PUT here would require the client
     * to send the complete application body — wasteful and error-prone.
     *
     * HTTP semantics:
     *   Method: PATCH — partial update of the resource
     *   Success: 200 OK with the updated LoanApplicationResponse
     *   Errors:
     *     404 — applicationId doesn't exist
     *     422 — the requested status transition is not legal (state machine validation)
     *     422 — REJECTED without a reason
     *     400 — validation failed (newStatus not provided)
     *
     * Why return the updated resource on PATCH?
     * ───────────────────────────────────────────
     * REST allows either 200 (with updated body) or 204 (no content) for successful PATCH.
     * We return 200 + body because the client needs the updatedAt timestamp and other
     * server-computed fields that changed as a result of the PATCH.
     * Returning 204 forces the client to make a separate GET call — unnecessary round-trip.
     *
     * @param id      the application's MongoDB _id from the URL path
     * @param request the status update request (newStatus, reason, officerId)
     * @return 200 OK with the updated LoanApplicationResponse
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<LoanApplicationResponse> updateApplicationStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateLoanStatusRequest request) {

        return ResponseEntity.ok(
            loanApplicationService.updateApplicationStatus(id, request)
        );
    }
}
