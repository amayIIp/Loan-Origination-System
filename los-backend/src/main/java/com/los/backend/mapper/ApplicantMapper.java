package com.los.backend.mapper;

import com.los.backend.dto.request.CreateApplicantRequest;
import com.los.backend.dto.response.ApplicantResponse;
import com.los.backend.model.Address;
import com.los.backend.model.Applicant;
import com.los.backend.model.EmploymentInfo;
import com.los.backend.model.KycDocument;
import com.los.backend.model.enums.VerificationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ApplicantMapper — converts between Applicant domain model, request DTOs,
 * and response DTOs. Centralises all mapping logic so it doesn't leak into
 * controllers (which should only handle HTTP concerns) or services (which
 * should only handle business logic).
 *
 * Why not use MapStruct or ModelMapper? (beginner explanation)
 * ─────────────────────────────────────────────────────────────
 * MapStruct is a code-generation library that creates mapping code at compile
 * time from annotated interfaces. It's excellent for large codebases but adds
 * complexity for teams unfamiliar with annotation processors.
 *
 * We use manual mapping here because:
 * 1. We need custom logic (PAN masking, KYC count calculation) that annotation
 *    based frameworks handle awkwardly with custom converters.
 * 2. Manual mapping is fully transparent — no "magic" that hides bugs.
 * 3. Easy to read, debug, and unit-test in isolation.
 * 4. Add MapStruct in Phase 5 if the mapping volume becomes burdensome.
 */
@Component
public class ApplicantMapper {

    // ── Request → Model ───────────────────────────────────────────────────────

    /**
     * toModel — converts a CreateApplicantRequest DTO (from the HTTP request body)
     * into an Applicant domain model ready to be saved to MongoDB.
     *
     * Fields like "id", "createdAt", "updatedAt", and "isActive" are NOT set here —
     * MongoDB generates "id", Spring Data auditing sets timestamps, and "isActive"
     * defaults to true via @Builder.Default. This is intentional: the client
     * must never control those server-managed fields.
     *
     * @param req the validated request DTO from the controller
     * @return a new Applicant model object (not yet saved to MongoDB)
     */
    public Applicant toModel(CreateApplicantRequest req) {
        // Build the nested Address sub-document from the nested AddressRequest DTO
        Address address = Address.builder()
            .line1(req.getAddress().getLine1())
            .line2(req.getAddress().getLine2())
            .city(req.getAddress().getCity())
            .state(req.getAddress().getState())
            .pincode(req.getAddress().getPincode())
            // Default "India" if client sends blank — the DTO validates non-blank so this
            // guard is defensive; country will always be set at this point.
            .country(req.getAddress().getCountry() != null ? req.getAddress().getCountry() : "India")
            .build();

        // Build the nested EmploymentInfo sub-document from EmploymentInfoRequest DTO
        CreateApplicantRequest.EmploymentInfoRequest empReq = req.getEmploymentInfo();
        EmploymentInfo employmentInfo = EmploymentInfo.builder()
            .employmentType(empReq.getEmploymentType())
            .employerName(empReq.getEmployerName())
            .monthlyIncome(empReq.getMonthlyIncome())
            // Default to zero if client didn't provide existing EMI obligations
            .totalMonthlyEmi(
                empReq.getTotalMonthlyEmi() != null
                    ? empReq.getTotalMonthlyEmi()
                    : BigDecimal.ZERO
            )
            .creditScore(empReq.getCreditScore())
            .yearsOfExperience(empReq.getYearsOfExperience())
            .build();

        // Build the root Applicant model — kycDocuments starts empty (uploaded separately)
        return Applicant.builder()
            .firstName(req.getFirstName().trim())      // Trim whitespace from names
            .lastName(req.getLastName().trim())
            .email(req.getEmail().toLowerCase().trim()) // Normalise email to lowercase
            .phone(req.getPhone().trim())
            .dateOfBirth(req.getDateOfBirth())
            .panNumber(req.getPanNumber().toUpperCase().trim()) // PAN is always uppercase
            .address(address)
            .employmentInfo(employmentInfo)
            // isActive defaults to true via @Builder.Default in the model
            // createdAt/updatedAt set by Spring Data auditing (@CreatedDate/@LastModifiedDate)
            .build();
    }

    // ── Model → Response ──────────────────────────────────────────────────────

    /**
     * toResponse — converts a saved Applicant model into an ApplicantResponse DTO
     * safe for exposing over the REST API.
     *
     * Key security transformation applied here: PAN masking.
     * The full PAN is stored in MongoDB but never returned raw in any API response.
     *
     * @param applicant the Applicant model loaded from MongoDB
     * @return ApplicantResponse DTO with sensitive fields masked
     */
    public ApplicantResponse toResponse(Applicant applicant) {
        return ApplicantResponse.builder()
            .id(applicant.getId())
            .firstName(applicant.getFirstName())
            .lastName(applicant.getLastName())
            .fullName(applicant.getFullName())   // computed helper method on the model
            .email(applicant.getEmail())
            .phone(applicant.getPhone())
            .dateOfBirth(applicant.getDateOfBirth())
            // Apply PAN masking — never expose full PAN in API responses
            .maskedPanNumber(maskPan(applicant.getPanNumber()))
            .address(mapAddress(applicant.getAddress()))
            .employmentInfo(mapEmploymentInfo(applicant.getEmploymentInfo()))
            .kycDocuments(mapKycDocuments(applicant.getKycDocuments()))
            .isActive(applicant.isActive())
            .createdAt(applicant.getCreatedAt())
            .updatedAt(applicant.getUpdatedAt())
            .build();
    }

