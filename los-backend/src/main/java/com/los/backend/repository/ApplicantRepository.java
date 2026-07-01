package com.los.backend.repository;

/*
 * What is a Spring Data Repository? (beginner explanation)
 * ─────────────────────────────────────────────────────────
 * A "repository" is an interface that handles all database read/write operations
 * for one entity type. We write ONLY the interface — no implementation class.
 * Spring Data MongoDB generates the actual database query code at startup by
 * reading our method names and translating them into MongoDB queries automatically.
 *
 * For example, the method findByEmail(String email) is automatically translated to:
 *   db.applicants.find({ "email": "someone@example.com" })
 * We never write that MongoDB query manually — Spring infers it from the method name.
 *
 * MongoRepository<EntityType, IdType> gives us free built-in methods:
 *   save(entity)        → INSERT or UPDATE one document
 *   findById(id)        → SELECT WHERE _id = ?
 *   findAll()           → SELECT all documents
 *   deleteById(id)      → DELETE WHERE _id = ?
 *   count()             → COUNT all documents
 *   existsById(id)      → EXISTS WHERE _id = ?
 * We add custom methods below for our domain-specific query needs.
 */

import com.los.backend.model.Applicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ApplicantRepository — data access interface for the "applicants" MongoDB collection.
 *
 * Spring Data translates each method signature below into the equivalent MongoDB query.
 * The repository is injected into ApplicantService via Spring's Dependency Injection.
 *
 * @Repository — optional annotation (MongoRepository already implies it), but we add it
 *   explicitly for clarity: this tells Spring "this is a data access component" and
 *   enables Spring's exception translation (converts low-level MongoDB exceptions into
 *   Spring's portable DataAccessException hierarchy).
 */
@Repository
public interface ApplicantRepository extends MongoRepository<Applicant, String> {

    // ── Method 1: findByEmail ─────────────────────────────────────────────────
    /**
     * Find one applicant by their email address.
     *
     * Use case: User registration deduplication — before creating a new Applicant,
     * we check if this email already exists. If present, we return a conflict error
     * rather than creating a duplicate profile.
     *
     * Returns Optional<Applicant> — a container that is either:
     *   - Optional.of(applicant) if found
     *   - Optional.empty() if no document has this email
     * The caller uses applicant.isPresent() to check, avoiding NullPointerException.
     *
     * Generated MongoDB query: db.applicants.findOne({ "email": <email> })
     * Performance: fast due to the unique index on "email" (O(log n) index lookup)
     *
     * @param email the email address to search for (case-sensitive)
     * @return an Optional containing the matching Applicant, or empty if not found
     */
    Optional<Applicant> findByEmail(String email);

    // ── Method 2: findByPanNumber ─────────────────────────────────────────────
    /**
     * Find one applicant by their PAN card number.
     *
     * Use case: KYC deduplication — a person cannot hold two applicant profiles
     * under the same PAN. We check this before creating a new applicant to prevent
     * identity fraud (using one PAN to apply for multiple parallel loans simultaneously).
     *
     * Generated MongoDB query: db.applicants.findOne({ "pan_number": <panNumber> })
     * Performance: fast due to the unique index on "pan_number"
     *
     * @param panNumber the 10-character PAN in format AAAAA9999A
     * @return an Optional containing the matching Applicant, or empty if not found
     */
    Optional<Applicant> findByPanNumber(String panNumber);

