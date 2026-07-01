package com.los.backend.service;

import com.los.backend.dto.request.AttachKycDocumentRequest;
import com.los.backend.dto.request.CreateApplicantRequest;
import com.los.backend.dto.response.ApplicantResponse;
import com.los.backend.exception.DuplicateResourceException;
import com.los.backend.exception.ResourceNotFoundException;
import com.los.backend.mapper.ApplicantMapper;
import com.los.backend.model.Applicant;
import com.los.backend.model.KycDocument;
import com.los.backend.repository.ApplicantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

/**
 * ApplicantService — business logic layer for all applicant-related operations.
 *
 * What is a Service layer? (beginner explanation)
 * ────────────────────────────────────────────────
 * The service layer sits between the controller (HTTP boundary) and the
 * repository (database boundary). It is responsible for:
 * 1. Business rule enforcement (e.g., "you cannot register the same email twice")
 * 2. Orchestrating multiple repository calls when needed
 * 3. Data transformation (calling mappers to convert models ↔ DTOs)
 *
 * Controllers should be thin — they only parse HTTP input, call a service method,
 * and return an HTTP response. Heavy logic belongs here, not in controllers.
 *
 * @Slf4j — Lombok generates a "log" field (SLF4J Logger) for structured logging.
 * We log at entry/exit of important methods to support observability —
 * being able to trace a request's path through logs when debugging production issues.
 */
@Service  // Marks this as a Spring-managed service component (singleton bean)
@Slf4j    // Provides: log.info(), log.warn(), log.error(), log.debug()
public class ApplicantService {

    // Repository for MongoDB read/write on the "applicants" collection
    private final ApplicantRepository applicantRepository;

    // Mapper for converting between model ↔ request/response DTOs
    private final ApplicantMapper applicantMapper;

    /**
     * Constructor injection — Spring provides both dependencies automatically.
     * We declare them final so they cannot be accidentally reassigned after injection.
     */
    public ApplicantService(ApplicantRepository applicantRepository,
                            ApplicantMapper applicantMapper) {
        this.applicantRepository = applicantRepository;
        this.applicantMapper     = applicantMapper;
    }

    // ════════════════════════════════════════════════════════════════════════
    // CREATE APPLICANT — POST /api/applicants
    // ════════════════════════════════════════════════════════════════════════

