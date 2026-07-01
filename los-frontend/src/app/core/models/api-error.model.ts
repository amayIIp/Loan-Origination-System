/**
 * api-error.model.ts — standardised shape for API error responses.
 *
 * When the backend returns an error (4xx, 5xx HTTP status codes), we expect
 * a structured JSON error body. This interface ensures we handle it consistently
 * across all services in the application.
 */
export interface ApiError {
  /** HTTP status code (e.g., 400, 404, 500) */
  status: number;

  /** Short error category (e.g., "BAD_REQUEST", "NOT_FOUND", "INTERNAL_ERROR") */
  error: string;

  /** Human-readable error message — safe to display to users or log for debugging */
  message: string;

  /** The URL path that triggered the error (e.g., "/api/loans/123") */
  path: string;

  /** ISO-8601 timestamp of when the error occurred on the server */
  timestamp: string;

  /** Validation field errors — populated when @Valid fails on a request body.
   *  Key = field name, Value = the validation error message for that field. */
  fieldErrors?: Record<string, string>;
}
