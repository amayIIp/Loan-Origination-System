package com.los.backend.config;

/*
 * MongoConfig — Spring Data MongoDB configuration.
 *
 * What does this config class do? (beginner explanation)
 * ────────────────────────────────────────────────────────
 * Spring Data MongoDB's @CreatedDate and @LastModifiedDate annotations
 * (used in our model classes) ONLY work if MongoDB auditing is enabled.
 * This class turns on that feature so Spring automatically populates
 * timestamp fields whenever a document is saved.
 *
 * Without this class, @CreatedDate and @LastModifiedDate fields would
 * remain null even after saving — a silent data integrity failure.
 */

// @Configuration — this class provides Spring beans (configured objects)
import org.springframework.context.annotation.Configuration;

// @EnableMongoAuditing — activates automatic population of @CreatedDate,
// @LastModifiedDate, @CreatedBy, @LastModifiedBy fields on every save().
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// AbstractMongoClientConfiguration — base class for custom MongoDB config.
// We don't extend it here (application.yml handles connection config),
// but it's available if you need fine-grained driver control.

/**
 * MongoConfig — enables Spring Data MongoDB auditing features.
 *
 * Auditing means: every time we call repository.save(entity), Spring Data
 * automatically sets:
 *   - @CreatedDate fields   → current UTC Instant (ONLY on first save)
 *   - @LastModifiedDate fields → current UTC Instant (on EVERY save)
 *
 * This keeps our business logic clean — we never manually set timestamps.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // No additional bean definitions needed in Phase 1.
    // Spring Boot auto-configures the MongoClient from application.yml.
    //
    // Phase 2 additions (add here when implementing):
    // 1. MongoCustomConversions — for custom type mappings (e.g., BigDecimal ↔ Decimal128)
    // 2. AuditorAware<String> bean — tells Spring WHO made a change (current logged-in userId)
    //    so @CreatedBy / @LastModifiedBy fields are also auto-populated.
    // 3. IndexOperations — programmatic index creation (alternative to @Indexed annotations)
}
