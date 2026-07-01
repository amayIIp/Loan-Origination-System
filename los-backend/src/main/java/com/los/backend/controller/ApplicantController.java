package com.los.backend.controller;

import com.los.backend.dto.request.AttachKycDocumentRequest;
import com.los.backend.dto.request.CreateApplicantRequest;
import com.los.backend.dto.response.ApplicantResponse;
import com.los.backend.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * ApplicantController — the HTTP boundary layer for all applicant-related endpoints.
 *
 * What does a Controller do? (beginner explanation)
 * ──────────────────────────────────────────────────
 * A controller is the entry point for an incoming HTTP request. Its ONLY jobs are:
 *   1. Parse the HTTP request (path variables, query params, request body)
 *   2. Call the appropriate service method with clean parameters
 *   3. Wrap the result in an HTTP response with the correct status code
 *
 * Controllers must contain NO business logic. All decisions about "is this valid?",
 * "does this resource exist?", "is this allowed?" live in the service layer.
 * This strict separation makes both layers easy to test independently.
 *
 * @RestController — declares this as a REST controller (returns JSON, not HTML views)
 * @RequestMapping — all paths in this controller are prefixed with /applicants
 *   Combined with the context-path /api in application.yml → /api/applicants
 */
@RestController
@RequestMapping("/applicants")
@Slf4j
public class ApplicantController {

    // Service is injected via constructor — the controller delegates all logic to it
    private final ApplicantService applicantService;

    public ApplicantController(ApplicantService applicantService) {
        this.applicantService = applicantService;
    }

    // ══════════════════════════════════════════════════════════════════════
    // POST /api/applicants — Create a new applicant
    // ══════════════════════════════════════════════════════════════════════

    /**
     * createApplicant — registers a new loan applicant in the system.
     *
     * HTTP semantics:
     *   Method: POST — we are CREATING a new resource
     *   Success: 201 Created — not 200 OK, because a new resource was created.
     *            HTTP 201 MUST include a Location header pointing to the new resource.
     *            This is the REST standard — clients can bookmark the new resource URL.
     *   Body: the created ApplicantResponse with masked PAN and generated id.
     *
     * @Valid — triggers Spring's Bean Validation on the request body BEFORE the
     *          method body runs. If any @NotBlank, @Email, etc. constraint fails,
     *          Spring throws MethodArgumentNotValidException → GlobalExceptionHandler
     *          returns 400 with field-level error details. The method is never called.
     *
     * @param request the validated request body deserialized from JSON
     * @return ResponseEntity with 201 status, Location header, and applicant body
     */
    @PostMapping
    public ResponseEntity<ApplicantResponse> createApplicant(
            @Valid @RequestBody CreateApplicantRequest request) {

        // Delegate all business logic to the service — controller stays thin
        ApplicantResponse response = applicantService.createApplicant(request);

        // Build the Location header URL: /api/applicants/{newId}
        // ServletUriComponentsBuilder automatically picks up the current request's
        // base URL, so this works correctly behind any proxy or load balancer.
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()           // starts with the current /api/applicants URL
            .path("/{id}")                  // appends /{id} path template
            .buildAndExpand(response.getId()) // replaces {id} with the actual generated id
            .toUri();

        // 201 Created + Location header + body
        return ResponseEntity.created(location).body(response);
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET /api/applicants/{id} — Fetch a single applicant
    // ══════════════════════════════════════════════════════════════════════

    /**
     * getApplicant — retrieves one applicant's full profile by their MongoDB id.
     *
     * HTTP semantics:
     *   Method: GET — reading a resource, no side effects
     *   Success: 200 OK
     *   Not found: 404 (thrown by service → handled by GlobalExceptionHandler)
     *
     * @PathVariable — extracts the {id} segment from the URL path.
     *   Example: GET /api/applicants/abc123 → id = "abc123"
     *
     * @param id the MongoDB ObjectId of the applicant to retrieve
     * @return ResponseEntity with 200 and ApplicantResponse body
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantResponse> getApplicant(@PathVariable String id) {
        return ResponseEntity.ok(applicantService.getApplicantById(id));
    }

    // ══════════════════════════════════════════════════════════════════════
    // POST /api/applicants/{id}/kyc — Attach a KYC document reference
    // ══════════════════════════════════════════════════════════════════════

    /**
     * attachKycDocument — registers a KYC document metadata record for an applicant.
     *
     * The file itself is already stored in cloud storage before this call.
     * This endpoint only stores the metadata (type, URL) in MongoDB.
     *
     * HTTP semantics:
     *   Method: POST — creating a new sub-resource (a new document in the kyc array)
     *   Success: 200 OK — we return the full updated applicant (not 201, because
     *            we're adding to an existing resource, not creating a top-level resource)
     *   Not found: 404 if the applicant with {id} doesn't exist
     *
     * @param id      the applicant's MongoDB id from the URL path
     * @param request the KYC document metadata from the request body
     * @return ResponseEntity with 200 and the updated ApplicantResponse
     */
    @PostMapping("/{id}/kyc")
    public ResponseEntity<ApplicantResponse> attachKycDocument(
            @PathVariable String id,
            @Valid @RequestBody AttachKycDocumentRequest request) {

        return ResponseEntity.ok(applicantService.attachKycDocument(id, request));
    }
}
