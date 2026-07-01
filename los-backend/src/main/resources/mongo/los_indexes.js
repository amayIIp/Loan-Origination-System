






































use("los_db");

print("═══════════════════════════════════════════════════");
print("  LOS MongoDB Index Creation Script");
print("  Database: los_db");
print("  Started:", new Date().toISOString());
print("═══════════════════════════════════════════════════\n");




print("▶ Creating indexes on 'applicants' collection...\n");














db.applicants.createIndex(
    { "email": 1 },  
    {
        name: "idx_applicants_email_unique",
        unique: true,       
        background: true,   
        comment: "Unique index for applicant email — deduplication and notification lookups"
    }
);
print("  ✅ idx_applicants_email_unique created");







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










db.applicants.createIndex(
    { "created_at": -1 },
    {
        name: "idx_applicants_created_at",
        background: true,
        comment: "Descending index on created_at for time-range queries and reports"
    }
);
print("  ✅ idx_applicants_created_at created");












db.applicants.createIndex(
    { "is_active": 1 },
    {
        name: "idx_applicants_is_active",
        background: true,
        comment: "Supports soft-delete filtering — most queries filter is_active=true"
    }
);
print("  ✅ idx_applicants_is_active created");












db.applicants.createIndex(
    { "kyc_documents.verification_status": 1 },
    {
        name: "idx_applicants_kyc_verification_status",
        background: true,
        comment: "Multikey index on embedded KYC document statuses — supports batch KYC workflow queries"
    }
);
print("  ✅ idx_applicants_kyc_verification_status created (multikey)\n");





print("▶ Creating indexes on 'loan_applications' collection...\n");








db.loan_applications.createIndex(
    { "applicant_id": 1 },
    {
        name: "idx_loans_applicant_id",
        background: true,
        comment: "Primary foreign-key index — all queries filtering by applicant_id use this"
    }
);
print("  ✅ idx_loans_applicant_id created");





db.loan_applications.createIndex(
    { "status": 1 },
    {
        name: "idx_loans_status",
        background: true,
        comment: "Covers simple status-filter queries and countByStatus() calls"
    }
);
print("  ✅ idx_loans_status created");





db.loan_applications.createIndex(
    { "created_at": -1 },
    {
        name: "idx_loans_created_at",
        background: true,
        comment: "Descending created_at index for time-range reports and newest-first sorting"
    }
);
print("  ✅ idx_loans_created_at created");


















db.loan_applications.createIndex(
    { "status": 1, "created_at": -1 },
    {
        name: "idx_loans_status_created_compound",
        background: true,
        comment: "PRIMARY compound index — covers status-filter + date-range queries (ESR rule applied)"
    }
);
print("  ✅ idx_loans_status_created_compound created");







db.loan_applications.createIndex(
    { "applicant_id": 1, "status": 1 },
    {
        name: "idx_loans_applicant_status_compound",
        background: true,
        comment: "Covers duplicate-application checks and per-applicant status queries"
    }
);
print("  ✅ idx_loans_applicant_status_compound created");





db.loan_applications.createIndex(
    { "assigned_officer_id": 1 },
    {
        name: "idx_loans_assigned_officer",
        background: true,
        
        
        
        sparse: true,
        comment: "Sparse index — only covers assigned applications; skips unassigned (null) docs"
    }
);
print("  ✅ idx_loans_assigned_officer created (sparse)\n");





print("▶ Creating indexes on 'credit_rules' collection...\n");







db.credit_rules.createIndex(
    { "is_active": 1, "weight": -1 },
    {
        name: "idx_rules_active_weight",
        background: true,
        comment: "BRE primary index — loads active rules sorted by weight in one index scan"
    }
);
print("  ✅ idx_rules_active_weight created");




db.credit_rules.createIndex(
    { "rule_type": 1 },
    {
        name: "idx_rules_rule_type",
        background: true,
        comment: "Supports admin filter by rule_type and BRE partial rule loading"
    }
);
print("  ✅ idx_rules_rule_type created\n");





print("▶ Creating indexes on 'decision_logs' collection...\n");







db.decision_logs.createIndex(
    { "loan_application_id": 1, "evaluated_at": -1 },
    {
        name: "idx_decision_logs_app_evaluated",
        background: true,
        comment: "PRIMARY decision_logs index — covers all lookups by loan application"
    }
);
print("  ✅ idx_decision_logs_app_evaluated created");





db.decision_logs.createIndex(
    { "applicant_id": 1, "final_decision": 1 },
    {
        name: "idx_decision_logs_applicant_decision",
        background: true,
        comment: "Compliance reporting — find all decisions of a given type for one applicant"
    }
);
print("  ✅ idx_decision_logs_applicant_decision created");






db.decision_logs.createIndex(
    { "evaluated_at": -1, "final_decision": 1 },
    {
        name: "idx_decision_logs_evaluated_decision",
        background: true,
        comment: "Approval rate reporting — date-range queries filtered by decision type"
    }
);
print("  ✅ idx_decision_logs_evaluated_decision created");






db.decision_logs.createIndex(
    { "evaluator_version": 1 },
    {
        name: "idx_decision_logs_evaluator_version",
        background: true,
        comment: "BRE version impact analysis — find all decisions by a specific BRE version"
    }
);
print("  ✅ idx_decision_logs_evaluator_version created\n");





print("▶ Creating indexes on 'users' collection...\n");





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





print("═══════════════════════════════════════════════════");
print("  INDEX VERIFICATION");
print("═══════════════════════════════════════════════════\n");

const collections = ["applicants", "loan_applications", "credit_rules", "decision_logs", "users"];

collections.forEach(collectionName => {
    
    const indexes = db.getCollection(collectionName).getIndexes();
    print(`📦 ${collectionName}: ${indexes.length} indexes`);
    indexes.forEach(idx => {
        
        print(`   • ${idx.name}: ${JSON.stringify(idx.key)}`);
    });
    print("");
});

print("═══════════════════════════════════════════════════");
print("  ✅ Index creation complete!");
print("  Finished:", new Date().toISOString());
print("═══════════════════════════════════════════════════");
