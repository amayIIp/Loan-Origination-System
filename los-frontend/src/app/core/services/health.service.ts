/**
 * health.service.ts — Angular service responsible for communicating with
 * the Spring Boot /api/health endpoint.
 *
 * What is an Angular Service? (beginner explanation)
 * ───────────────────────────────────────────────────
 * A service is a plain TypeScript class that holds business logic and
 * data-fetching code. It is SEPARATE from components (which only handle the UI).
 * Components should focus on "what to show"; services handle "how to get the data."
 *
 * We use Angular's Dependency Injection (DI) system to share one service instance
 * across many components — instead of every component making its own HTTP call,
 * they all share the same HealthService, which also makes testing easier
 * (we can "mock" the service in unit tests without a real backend).
 *
 * What is Dependency Injection? (beginner explanation)
 * ──────────────────────────────────────────────────────
 * DI is a design pattern where objects (services) are "injected" (handed to you)
 * by a framework rather than you having to create them with "new". Angular maintains
 * a registry of services; when a component declares it needs a service, Angular
 * looks it up and provides the existing instance automatically.
 */

// Injectable — marks this class as something Angular's DI system can manage and inject
import { Injectable } from '@angular/core';

// HttpClient — Angular's built-in HTTP client for making REST API calls.
// It returns RxJS Observables (explained below) rather than raw Promises.
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

// Observable — an RxJS data stream that emits values over time.
// Think of it like a "promise that can emit multiple values" and can be
// cancelled, retried, transformed, and combined with other streams.
import { Observable, throwError } from 'rxjs';

// catchError — an RxJS operator that intercepts errors in a stream
// and lets us handle or rethrow them gracefully.
// retry — retries a failed HTTP request N times before giving up.
// tap — lets us "peek" at stream values for logging without changing them.
import { catchError, retry, tap } from 'rxjs/operators';

// Our TypeScript interface describing the API response shape
import { HealthResponse } from '../models/health.model';

// The environment file holds our API base URL — importing from here means
// we only have to change the URL in one place when deploying to different environments.
import { environment } from '../../../environments/environment';

/**
 * @Injectable({ providedIn: 'root' }) — tells Angular's DI system to:
 * 1. Create exactly ONE instance of HealthService for the entire application.
 * 2. Make it available for injection anywhere without explicitly listing it
 *    in a module's providers array (modern Angular best practice).
 *
 * "providedIn: 'root'" = singleton scope — the same object is shared by all consumers.
 */
@Injectable({
  providedIn: 'root',
})
export class HealthService {

  /**
   * apiUrl — the base URL for all requests in this service.
   * Constructed from the environment file so it changes automatically
   * between development (localhost) and production (real domain).
   */
  private readonly apiUrl = `${environment.apiUrl}/health`;

  /**
   * Constructor — Angular calls this when creating the service instance.
   * We declare HttpClient as a parameter; Angular's DI system automatically
   * finds the configured HttpClient instance and passes it in for us.
   *
   * @param http — Angular's HTTP client, injected by the DI framework.
   *               We make it private so only this service's methods can use it.
   */
  constructor(private readonly http: HttpClient) {}

  /**
   * getHealth — fetches the backend health status from /api/health.
   *
   * Returns an Observable<HealthResponse> — the component that calls this
   * method must "subscribe" to the observable to actually trigger the HTTP request
   * and receive the response. Observables are lazy — nothing happens until
   * someone subscribes (or Angular's async pipe does it automatically in the template).
   *
   * @returns Observable that emits a HealthResponse object when the request succeeds,
   *          or an error object when the request fails.
   */
  getHealth(): Observable<HealthResponse> {
    return this.http
      // Make an HTTP GET request to /api/health.
      // The generic type <HealthResponse> tells TypeScript what shape to expect back —
      // if the backend response doesn't match, we'll get compile-time warnings.
      .get<HealthResponse>(this.apiUrl)
      .pipe(
        // pipe() chains operators that transform or handle the data stream.

        // retry(1) — if the first request fails (network blip, server momentarily down),
        // automatically retry once before propagating the error.
        // Helps handle transient network issues without the user seeing an error.
        retry(1),

        // tap() — "side effect" operator: log the response for debugging without
        // modifying the data. This does NOT change what reaches the subscriber.
        tap((response) => {
          console.debug('[HealthService] Backend health response received:', response);
        }),

        // catchError — if the request fails (after the retry), transform the raw
        // HttpErrorResponse into a user-friendly format using our handler method.
        catchError(this.handleError),
      );
  }

  /**
   * handleError — converts raw HTTP errors into developer-friendly error objects.
   *
   * Why a dedicated error handler? (beginner explanation)
   * ──────────────────────────────────────────────────────
   * HttpClient throws different types of errors:
   * - Network errors: the backend is unreachable (server down, DNS failure)
   * - HTTP errors: the backend responded but with 4xx/5xx status codes
   * A centralised handler lets us differentiate, log, and format them consistently.
   *
   * @param error — the raw error object from HttpClient
   * @returns Observable that immediately emits the formatted error (so subscribers
   *          land in their error callback with useful information)
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unexpected error occurred. Please try again.';

    if (error.status === 0) {
      // status 0 = the request never reached the server.
      // Possible causes: backend not running, network offline, CORS preflight failure.
      errorMessage = `Cannot reach the backend server. Is Spring Boot running on port 8080?
                      Technical detail: ${error.message}`;
      console.error('[HealthService] Network/CORS error — backend unreachable:', error);
    } else {
      // The backend responded but with an error HTTP status code.
      // error.status = the HTTP status (e.g., 500 = Internal Server Error)
      // error.error = the response body (may contain our ApiError JSON)
      errorMessage = `Backend returned HTTP ${error.status}: ${error.error?.message ?? error.message}`;
      console.error(`[HealthService] HTTP ${error.status} error from backend:`, error.error);
    }

    // throwError returns an Observable that immediately emits an error.
    // The subscriber's error callback receives this Error object.
    return throwError(() => new Error(errorMessage));
  }
}
