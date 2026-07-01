package com.los.backend.config;

// Spring's web MVC configuration interface — implementing this lets us
// customise how Spring handles HTTP requests (routing, CORS, interceptors, etc.).
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig — Cross-Origin Resource Sharing (CORS) configuration.
 *
 * ─── What is CORS? (beginner explanation) ───────────────────────────────────
 * By default, web browsers enforce a "same-origin policy": a web page loaded
 * from http://localhost:4200 (Angular) is NOT allowed to make HTTP requests to
 * http://localhost:8080 (our backend) because they are on different ports —
 * the browser treats them as different "origins" and blocks the request.
 *
 * CORS is the browser standard that lets the SERVER tell the browser:
 * "It's OK — I trust requests from http://localhost:4200."
 * Without this, every Angular → Spring Boot API call would fail with a
 * "CORS policy" error in the browser console.
 *
 * ─── Security note ──────────────────────────────────────────────────────────
 * In development we allow localhost:4200. In production, replace with your
 * actual deployed frontend domain (e.g., https://los.yourcompany.com).
 * NEVER use allowedOrigins("*") in production — it opens the API to any website.
 * ────────────────────────────────────────────────────────────────────────────
 */
// @Configuration tells Spring: "This class contains setup/config logic —
// process it during startup, not as a normal REST controller."
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * addCorsMappings — registers which origins, HTTP methods, and headers are
     * allowed for cross-origin requests.
     *
     * @param registry Spring's registry object where we declare CORS rules.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry
            // Apply this CORS rule to ALL endpoints in our API (/** = wildcard path)
            .addMapping("/**")

            // Only allow requests that originate from our Angular dev server.
            // This prevents other websites from calling our API in a user's browser.
            .allowedOrigins("http://localhost:4200")

            // Allow these HTTP verbs — covers the full REST API surface:
            // GET = read data, POST = create, PUT = full update,
            // PATCH = partial update, DELETE = remove, OPTIONS = preflight check
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

            // Allow all request headers — including "Authorization" (needed for JWT
            // tokens in Phase 2) and "Content-Type" (needed for JSON request bodies).
            .allowedHeaders("*")

            // Allow the browser to send cookies or Authorization headers
            // with cross-origin requests — required for session-based or
            // JWT cookie-based auth in Phase 2. Set to false if using
            // header-based JWT only (more common in SPAs).
            .allowCredentials(true)

            // Cache the preflight OPTIONS response for 1 hour (3600 seconds).
            // This reduces the number of extra "preflight" requests the browser
            // makes — improves performance for frequent API calls.
            .maxAge(3600);
    }
}
