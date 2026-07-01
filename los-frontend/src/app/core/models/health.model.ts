/**
 * health.model.ts — TypeScript interface defining the shape of the
 * health-check API response from the Spring Boot backend.
 *
 * What is a TypeScript interface? (beginner explanation)
 * ───────────────────────────────────────────────────────
 * An interface describes the "shape" (properties and their types) of an object.
 * TypeScript uses it at compile time to catch mistakes — if the backend changes
 * its response and our code tries to access a field that no longer exists,
 * TypeScript will warn us immediately rather than failing silently at runtime.
 *
 * It is purely a compile-time construct — it generates NO JavaScript in the output.
 */

/**
 * HealthResponse — mirrors the JSON response body from GET /api/health.
 * If the backend changes any field name or type, update this interface to match.
 */
export interface HealthResponse {
  /** "UP" or "DOWN" — indicates whether the backend service is running */
  status: string;

  /** Name of the microservice that responded (e.g., "los-backend") */
  service: string;

  /** Human-readable label (e.g., "Loan Origination System — Backend API") */
  description: string;

  /** API version string (e.g., "1.0.0") — helps detect mismatched frontend/backend */
  version: string;

  /** ISO-8601 UTC timestamp of when the response was generated (e.g., "2024-06-01T10:30:00Z") */
  timestamp: string;
}