    /**
     * toResponseList — convenience method to map a list of Applicants at once.
     * Uses Java streams to apply toResponse() to each element in the list.
     *
     * @param applicants list of Applicant models from a MongoDB query result
     * @return list of ApplicantResponse DTOs
     */
    public List<ApplicantResponse> toResponseList(List<Applicant> applicants) {
        if (applicants == null) return Collections.emptyList();
        // Stream the list, apply toResponse to each element, collect back to List
        return applicants.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // ── Private Mapping Helpers ───────────────────────────────────────────────

    /**
     * maskPan — replaces the first 6 characters of a 10-character PAN with 'X'.
     * Example: "ABCDE1234F" → "XXXXXX234F"
     *
     * Why 6 characters masked? — The last 4 chars (3 digits + 1 letter) are sufficient
     * for a user to verify "yes this is my PAN" without revealing the full number.
     * This matches the masking convention used by Indian banks and payment gateways.
     *
     * @param pan the full 10-character PAN number
     * @return masked PAN string, or "N/A" if pan is null (defensive)
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) return "N/A";
        // Replace first 6 characters with 'X', keep the last 4
        return "XXXXXX" + pan.substring(6);
    }

    /**
     * mapAddress — converts Address model to AddressResponse DTO.
     * All address fields are safe to expose without masking.
     *
     * @param address the Address sub-document from the Applicant model
     * @return AddressResponse DTO, or null if address is null (defensive)
     */
    private ApplicantResponse.AddressResponse mapAddress(Address address) {
        if (address == null) return null;
        return ApplicantResponse.AddressResponse.builder()
            .line1(address.getLine1())
            .line2(address.getLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .build();
    }

    /**
     * mapEmploymentInfo — converts EmploymentInfo model to EmploymentInfoResponse DTO.
     * Income and credit score are sensitive but necessary for loan officers —
     * they are included without masking in internal-facing responses.
     * If this API were applicant-facing, consider masking income digits too.
     *
     * @param info the EmploymentInfo sub-document from the Applicant model
     * @return EmploymentInfoResponse DTO, or null if info is null
     */
    private ApplicantResponse.EmploymentInfoResponse mapEmploymentInfo(EmploymentInfo info) {
        if (info == null) return null;
        return ApplicantResponse.EmploymentInfoResponse.builder()
            .employmentType(info.getEmploymentType())
            .employerName(info.getEmployerName())
            .monthlyIncome(info.getMonthlyIncome())
            .totalMonthlyEmi(info.getTotalMonthlyEmi())
            .creditScore(info.getCreditScore())
            .yearsOfExperience(info.getYearsOfExperience())
            .build();
    }

    /**
     * mapKycDocuments — converts a list of KycDocument sub-documents to DTOs.
     * The fileUrl is included so the frontend can render a "View" link,
     * but the actual binary file is served by cloud storage, not this API.
     *
     * @param docs the kycDocuments array from the Applicant model
     * @return list of KycDocumentResponse DTOs
     */
    private List<ApplicantResponse.KycDocumentResponse> mapKycDocuments(List<KycDocument> docs) {
        if (docs == null) return Collections.emptyList();
        return docs.stream()
            .map(doc -> ApplicantResponse.KycDocumentResponse.builder()
                .documentType(doc.getDocumentType())
                .fileUrl(doc.getFileUrl())
                .uploadedAt(doc.getUploadedAt())
                .verificationStatus(doc.getVerificationStatus())
                .rejectionReason(doc.getRejectionReason())
                .verifiedAt(doc.getVerifiedAt())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * buildApplicantSummary — creates a lightweight ApplicantSummary used inside
     * LoanApplicationResponse for the detail view endpoint (GET /applications/{id}).
     *
     * This is the "manual join" artifact: LoanApplication references Applicant by ID;
     * the service loads the Applicant separately and calls this method to build the
     * embedded summary that appears in the loan application's detail response.
     *
     * @param applicant the Applicant loaded from MongoDB by applicantId
     * @return ApplicantSummary embedded inside LoanApplicationResponse
     */
    public LoanApplicationResponse.ApplicantSummary buildApplicantSummary(Applicant applicant) {
        if (applicant == null) return null;

        // Count how many KYC documents are verified — quick eligibility indicator
        long verifiedCount = applicant.getKycDocuments() == null ? 0L :
            applicant.getKycDocuments().stream()
                .filter(d -> VerificationStatus.VERIFIED.equals(d.getVerificationStatus()))
                .count();

        // Count pending or rejected docs — flags for the officer to investigate
        long pendingOrRejectedCount = applicant.getKycDocuments() == null ? 0L :
            applicant.getKycDocuments().stream()
                .filter(d -> !VerificationStatus.VERIFIED.equals(d.getVerificationStatus()))
                .count();

        return LoanApplicationResponse.ApplicantSummary.builder()
            .id(applicant.getId())
            .fullName(applicant.getFullName())
            .email(applicant.getEmail())
            .phone(applicant.getPhone())
            .maskedPanNumber(maskPan(applicant.getPanNumber()))
            .creditScore(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getCreditScore() : null)
            .monthlyIncome(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getMonthlyIncome() : null)
            .totalMonthlyEmi(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getTotalMonthlyEmi() : BigDecimal.ZERO)
            .employmentType(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getEmploymentType().name() : null)
            .verifiedKycDocumentCount(verifiedCount)
            .pendingOrRejectedKycCount(pendingOrRejectedCount)
            .build();
    }
}

// We need this import for the method above:
// It references LoanApplicationResponse which is in the response package.
// The import is at the top — confirmed correct.
