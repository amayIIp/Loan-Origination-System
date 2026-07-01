package com.los.backend.dto.request;

import com.los.backend.model.enums.EmploymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CreateApplicantRequest — the request body DTO for POST /api/applicants.
 *
 * Why a DTO instead of accepting the Applicant model directly?
 * ─────────────────────────────────────────────────────────────
 * 1. SECURITY: The Applicant model has fields like "id", "createdAt", "isActive"
 *    that must NEVER come from the client — they are server-set. Accepting the
 *    model directly would let a malicious client set their own ID or bypass
 *    soft-delete by setting isActive=false on creation.
 * 2. DECOUPLING: The API contract (what fields the client sends) is separate from
 *    the storage model (how data is stored in MongoDB). Changing the database
 *    schema doesn't break the API contract, and vice versa.
 * 3. VALIDATION: DTO validation rules can differ from model constraints.
 *    The DTO validates what the CLIENT sends; the model validates what gets stored.
 *
 * Bean Validation (beginner explanation):
 * ────────────────────────────────────────
 * Annotations like @NotBlank, @Email, @Min are processed by Spring's validation
 * framework when we annotate the controller parameter with @Valid. If any constraint
 * fails, Spring throws MethodArgumentNotValidException BEFORE the service method
 * runs — our GlobalExceptionHandler catches it and returns a 400 with field details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicantRequest {

    // ── Personal Info ─────────────────────────────────────────────────────────

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be 2–100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be 1–100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address (e.g., name@domain.com)")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be exactly 10 digits (no spaces or dashes)")
    private String phone;

    /**
     * dateOfBirth — must be a date in the past.
     * @Past ensures the client can't submit a future date or today's date
     * (you can't have been born today and apply for a loan simultaneously).
     */
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    /**
     * panNumber — 10-character Indian PAN in format AAAAA9999A.
     * Regex: 5 uppercase letters, 4 digits, 1 uppercase letter.
     */
    @NotBlank(message = "PAN number is required")
    @Pattern(
        regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}",
        message = "PAN must be in format AAAAA9999A (5 uppercase letters, 4 digits, 1 uppercase letter)"
    )
    private String panNumber;

    // ── Address (nested DTO, validated recursively via @Valid) ─────────────────

    @NotNull(message = "Address is required")
    @Valid
    private AddressRequest address;

    // ── Employment Info (nested DTO) ──────────────────────────────────────────

    @NotNull(message = "Employment information is required")
    @Valid
    private EmploymentInfoRequest employmentInfo;

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    /**
     * AddressRequest — nested DTO for the applicant's residential address.
     * Declared as a static inner class because it is only used within this request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressRequest {

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 must not exceed 200 characters")
        private String line1;

        @Size(max = 200, message = "Address line 2 must not exceed 200 characters")
        private String line2;   // Optional — many addresses don't need a second line

        @NotBlank(message = "City is required")
        @Size(max = 100)
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 100)
        private String state;

        @NotBlank(message = "PIN code is required")
        @Pattern(regexp = "\\d{6}", message = "PIN code must be exactly 6 digits")
        private String pincode;

        @NotBlank(message = "Country is required")
        private String country;
    }

    /**
     * EmploymentInfoRequest — nested DTO for the applicant's employment details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentInfoRequest {

        @NotNull(message = "Employment type is required")
        private EmploymentType employmentType;

        @Size(max = 200, message = "Employer name must not exceed 200 characters")
        private String employerName;    // Optional for SELF_EMPLOYED

        /**
         * monthlyIncome — gross monthly income in INR.
         * @DecimalMin — must be at least ₹1 (0 income = ineligible for any loan product,
         *               but we allow the submission; the BRE rejects it during evaluation).
         */
        @NotNull(message = "Monthly income is required")
        @DecimalMin(value = "1.00", message = "Monthly income must be greater than ₹0")
        @Digits(integer = 12, fraction = 2, message = "Monthly income must be a valid monetary amount")
        private BigDecimal monthlyIncome;

        /**
         * totalMonthlyEmi — sum of existing loan EMI obligations.
         * Optional — defaults to 0 if not provided (no existing debts assumed).
         */
        @DecimalMin(value = "0.00", message = "Total EMI cannot be negative")
        @Digits(integer = 12, fraction = 2)
        private BigDecimal totalMonthlyEmi;

        /**
         * creditScore — the applicant's CIBIL/credit bureau score.
         * Optional at submission (New-To-Credit applicants may not have one).
         * The BRE will handle the missing-score case during evaluation.
         * Range: 300–900 for CIBIL; we allow null to represent "no history".
         */
        @Min(value = 300, message = "Credit score must be between 300 and 900")
        @Max(value = 900, message = "Credit score must be between 300 and 900")
        private Integer creditScore;

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 60, message = "Years of experience cannot exceed 60")
        private Integer yearsOfExperience;
    }
}
