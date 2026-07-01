// ═══════════════════════════════════════════════════════════════════════════
// los_indexes.js — MongoDB index creation script for the LOS database.
//
// WHAT IS A MONGODB INDEX? (beginner explanation)
// ──────────────────────────────────────────────────────────────────────────
// An index in MongoDB is a special data structure (a B-tree) that stores a
// small subset of the collection's data in a sorted, easy-to-search form.
// Without indexes, MongoDB must scan EVERY document in a collection to find
// matches (called a "COLLSCAN" = collection scan) — O(n) complexity.
// With an index on the query field, MongoDB jumps directly to matching
// documents — O(log n) complexity. For a collection with 1 million documents,
// this is the difference between ~1ms and ~1000ms per query.
//
// HOW TO RUN THIS SCRIPT:
// ──────────────────────────────────────────────────────────────────────────
// Option 1 — mongosh (MongoDB Shell):
//   mongosh mongodb://localhost:27017/los_db los_indexes.js
//
// Option 2 — mongosh interactive mode:
//   mongosh
//   use los_db
//   load("/path/to/los_indexes.js")
//
// Option 3 — Docker (if MongoDB runs in Docker):
//   docker cp los_indexes.js los-mongodb:/tmp/
//   docker exec -it los-mongodb mongosh los_db /tmp/los_indexes.js
//
// WHEN TO RUN:
//   - After first deployment (before the application handles any traffic)
//   - After adding new query patterns that need index support
//   - Index creation on large existing collections should use { background: true }
//     (already set below) to avoid locking the collection during the build.
//
// IMPORTANT: createIndex() is idempotent — running this script multiple times
// will NOT create duplicate indexes. MongoDB checks if an identical index already
// exists and skips creation if it does. Safe to run repeatedly.
// ═══════════════════════════════════════════════════════════════════════════

// Ensure we're operating on the correct database
use("los_db");

print("═══════════════════════════════════════════════════");
print("  LOS MongoDB Index Creation Script");
print("  Database: los_db");
print("  Started:", new Date().toISOString());
print("═══════════════════════════════════════════════════\n");

// ──────────────────────────────────────────────────────────────────────────
// COLLECTION: applicants
// ──────────────────────────────────────────────────────────────────────────
print("▶ Creating indexes on 'applicants' collection...\n");

// INDEX 1: email (unique)
// ──────────────────────────────────────────────────────────────────────────
// WHY: Email is used for:
//   a) Login deduplication — before creating a new applicant, we check for email conflicts.
//   b) Notification delivery — we query by email when sending status update emails.
//   c) Applicant lookup when integrating with external email-based identity services.
//
// unique: true — MongoDB will REJECT any insert that would create a second document
// with the same email. This is our database-level safety net in addition to the
// application-level check in ApplicantService.
//
// Performance: O(log n) lookup instead of O(n) full scan.
// For 1,000,000 applicants, this means <1ms instead of ~500ms for a single lookup.
db.applicants.createIndex(
    { "email": 1 },  // 1 = ascending order in the index (required for uniqueness)
    {
        name: "idx_applicants_email_unique",
        unique: true,       // Enforces uniqueness at the database level
        background: true,   // Builds without blocking reads/writes on this collection
        comment: "Unique index for applicant email — deduplication and notification lookups"
    }
);
print("  ✅ idx_applicants_email_unique created");

// INDEX 2: pan_number (unique)
// ──────────────────────────────────────────────────────────────────────────
// WHY: PAN card number uniquely identifies an Indian taxpayer.
//   a) KYC deduplication — prevents the same person from creating two applicant profiles.
//   b) Anti-fraud — two applications with the same PAN = potential identity fraud.
//   c) Government compliance — we may need to report by PAN for regulatory filings.
db.applicants.createIndex(
    { "pan_number": 1 },
    {
        name: "idx_applicants_pan_unique",
        unique: true,
        background: true,
        comment: "Unique index for PAN card number — KYC deduplication and fraud detection"
    }
);
print("  ✅ idx_applicants_pan_unique created");

