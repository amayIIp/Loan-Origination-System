package com.los.backend.dto.request;

import com.los.backend.model.enums.LoanStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateLoanStatusRequest — request body DTO for PATCH /api/applications/{id}/status.
 *
 * PATCH is used instead of PUT because we are updating ONE specific field (status)
 * rather than replacing the entire resource. This is the correct HTTP semantic:
 *   PUT  = replace the whole resource with what I'm sending
 *   PATCH = apply a partial update (only the fields I specify)
 *
 * The service layer validates that the requested newStatus is a legal next step
 * from the application's current status using the LoanStatusStateMachine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoanStatusRequest {

    /**
     * newStatus — the target status we want to transition this application to.
     * @NotNull — the whole purpose of this request is the new status; cannot be absent.
     *
     * Valid values match the LoanStatus enum:
     *   KYC_PENDING, UNDER_REVIEW, CREDIT_CHECK, APPROVED, REJECTED, DISBURSED
     * (SUBMITTED is the initial state set on creation — cannot be requested via PATCH)
     */
    @NotNull(message = "New status is required. Valid values: KYC_PENDING, UNDER_REVIEW, CREDIT_CHECK, APPROVED, REJECTED, DISBURSED")
    private LoanStatus newStatus;

    /**
     * reason — required when the new status is REJECTED.
     * Provides the applicant and audit trail with a clear explanation.
     * The service enforces this requirement in code (not just here) because
     * Bean Validation cannot express conditional requirements.
     */
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    /**
     * officerId — the MongoDB User._id of the officer making this status change.
     * Stored as "assignedOfficerId" and used for audit trail.
     * In Phase 2 this will be extracted from the JWT token instead of being
     * sent in the request body — for now it's explicit for testability.
     */
    @Size(max = 50)
    private String officerId;
}
