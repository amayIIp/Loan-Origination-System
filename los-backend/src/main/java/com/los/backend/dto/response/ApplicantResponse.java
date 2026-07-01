package com.los.backend.dto.response;

import com.los.backend.model.enums.EmploymentType;
import com.los.backend.model.enums.KycDocumentType;
import com.los.backend.model.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * ApplicantResponse — the response DTO returned by applicant-related endpoints.
 *
 * Why a response DTO instead of returning the Applicant model directly?
 * ───────────────────────────────────────────────────────────────────────
 * 1. PAN masking: The real PAN number is sensitive PII. We expose only the
 *    last 4 characters (e.g., "XXXXX1234A") in responses. The model holds
 *    the full PAN — the mapper applies masking before building this DTO.
 * 2. Field selection: Response DTOs include only the fields the client actually
 *    needs — fields like passwordHash, internal flags, etc. are excluded.
 * 3. Shape transformation: We may rename, restructure, or flatten fields in
 *    the response without changing the MongoDB document structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponse {

    /** MongoDB ObjectId — the applicant's unique identifier */
    private String id;

    private String firstName;
    private String lastName;

    /**
     * fullName — computed "FirstName LastName" for display convenience.
     * Saves the frontend from concatenating two fields.
     */
    private String fullName;

    private String email;
    private String phone;
    private LocalDate dateOfBirth;

    /**
     * maskedPanNumber — PAN with first 6 chars replaced by 'X' for security.
     * Example: "ABCDE1234F" → "XXXXXX234F"
     * Masking is applied in the ApplicantMapper, not in the model.
     */
    private String maskedPanNumber;

    /** Nested address details — safe to expose in full */
    private AddressResponse address;

    /** Nested employment/income details */
    private EmploymentInfoResponse employmentInfo;

    /** List of KYC document metadata (no file binary — just metadata + URL) */
    private List<KycDocumentResponse> kycDocuments;

    /** Whether this applicant profile is active (not soft-deleted) */
    private boolean isActive;

    /** When this applicant record was first created in the system */
    private Instant createdAt;

    /** When this applicant record was last updated */
    private Instant updatedAt;

    // ── Nested Response DTOs ──────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String pincode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentInfoResponse {
        private EmploymentType employmentType;
        private String employerName;
        /** Monthly income rounded to 2 decimal places for display */
        private BigDecimal monthlyIncome;
        private BigDecimal totalMonthlyEmi;
        /** Credit score — null if applicant is New-To-Credit */
        private Integer creditScore;
        private Integer yearsOfExperience;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycDocumentResponse {
        private KycDocumentType documentType;
        /** fileUrl is included so the frontend can render a "View Document" link */
        private String fileUrl;
        private Instant uploadedAt;
        private VerificationStatus verificationStatus;
        /** Rejection reason — only populated when verificationStatus = REJECTED */
        private String rejectionReason;
        private Instant verifiedAt;
    }
}