// INDEX 3: created_at (descending)
// ──────────────────────────────────────────────────────────────────────────
// WHY: Supports time-range queries:
//   "Find applicants registered between [date1] and [date2]" (monthly reports)
//   "Find applicants registered in the last 7 days" (weekly onboarding report)
//
// -1 = descending order — most recent first, which is the natural sort order
//   for "show me new registrations" queries. An index in descending order
//   means MongoDB can read the first N results without a sort step.
db.applicants.createIndex(
    { "created_at": -1 },
    {
        name: "idx_applicants_created_at",
        background: true,
        comment: "Descending index on created_at for time-range queries and reports"
    }
);
print("  ✅ idx_applicants_created_at created");

// INDEX 4: is_active
// ──────────────────────────────────────────────────────────────────────────
// WHY: Nearly every query filters on is_active = true (we soft-delete records,
//   so we always want to exclude deleted ones). Without this index, MongoDB must
//   read ALL applicants and then filter out the deleted ones — wasteful when
//   99%+ of documents are active.
//
// Note: A boolean field index has low cardinality (only 2 values: true/false).
// MongoDB's query planner may sometimes prefer a collection scan for boolean-only
// filters. However, COMBINED with other filter fields (e.g., created_at range),
// this index becomes a useful part of a compound index scan.
db.applicants.createIndex(
    { "is_active": 1 },
    {
        name: "idx_applicants_is_active",
        background: true,
        comment: "Supports soft-delete filtering — most queries filter is_active=true"
    }
);
print("  ✅ idx_applicants_is_active created");

// INDEX 5: kyc_documents.verification_status (multikey index)
// ──────────────────────────────────────────────────────────────────────────
// WHY: The BRE and KYC workflow query "which applicants have PENDING or REJECTED docs?"
//   MongoDB automatically creates a "multikey" index when the indexed field is inside
//   an array — it indexes EVERY element of the array, not just the outer document.
//
// MULTIKEY INDEX explanation:
//   Our kyc_documents is an array of sub-documents. MongoDB indexes each element:
//   Document: { kyc_documents: [{verification_status: "PENDING"}, {verification_status: "VERIFIED"}] }
//   Index entries: "PENDING" → doc1, "VERIFIED" → doc1
//   So querying { "kyc_documents.verification_status": "PENDING" } hits the index.
db.applicants.createIndex(
    { "kyc_documents.verification_status": 1 },
    {
        name: "idx_applicants_kyc_verification_status",
        background: true,
        comment: "Multikey index on embedded KYC document statuses — supports batch KYC workflow queries"
    }
);
print("  ✅ idx_applicants_kyc_verification_status created (multikey)\n");


// ──────────────────────────────────────────────────────────────────────────
// COLLECTION: loan_applications
// ──────────────────────────────────────────────────────────────────────────
print("▶ Creating indexes on 'loan_applications' collection...\n");

// INDEX 6: applicant_id (single field)
// ──────────────────────────────────────────────────────────────────────────
// WHY: "Find all loans for this applicant" is the single most common query
//   in the LOS — it powers the applicant portal, officer dashboard, and the
//   duplicate-application check in LoanService.
//   Without this index, the query scans the ENTIRE loan_applications collection
//   for every request. Disastrous at scale.
db.loan_applications.createIndex(
    { "applicant_id": 1 },
    {
        name: "idx_loans_applicant_id",
        background: true,
        comment: "Primary foreign-key index — all queries filtering by applicant_id use this"
    }
);
print("  ✅ idx_loans_applicant_id created");

// INDEX 7: status (single field)
// ──────────────────────────────────────────────────────────────────────────
// WHY: The officer dashboard primary view is "show all applications in status X".
//   Simple single-field index covers COUNT queries by status efficiently too.
db.loan_applications.createIndex(
    { "status": 1 },
    {
        name: "idx_loans_status",
        background: true,
        comment: "Covers simple status-filter queries and countByStatus() calls"
    }
);
print("  ✅ idx_loans_status created");

// INDEX 8: created_at (descending)
// ──────────────────────────────────────────────────────────────────────────
// WHY: Many queries sort by created_at DESC (newest first).
//   Also supports date-range filters for monthly/weekly pipeline reports.
db.loan_applications.createIndex(
    { "created_at": -1 },
    {
        name: "idx_loans_created_at",
        background: true,
        comment: "Descending created_at index for time-range reports and newest-first sorting"
    }
);
print("  ✅ idx_loans_created_at created");

