package com.los.backend;

// Spring Boot's test runner — loads the full application context
// (all beans, configs, database connections) before running tests.
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

/**
 * LosBackendApplicationTests — smoke test to verify the application context loads.
 *
 * What is a "context load" test? (beginner explanation)
 * ──────────────────────────────────────────────────────
 * Spring Boot builds an "application context" on startup — it scans all your
 * classes, wires dependencies together, reads configuration, and connects to
 * databases. If ANY of that setup fails (bad config, missing bean, wrong type),
 * this test catches it immediately without needing to run the app manually.
 *
 * Think of it as a "does it even start?" safety net.
 */
// @SpringBootTest loads the full Spring context — equivalent to running the app
@SpringBootTest
class LosBackendApplicationTests {

    /**
     * contextLoads — passes if (and only if) the entire Spring context
     * initialises without throwing an exception.
     * No assertions needed — a thrown exception = test failure.
     */
    @Test
    void contextLoads() {
        // If Spring Boot can build its internal component graph without errors,
        // this test passes automatically. No explicit assertion required.
    }

}
