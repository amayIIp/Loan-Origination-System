/**
 * main.ts — Angular application bootstrap entry point.
 *
 * What is bootstrapping? (beginner explanation)
 * ──────────────────────────────────────────────
 * "Bootstrapping" means starting the Angular application.
 * When the browser loads index.html, it eventually runs this file.
 * bootstrapApplication() is the modern Angular 17+ way to start the app
 * without a traditional NgModule — using "standalone" components instead.
 *
 * Standalone components (Angular 17+) are self-contained — each component
 * declares its own imports rather than relying on a shared NgModule.
 * This makes the codebase simpler and enables better tree-shaking
 * (Angular can remove unused code more aggressively from the final bundle).
 */

// bootstrapApplication — the function that starts our Angular app
import { bootstrapApplication } from '@angular/platform-browser';

// The root component — the topmost component that hosts the entire app
import { AppComponent } from './app/app.component';

// appConfig — our centralised application configuration (routing, HTTP client, etc.)
import { appConfig } from './app/app.config';

// bootstrapApplication:
// 1. Creates the Angular runtime
// 2. Reads appConfig (registers routes, HTTP client, etc.)
// 3. Renders AppComponent into the <los-root> element in index.html
bootstrapApplication(AppComponent, appConfig)
  .catch((err) => {
    // If Angular fails to start (e.g., a missing provider or config error),
    // log the full error to the browser console so we can debug it.
    // In production, this could be routed to an error monitoring service (e.g., Sentry).
    console.error('Angular bootstrap failed:', err);
  });
