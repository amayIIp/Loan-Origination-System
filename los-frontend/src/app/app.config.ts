/**
 * app.config.ts — centralised Angular application configuration.
 *
 * In Angular 17+ (standalone architecture), this file replaces the old
 * AppModule's "providers" array. It is the single place where we register:
 * - The router (for multi-page navigation)
 * - The HTTP client (for making API calls to the backend)
 * - Any global services or interceptors
 *
 * ApplicationConfig (beginner explanation):
 * ─────────────────────────────────────────
 * Think of this as a "startup manifest" — Angular reads it before rendering
 * anything, sets up all the listed services, then hands control to AppComponent.
 */

// ApplicationConfig — the type that describes our app's provider configuration
import { ApplicationConfig } from '@angular/core';

// provideRouter — registers the Angular Router so <router-outlet> and
// [routerLink] directives work. We pass our routes array to it.
import { provideRouter } from '@angular/router';

// withComponentInputBinding — lets route parameters be bound directly
// to component @Input() properties (cleaner than injecting ActivatedRoute manually)
import { withComponentInputBinding } from '@angular/router';

// provideHttpClient — registers Angular's HttpClient service globally.
// HttpClient is what we use to make HTTP GET/POST/PUT/DELETE calls to the backend.
// Without this, injecting HttpClient anywhere would throw a runtime error.
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

// Our application route definitions — imported from the routes file
import { routes } from './app.routes';

// provideAnimations — enables Angular's animation system (used by transitions,
// modals, toasts, etc.). Import the "async" version to lazy-load animations
// and keep the initial bundle smaller.
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

/**
 * appConfig — the exported configuration object passed to bootstrapApplication().
 * Each entry in "providers" is a function that registers a feature globally.
 */
export const appConfig: ApplicationConfig = {
  providers: [

    // Register the router with our route definitions.
    // withComponentInputBinding() allows route params (e.g., /loans/:id)
    // to be injected as @Input() on the destination component.
    provideRouter(routes, withComponentInputBinding()),

    // Register HttpClient globally — every service that injects HttpClient
    // will share the same configured instance (connection pooling, interceptors, etc.).
    // withInterceptorsFromDi() enables DI-provided interceptors (for JWT, error handling)
    // which we'll add in Phase 2 (auth phase).
    provideHttpClient(withInterceptorsFromDi()),

    // Register Angular's animation engine asynchronously — this prevents animations
    // from blocking the initial page render (better perceived performance).
    provideAnimationsAsync(),

  ],
};
