package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * EMBEDDING vs REFERENCING decision for Address:
 *
 * WHY EMBEDDED (inside Applicant)?
 * ──────────────────────────────────────────────
 * Address is a VALUE OBJECT — it has no identity of its own and is only ever
 * read/updated as part of its owning Applicant. It's also tiny (6–8 fields),
 * and always fetched together with the applicant record. Embedding it avoids
 * a pointless secondary lookup and keeps the Applicant document self-contained,
 * which is the core philosophy of document databases: "store what you query together."
 * ─────────────────────────────────────────────────────────────────────────────
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Address — residential address of an Applicant.
 * Embedded as a sub-document inside the "applicants" MongoDB collection.
 * Not a separate @Document — has no independent MongoDB collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /**
     * line1 — first address line (house/flat number, building name, street).
     * @NotBlank — mandatory; we cannot verify residence without an address.
     * @Size — capped at 200 chars to prevent runaway storage and UI overflow.
     */
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200, message = "Address line 1 must not exceed 200 characters")
    @Field("line1")
    private String line1;

    /**
     * line2 — optional second address line (area, locality, landmark).
     * Many addresses don't need it, so nullable is correct here.
     */
    @Size(max = 200, message = "Address line 2 must not exceed 200 characters")
    @Field("line2")
    private String line2;

    /**
     * city — the city/town of residence.
     * @NotBlank — required for geographic risk assessment and postal communication.
     */
    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Field("city")
    private String city;

    /**
     * state — the Indian state or union territory (e.g., "Maharashtra", "Tamil Nadu").
     * Used for state-level regulatory reporting and lender branch routing.
     */
    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Field("state")
    private String state;

    /**
     * pincode — Indian postal PIN code — exactly 6 digits (e.g., "400001").
     * @Pattern — regex enforces exactly 6 digits, preventing "1234" or "ABCDEF".
     * The PIN code is also used to derive geographic location for risk profiling.
     */
    @NotBlank(message = "PIN code is required")
    // \\d{6} = exactly 6 digit characters, nothing else
    @Pattern(regexp = "\\d{6}", message = "PIN code must be exactly 6 digits")
    @Field("pincode")
    private String pincode;

    /**
     * country — defaults to "India" for this domestic lending system.
     * Kept as a field (not hardcoded) to support potential expansion to
     * other markets without a schema migration.
     */
    @NotBlank(message = "Country is required")
    @Field("country")
    @Builder.Default
    private String country = "India";
}
