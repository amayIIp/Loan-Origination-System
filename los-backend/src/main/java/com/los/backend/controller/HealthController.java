package com.los.backend.controller;

// java.time gives us timezone-aware date/time — safer than legacy java.util.Date
import java.time.Instant;

// Map is a key-value data structure — we use it to build a flexible JSON response
// without needing a dedicated DTO (Data Transfer Object) class for this simple endpoint.
import java.util.LinkedHashMap;
import java.util.Map;

// @RestController = @Controller + @ResponseBody combined:
// - @Controller tells Spring this class handles HTTP requests
// - @ResponseBody tells Spring to serialize the return value directly to JSON
//   instead of looking for an HTML template file
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ResponseEntity lets us control the full HTTP response:
// status code, headers, and body — important for professional APIs
import org.springframework.http.ResponseEntity;

/**
 * HealthController — a simple "is the server alive?" endpoint.
 *
 * This is the first REST controller we build. It proves that:
 * 1. The Spring Boot application started correctly.
 * 2. The Angular frontend can reach the backend over HTTP (CORS is working).
 * 3. Our Maven dependencies compiled and linked without errors.
 *
 * A REST Controller (beginner explanation):
 * ─────────────────────────────────────────
 * A controller is like a receptionist — it sits at a specific URL path,
 * listens for incoming HTTP requests, does some work, and sends back a response.
 * In our case, the "work" is simply assembling a status map and returning it as JSON.
 */
// @RestController — register this class as an HTTP request handler
@RestController
// @RequestMapping — all methods in this class will be reachable under /health
// Combined with the context-path "/api" set in application.yml,
// the full URL becomes: GET http://localhost:8080/api/health
@RequestMapping("/health")
public class HealthController {

    /**
     * getHealth — handles HTTP GET requests to /api/health.
     *
     * Returns a JSON object like:
     * {
     *   "status": "UP",
     *   "service": "los-backend",
     *   "version": "1.0.0",
     *   "timestamp": "2024-06-01T10:30:00.000Z"
     * }
     *
     * @return ResponseEntity containing the status map and HTTP 200 OK status code.
     */
    // @GetMapping — listen only for HTTP GET requests on this method's path
    // (no extra path here, so it maps to /api/health directly)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {

        // LinkedHashMap preserves insertion order — our JSON fields will appear
        // in the same order we put them in, making the response easier to read.
        Map<String, Object> response = new LinkedHashMap<>();

        // "status": "UP" — standard convention borrowed from Spring Actuator.
        // Monitoring systems (Kubernetes, AWS ELB health checks) look for this field.
        response.put("status", "UP");

        // "service" — identifies which microservice responded, useful when
        // multiple services share a monitoring dashboard.
        response.put("service", "los-backend");

        // "description" — human-readable label shown in dev tools and dashboards
        response.put("description", "Loan Origination System — Backend API");

        // "version" — helps the frontend know which API contract it is talking to.
        // Increment this when you make breaking changes to the API.
        response.put("version", "1.0.0");

        // Instant.now() returns the current UTC timestamp as an ISO-8601 string
        // e.g. "2024-06-01T10:30:00.000Z" — the "Z" means UTC (no timezone offset).
        // We use UTC so the timestamp means the same thing regardless of where
        // the server is hosted (different regions = different local times).
        response.put("timestamp", Instant.now().toString());

        // ResponseEntity.ok() wraps our map in an HTTP 200 OK response.
        // HTTP 200 = success; the caller knows the request was handled properly.
        return ResponseEntity.ok(response);
    }
}