// INDEX 9: COMPOUND — (status, created_at DESC) ← MOST IMPORTANT LOAN INDEX
// ──────────────────────────────────────────────────────────────────────────
// WHY: This is the most common compound query pattern in the entire LOS:
//   "Give me all UNDER_REVIEW applications from the last 7 days, newest first"
//   db.loan_applications.find({ status: "UNDER_REVIEW", created_at: { $gte: ... } })
//                        .sort({ created_at: -1 })
//
// COMPOUND INDEX ADVANTAGE:
//   MongoDB can satisfy BOTH the status equality filter AND the created_at range
//   filter + sort using a SINGLE index scan — no separate sort step needed.
//   Two separate indexes (status alone + created_at alone) would be less efficient
//   because MongoDB would have to use one index, then apply the other filter in memory.
//
// INDEX KEY ORDER RULE:
//   Equality fields first (status), range/sort fields last (created_at).
//   This is the ESR rule: Equality → Sort → Range.
//   Following ESR maximises how much of the index MongoDB can use per query.
db.loan_applications.createIndex(
    { "status": 1, "created_at": -1 },
    {
        name: "idx_loans_status_created_compound",
        background: true,
        comment: "PRIMARY compound index — covers status-filter + date-range queries (ESR rule applied)"
    }
);
print("  ✅ idx_loans_status_created_compound created");

// INDEX 10: COMPOUND — (applicant_id, status)
// ──────────────────────────────────────────────────────────────────────────
// WHY: Covers the duplicate-application check:
//   "Does applicant X have any active application?"
//   db.loan_applications.find({ applicant_id: <id>, status: { $in: [...] } })
//   Both fields in one index → single index scan, no in-memory filtering.
db.loan_applications.createIndex(
    { "applicant_id": 1, "status": 1 },
    {
        name: "idx_loans_applicant_status_compound",
        background: true,
        comment: "Covers duplicate-application checks and per-applicant status queries"
    }
);
print("  ✅ idx_loans_applicant_status_compound created");

// INDEX 11: assigned_officer_id
// ──────────────────────────────────────────────────────────────────────────
// WHY: Officer personal work queue — "Find all applications assigned to officer X"
//   is queried every time a loan officer logs in and loads their dashboard.
db.loan_applications.createIndex(
    { "assigned_officer_id": 1 },
    {
        name: "idx_loans_assigned_officer",
        background: true,
        // sparse: true — only indexes documents where assigned_officer_id exists.
        // Many applications are unassigned (null field). A sparse index skips
        // null-value documents, making the index smaller and faster to build.
        sparse: true,
        comment: "Sparse index — only covers assigned applications; skips unassigned (null) docs"
    }
);
print("  ✅ idx_loans_assigned_officer created (sparse)\n");


// ──────────────────────────────────────────────────────────────────────────
// COLLECTION: credit_rules
// ──────────────────────────────────────────────────────────────────────────
print("▶ Creating indexes on 'credit_rules' collection...\n");

// INDEX 12: is_active + rule_type (compound)
// ──────────────────────────────────────────────────────────────────────────
// WHY: The BRE's primary rule-loading query is:
//   "Find all active (is_active=true) rules, ordered by weight"
//   With a compound index on (is_active, weight), the BRE can find active rules
//   in sorted order without a separate sort step.
db.credit_rules.createIndex(
    { "is_active": 1, "weight": -1 },
    {
        name: "idx_rules_active_weight",
        background: true,
        comment: "BRE primary index — loads active rules sorted by weight in one index scan"
    }
);
print("  ✅ idx_rules_active_weight created");

// INDEX 13: rule_type
// ──────────────────────────────────────────────────────────────────────────
// WHY: Admin UI filter: "Show me all CREDIT_SCORE rules"
db.credit_rules.createIndex(
    { "rule_type": 1 },
    {
        name: "idx_rules_rule_type",
        background: true,
        comment: "Supports admin filter by rule_type and BRE partial rule loading"
    }
);
print("  ✅ idx_rules_rule_type created\n");


// ──────────────────────────────────────────────────────────────────────────
// COLLECTION: decision_logs
// ──────────────────────────────────────────────────────────────────────────
print("▶ Creating indexes on 'decision_logs' collection...\n");