    // ── Method 3: findByIsActiveTrue with Pagination ──────────────────────────
    /**
     * Find all active (not soft-deleted) applicants, with pagination support.
     *
     * Use case: Loan officer admin panel — listing all applicants in the system.
     * We NEVER fetch all documents without pagination in production — a table with
     * 100,000 applicants would time out and exhaust server memory if returned at once.
     *
     * Pageable parameter (beginner explanation):
     * Pageable is passed by the caller and specifies:
     *   - which page to return (page 0, 1, 2, ...)
     *   - how many items per page (e.g., 20)
     *   - optional sort field and direction
     * Example: PageRequest.of(0, 20, Sort.by("createdAt").descending())
     *
     * Returns Page<Applicant> — contains:
     *   - the list of items on this page
     *   - total number of matching documents (for "Page 1 of 47" display)
     *   - navigation helpers (hasNext(), hasPrevious(), etc.)
     *
     * Generated query: db.applicants.find({ "is_active": true }).skip(n).limit(20)
     *
     * @param pageable pagination and sort parameters provided by the controller
     * @return a Page object containing this slice of active applicants
     */
    Page<Applicant> findByIsActiveTrue(Pageable pageable);

    // ── Method 4: findByCreatedAtBetweenAndIsActiveTrue ───────────────────────
    /**
     * Find all active applicants who registered within a given date range.
     *
     * Use case: Monthly/weekly onboarding reports — "How many new applicants
     * registered in June 2024?" Used by the reporting dashboard (Phase 5).
     *
     * Generated query:
     *   db.applicants.find({
     *     "created_at": { "$gte": <from>, "$lte": <to> },
     *     "is_active": true
     *   })
     * Performance: uses the index on "created_at" for the range scan
     *
     * @param from start of the date range (inclusive) — UTC Instant
     * @param to   end of the date range (inclusive) — UTC Instant
     * @return list of matching Applicant documents (use pagination for large ranges)
     */
    List<Applicant> findByCreatedAtBetweenAndIsActiveTrue(Instant from, Instant to);

    // ── Method 5: Custom @Query with MongoDB query language ───────────────────
    /**
     * Find all applicants whose KYC documents include at least one document
     * with verificationStatus = REJECTED — meaning they need to re-upload.
     *
     * Why @Query? — Spring Data cannot auto-generate queries that filter on
     * fields inside embedded sub-arrays. We use @Query with MongoDB's array
     * element match syntax instead.
     *
     * MongoDB query explanation:
     *   "kyc_documents": { "$elemMatch": { "verification_status": "REJECTED" } }
     *   $elemMatch — matches documents where AT LEAST ONE element of the array
     *   satisfies all the given conditions. Without $elemMatch, we'd get false
     *   positives if separate array elements partially match different conditions.
     *
     * Use case: Daily batch job that notifies applicants about rejected KYC docs.
     *
     * @return list of applicants who have at least one rejected KYC document
     */
    @Query("{ 'kyc_documents': { $elemMatch: { 'verification_status': 'REJECTED' } } }")
    List<Applicant> findApplicantsWithRejectedKycDocuments();

    // ── Method 6: countByIsActiveTrue ─────────────────────────────────────────
    /**
     * Count the total number of active applicants in the system.
     *
     * Use case: Dashboard KPI widget — "Total registered applicants: 4,823"
     * COUNT queries are extremely fast with MongoDB's index-backed collection stats.
     *
     * Generated query: db.applicants.countDocuments({ "is_active": true })
     *
     * @return the count of active Applicant documents
     */
    long countByIsActiveTrue();

    // ── Method 7: existsByEmailOrPanNumber ────────────────────────────────────
    /**
     * Check if an applicant already exists with the given email OR PAN number.
     *
     * Use case: Registration validation — before saving a new applicant, we do
     * a single existence check for BOTH deduplication keys at once, reducing
     * round-trips to the database from 2 to 1.
     *
     * Generated query:
     *   db.applicants.find({ $or: [{ "email": <email> }, { "pan_number": <pan> }] }).limit(1)
     *
     * Returns boolean — simpler than returning the full document when we only
     * need to know "does this exist?" for a conflict check.
     *
     * @param email     the email to check for duplicates
     * @param panNumber the PAN to check for duplicates
     * @return true if a matching applicant exists, false otherwise
     */
    boolean existsByEmailOrPanNumber(String email, String panNumber);
}