    /**
     * createApplicant — registers a new applicant in the LOS system.
     *
     * Business rules enforced here (not in the controller):
     * 1. No two applicants can share the same email address.
     * 2. No two applicants can share the same PAN number.
     *
     * Why check both separately (not with existsByEmailOrPanNumber)?
     * If both are duplicates, the client needs to know WHICH one is the conflict.
     * Separate checks produce better error messages than a combined boolean check.
     *
     * Logging strategy:
     * - Entry log at INFO level: records the attempt with email (no PAN in logs — PII)
     * - Success log at INFO level: records the new applicant's generated ID
     * - Duplicate log at WARN level: flags potential re-registration or fraud attempt
     *
     * @param request the validated CreateApplicantRequest from the controller
     * @return ApplicantResponse DTO with the newly saved applicant's data
     */
    public ApplicantResponse createApplicant(CreateApplicantRequest request) {
        // ── Structured entry log ──────────────────────────────────────────────
        // Log the attempt without PAN (sensitive). email is included because:
        // a) It identifies the user non-invasively for support lookups
        // b) It's already in the request and visible in HTTP access logs anyway
        log.info("[ApplicantService] CREATE APPLICANT attempt | email={}", request.getEmail());

        // ── Business Rule 1: Email uniqueness ─────────────────────────────────
        // Check the database BEFORE attempting the insert. This gives a
        // user-friendly error ("email already exists") rather than letting the
        // MongoDB unique index violation surface as a confusing 500 error.
        if (applicantRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            log.warn("[ApplicantService] Duplicate email detected | email={}", request.getEmail());
            throw new DuplicateResourceException("Applicant", "email", request.getEmail());
        }

        // ── Business Rule 2: PAN uniqueness ──────────────────────────────────
        // PAN uniqueness prevents identity fraud: one real person trying to create
        // multiple applicant profiles to apply for multiple loans simultaneously.
        if (applicantRepository.existsByPanNumber(request.getPanNumber().toUpperCase().trim())) {
            // We do NOT log the PAN itself — it's sensitive PII.
            // The exception message will reach the client; the log stays safe.
            log.warn("[ApplicantService] Duplicate PAN detected | email={}", request.getEmail());
            throw new DuplicateResourceException("Applicant", "panNumber",
                "***MASKED*** (PAN already registered)");
        }

        // ── Map DTO → Model ───────────────────────────────────────────────────
        // The mapper converts the request into a domain model. At this point:
        // - id is null (MongoDB will generate it on save)
        // - createdAt/updatedAt are null (Spring Data auditing sets them on save)
        // - kycDocuments is an empty list (uploaded separately via POST /kyc)
        Applicant applicant = applicantMapper.toModel(request);

        // ── Persist to MongoDB ────────────────────────────────────────────────
        // repository.save() performs an INSERT (since id is null).
        // After save(), the returned savedApplicant has id, createdAt, updatedAt populated.
        Applicant savedApplicant = applicantRepository.save(applicant);

        // ── Success log ───────────────────────────────────────────────────────
        log.info("[ApplicantService] CREATE APPLICANT success | applicantId={} | email={}",
                 savedApplicant.getId(), savedApplicant.getEmail());

        // ── Map Model → Response DTO and return ───────────────────────────────
        // The mapper applies PAN masking before building the response.
        // The raw PAN is in MongoDB but never in the HTTP response body.
        return applicantMapper.toResponse(savedApplicant);
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET APPLICANT BY ID — used by LoanApplicationService for manual join
    // ════════════════════════════════════════════════════════════════════════

    /**
     * getApplicantById — loads one applicant by MongoDB _id.
     *
     * Used internally by LoanApplicationService.getApplicationById() to perform
     * the "manual join" (load LoanApplication → extract applicantId → load Applicant).
     * Also exposed directly for GET /api/applicants/{id} if that endpoint is added later.
     *
     * Optional.orElseThrow() is the idiomatic Java way to handle "not found":
     * it returns the value inside the Optional if present, or executes the
     * supplier lambda and throws the exception if empty. Clean, no null checks needed.
     *
     * @param applicantId the MongoDB _id of the applicant to load
     * @return ApplicantResponse DTO with masked PAN
     * @throws ResourceNotFoundException if no Applicant with this id exists
     */
    public ApplicantResponse getApplicantById(String applicantId) {
        log.debug("[ApplicantService] GET APPLICANT | applicantId={}", applicantId);

        Applicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", "id", applicantId));

        return applicantMapper.toResponse(applicant);
    }

    /**
     * getApplicantModelById — loads the raw Applicant model (not DTO) by ID.
     *
     * Called by LoanApplicationService when it needs the Applicant model
     * (not the DTO) to build the ApplicantSummary via the mapper.
     * Package-internal use — the controller should use getApplicantById() instead.
     *
     * @param applicantId the MongoDB _id of the applicant
     * @return the raw Applicant model from MongoDB
     * @throws ResourceNotFoundException if not found
     */
    public Applicant getApplicantModelById(String applicantId) {
        return applicantRepository.findById(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", "id", applicantId));
    }

    // ════════════════════════════════════════════════════════════════════════
    // ATTACH KYC DOCUMENT — POST /api/applicants/{id}/kyc
    // ════════════════════════════════════════════════════════════════════════

    /**
     * attachKycDocument — adds a KYC document metadata record to an applicant's
     * kycDocuments array in MongoDB.
     *
     * Important: This does NOT upload the file. The file must already be in
     * cloud storage and the caller provides the URL. This method only registers
     * the metadata (type, URL, timestamps) in the applicant's embedded array.
     *
     * Business rules enforced:
     * 1. The applicant must exist (throws 404 if not).
     * 2. We allow multiple documents of the same type (e.g., two ID_PROOF submissions
     *    when the first was rejected and the applicant re-uploads). The loan officer
     *    chooses which one to verify.
     *
     * MongoDB update strategy:
     * We load the full Applicant document, add the new KycDocument to the list,
     * then call save() to update the entire document. This is the simplest approach.
     * For very high concurrency, we'd use MongoTemplate with $push operator to
     * atomically append to the array without loading the full document — but for
     * Phase 1, the load-modify-save pattern is simpler and safe enough.
     *
     * @param applicantId the MongoDB _id of the applicant
     * @param request     the KYC document metadata (type, URL, description)
     * @return updated ApplicantResponse with the new KYC document in the list
     */
    public ApplicantResponse attachKycDocument(String applicantId, AttachKycDocumentRequest request) {
        log.info("[ApplicantService] ATTACH KYC DOCUMENT | applicantId={} | docType={}",
                 applicantId, request.getDocumentType());

        // Load the applicant — throws 404 if not found
        Applicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", "id", applicantId));

        // Build the new KycDocument embedded object from the request DTO
        KycDocument newDoc = KycDocument.builder()
            .documentType(request.getDocumentType())
            .fileUrl(request.getFileUrl())
            .uploadedAt(Instant.now())   // Record the server-side upload timestamp
            // All new documents start as PENDING — a loan officer must verify them
            // (VerificationStatus.PENDING is the @Builder.Default so this is automatic,
            //  but we set it explicitly for clarity in business logic)
            .rejectionReason(null)       // Not rejected yet — just uploaded
            .verifiedAt(null)            // Not yet reviewed
            .build();

        // Defensively initialise the list if it's somehow null (shouldn't be due to
        // @Builder.Default, but protects against documents loaded before the default was added)
        if (applicant.getKycDocuments() == null) {
            applicant.setKycDocuments(new ArrayList<>());
        }

        // Add the new document to the embedded array
        applicant.getKycDocuments().add(newDoc);

        // Persist the updated applicant document to MongoDB
        // save() here performs an UPDATE (upsert by _id) since id is already set
        Applicant updated = applicantRepository.save(applicant);

        log.info("[ApplicantService] ATTACH KYC DOCUMENT success | applicantId={} | docType={} | totalDocs={}",
                 applicantId, request.getDocumentType(), updated.getKycDocuments().size());

        return applicantMapper.toResponse(updated);
    }

    /**
     * existsById — checks if an Applicant with the given id exists.
     * Used by LoanApplicationService before creating a loan application.
     * Cheaper than loading the full document just to check existence.
     *
     * @param applicantId the MongoDB _id to check
     * @return true if the applicant exists, false otherwise
     */
    public boolean existsById(String applicantId) {
        return applicantRepository.existsById(applicantId);
    }
}
