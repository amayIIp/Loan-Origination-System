package com.los.backend.model;

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * ENTITY: User (Staff)
 * MongoDB Collection: "users"
 *
 * EMBEDDING vs REFERENCING decisions:
 *
 * No sub-documents are embedded in User for now because:
 * - A user's role/permissions are simple scalar fields or a small roles list.
 * - User profiles don't have complex nested data at this stage.
 *
 * REFERENCING from other entities:
 * - LoanApplication.assignedOfficerId → User._id (which loan officer is handling it)
 * - DecisionLog.triggeredByUserId → User._id (who triggered the credit check)
 * - CreditRule.modifiedBy → User._id (who last modified the rule)
 *
 * Security note: Password is stored as a BCrypt hash (never plaintext).
 * The raw password NEVER enters this class — hashing happens in UserService
 * before the User document is saved. Spring Security's BCryptPasswordEncoder
 * produces a 60-character hash that includes the salt and algorithm ID.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

/**
 * User — represents a staff member of the lending institution who uses the LOS backend.
 * This is NOT the loan applicant — applicants are stored in the Applicant collection.
 *
 * User types (roles):
 * - LOAN_OFFICER: reviews applications, triggers BRE, approves/rejects
 * - ADMIN: manages CreditRules, manages User accounts, views all data
 * - AUDITOR: read-only access for compliance and reporting
 *
 * Phase 2 will connect this entity to Spring Security for JWT-based auth.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    /** MongoDB auto-generated primary key for this staff user record */
    @Id
    private String id;

    /**
     * username — unique login identifier for the staff member.
     * @Indexed(unique=true) — login lookup must be O(1) and duplicates are invalid.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    // Only alphanumeric + underscore + hyphen characters allowed — prevents SQL/NoSQL injection
    // in username-based queries and makes usernames safe to display in URLs.
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, digits, underscores, and hyphens")
    @Indexed(unique = true)
    @Field("username")
    private String username;

    /**
     * email — staff member's corporate email address. Used for login and notifications.
     * @Indexed(unique=true) — unique constraint; a person cannot have two accounts.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Indexed(unique = true)
    @Field("email")
    private String email;

    /**
     * passwordHash — BCrypt hash of the user's password.
     *
     * ⚠️ NEVER store or log the raw password.
     * ⚠️ NEVER expose this field in any API response (use @JsonIgnore in Phase 2).
     *
     * BCrypt (beginner explanation):
     * BCrypt is a password-hashing algorithm that is deliberately slow (to resist
     * brute-force attacks). It automatically embeds a random "salt" in each hash,
     * so two users with the same password will have different hashes.
     * When verifying login: BCrypt.matches(rawPassword, storedHash) — never decrypt.
     */
    @NotBlank(message = "Password hash is required")
    @Field("password_hash")
    private String passwordHash;

    /**
     * firstName / lastName — the staff member's display name.
     * Shown in the loan officer dashboard header and assignment notifications.
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Field("first_name")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Field("last_name")
    private String lastName;

    /**
     * roles — list of role strings assigned to this user.
     * Possible values: "LOAN_OFFICER", "ADMIN", "AUDITOR"
     * Using a List allows a user to have multiple roles (e.g., an admin who also
     * reviews loans is both "ADMIN" and "LOAN_OFFICER").
     *
     * Why strings, not an enum? — Spring Security role checks use strings
     * (e.g., hasRole("ADMIN")), and a List<String> maps cleanly to Spring Security's
     * GrantedAuthority interface without extra conversion code.
     */
    @NotNull
    @Size(min = 1, message = "User must have at least one role")
    @Field("roles")
    private List<String> roles;

    /**
     * isActive — soft-delete / account suspension flag.
     * false = account is disabled; login attempts will be rejected by Spring Security.
     * We never hard-delete user accounts — the audit trail must remain intact
     * (who approved loan X must be traceable even after the officer leaves the company).
     */
    @Builder.Default
    @Field("is_active")
    private boolean isActive = true;

    /**
     * lastLoginAt — timestamp of the most recent successful login.
     * Used for security audits: accounts inactive for >90 days can be auto-suspended.
     */
    @Field("last_login_at")
    private Instant lastLoginAt;

    /**
     * department — the business unit this officer belongs to.
     * Example: "Retail Lending", "SME Loans", "Home Loans"
     * Used for workload assignment and reporting segmentation.
     */
    @Size(max = 100)
    @Field("department")
    private String department;

    /** Auto-set by Spring Data on account creation */
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    /** Auto-set by Spring Data on every profile update */
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    /** Convenience method — not stored in MongoDB */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