// INDEX 14: COMPOUND — (loan_application_id, evaluated_at DESC) ← PRIMARY
// ──────────────────────────────────────────────────────────────────────────
// WHY: The two most common queries on this collection:
//   a) "Find all decisions for application X, newest first"
//   b) "Find the latest decision for application X" (findTop1By...)
//   Both are covered by this compound index in a single scan.
db.decision_logs.createIndex(
    { "loan_application_id": 1, "evaluated_at": -1 },
    {
        name: "idx_decision_logs_app_evaluated",
        background: true,
        comment: "PRIMARY decision_logs index — covers all lookups by loan application"
    }
);
print("  ✅ idx_decision_logs_app_evaluated created");

// INDEX 15: applicant_id + final_decision
// ──────────────────────────────────────────────────────────────────────────
// WHY: Compliance queries: "All REJECTED decisions for applicant X".
//   applicantId is denormalised onto DecisionLog for exactly this purpose.
db.decision_logs.createIndex(
    { "applicant_id": 1, "final_decision": 1 },
    {
        name: "idx_decision_logs_applicant_decision",
        background: true,
        comment: "Compliance reporting — find all decisions of a given type for one applicant"
    }
);
print("  ✅ idx_decision_logs_applicant_decision created");

// INDEX 16: evaluated_at + final_decision (compound)
// ──────────────────────────────────────────────────────────────────────────
// WHY: Approval rate reporting:
//   "How many APPROVED decisions were made in June 2024?"
//   date-range filter on evaluated_at + equality filter on final_decision.
db.decision_logs.createIndex(
    { "evaluated_at": -1, "final_decision": 1 },
    {
        name: "idx_decision_logs_evaluated_decision",
        background: true,
        comment: "Approval rate reporting — date-range queries filtered by decision type"
    }
);
print("  ✅ idx_decision_logs_evaluated_decision created");

// INDEX 17: evaluator_version (partial index)
// ──────────────────────────────────────────────────────────────────────────
// WHY: BRE version impact analysis. Partial index only covers the most recent
//   versions (in production you'd filter for versions >= some minimum string).
//   Here we keep it simple — plain index on the field.
db.decision_logs.createIndex(
    { "evaluator_version": 1 },
    {
        name: "idx_decision_logs_evaluator_version",
        background: true,
        comment: "BRE version impact analysis — find all decisions by a specific BRE version"
    }
);
print("  ✅ idx_decision_logs_evaluator_version created\n");


// ──────────────────────────────────────────────────────────────────────────
// COLLECTION: users
// ──────────────────────────────────────────────────────────────────────────
print("▶ Creating indexes on 'users' collection...\n");

// INDEX 18: username (unique)
// ──────────────────────────────────────────────────────────────────────────
// WHY: Spring Security calls findByUsername() on every login attempt.
//   Must be O(log n). Unique ensures no two accounts share a username.
db.users.createIndex(
    { "username": 1 },
    {
        name: "idx_users_username_unique",
        unique: true,
        background: true,
        comment: "Unique index for Spring Security authentication — login username lookup"
    }
);
print("  ✅ idx_users_username_unique created");

// INDEX 19: email (unique)
// ──────────────────────────────────────────────────────────────────────────
db.users.createIndex(
    { "email": 1 },
    {
        name: "idx_users_email_unique",
        unique: true,
        background: true,
        comment: "Unique index for user email — password reset and account deduplication"
    }
);
print("  ✅ idx_users_email_unique created\n");


// ──────────────────────────────────────────────────────────────────────────
// VERIFICATION — Print all indexes for each collection
// ──────────────────────────────────────────────────────────────────────────
print("═══════════════════════════════════════════════════");
print("  INDEX VERIFICATION");
print("═══════════════════════════════════════════════════\n");

const collections = ["applicants", "loan_applications", "credit_rules", "decision_logs", "users"];

collections.forEach(collectionName => {
    // getIndexes() returns an array of all index definitions on this collection
    const indexes = db.getCollection(collectionName).getIndexes();
    print(`📦 ${collectionName}: ${indexes.length} indexes`);
    indexes.forEach(idx => {
        // Print each index name and its key pattern for verification
        print(`   • ${idx.name}: ${JSON.stringify(idx.key)}`);
    });
    print("");
});

print("═══════════════════════════════════════════════════");
print("  ✅ Index creation complete!");
print("  Finished:", new Date().toISOString());
print("═══════════════════════════════════════════════════");
