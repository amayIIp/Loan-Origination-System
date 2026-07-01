package com.los.backend.repository;

import com.los.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — data access for the "users" MongoDB collection.
 *
 * Used by: AuthService (Phase 2), UserManagementService (admin),
 *          LoanApplicationService (officer assignment).
 *
 * Security note: This repository returns full User documents including
 * password_hash. Service layer code must NEVER expose passwordHash in
 * API responses — map to a UserResponse DTO that excludes it.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find a user by username — the primary authentication lookup.
     * Spring Security's UserDetailsService calls this during login to load the
     * stored BCrypt hash for comparison against the submitted raw password.
     * Uses the unique index on "username" for O(log n) performance.
     *
     * @param username the login username
     * @return Optional<User> — empty if no account with this username exists
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email — used for password reset flows and duplicate-check
     * at registration time.
     *
     * @param email the staff member's corporate email
     * @return Optional<User> — empty if no account with this email exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all active users who have a specific role.
     *
     * Use case: Loan assignment — find all active LOAN_OFFICER accounts to build
     * a dropdown for assigning applications. Also used for bulk notifications.
     *
     * MongoDB query explanation:
     *   "roles": { "$in": [<role>] } — checks if the roles array contains the value.
     *   (The $in operator works on both scalar fields and array fields in MongoDB.)
     *   "is_active": true — only active accounts
     *
     * @param role the role string to filter by (e.g., "LOAN_OFFICER", "ADMIN")
     * @return list of active User documents having that role
     */
    @Query("{ 'roles': { $in: [?0] }, 'is_active': true }")
    List<User> findActiveUsersByRole(String role);

    /**
     * Check if a username or email is already registered.
     * Used during new account creation to prevent duplicates.
     *
     * @param username the username to check
     * @param email    the email to check
     * @return true if either already exists
     */
    boolean existsByUsernameOrEmail(String username, String email);

    /**
     * Count all active users grouped by role — admin dashboard KPI.
     * (Full aggregation version built in Phase 5; this count covers the simple case.)
     *
     * @return count of active users
     */
    long countByIsActiveTrue();
}
