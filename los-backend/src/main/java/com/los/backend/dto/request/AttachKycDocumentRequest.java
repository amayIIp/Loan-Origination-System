package com.los.backend.dto.request;

import com.los.backend.model.enums.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * AttachKycDocumentRequest — request body DTO for POST /api/applicants/{id}/kyc.
 *
 * IMPORTANT DESIGN DECISION: This endpoint does NOT accept the file binary itself.
 * The file has already been uploaded to cloud storage (AWS S3, GCS, Azure Blob)
 * by a separate file upload endpoint or a direct-to-storage presigned URL flow.
 * This endpoint ONLY registers the metadata (type + URL) in the Applicant's
 * kycDocuments array in MongoDB.
 *
 * Why this separation?
 * ─────────────────────
 * 1. Files can be large (PDFs, images). Routing them through Spring Boot adds
 *    memory pressure and latency. Cloud storage handles large binary uploads far better.
 * 2. Spring Boot does not need to touch the binary — it only needs the storage URL.
 * 3. This pattern enables presigned URL flows where the client uploads directly to
 *    S3 and then calls this endpoint to register the result — no proxy overhead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachKycDocumentRequest {

    /**
     * documentType — what kind of document this is.
     * @NotNull — we must know what type before storing; cannot process a KYC
     *            document without knowing whether it's an ID, address, or income proof.
     */
    @NotNull(message = "Document type is required (e.g., ID_PROOF, ADDRESS_PROOF, INCOME_PROOF)")
    private KycDocumentType documentType;

    /**
     * fileUrl — the publicly accessible (or signed) URL where the file is stored.
     * @NotBlank + @URL — validates it looks like a real URL (has http/https scheme).
     * In production, additionally verify the URL is in your trusted storage domain
     * (e.g., starts with "https://your-bucket.s3.amazonaws.com/") to prevent
     * URL injection attacks where a bad actor registers an external URL.
     */
    @NotBlank(message = "File URL is required — upload the file first, then register the URL here")
    @URL(message = "File URL must be a valid URL (must start with http:// or https://)")
    private String fileUrl;

    /**
     * originalFileName — the original filename as uploaded by the user.
     * Stored for display purposes only ("pan_card_front.pdf").
     * Not used for storage or retrieval — the fileUrl is the authoritative reference.
     */
    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    private String originalFileName;

    /**
     * description — optional note about this document.
     * Example: "Front side of Aadhaar card", "Salary slip for March 2024"
     * Helps loan officers understand exactly what they're looking at.
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
