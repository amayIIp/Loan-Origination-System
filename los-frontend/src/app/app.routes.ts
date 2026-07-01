/**
 * app.routes.ts — top-level route definitions for the Angular application.
 *
 * What is Angular Routing? (beginner explanation)
 * ────────────────────────────────────────────────
 * A "route" maps a URL path to a component. When the user navigates to
 * /dashboard, Angular looks up the route, finds DashboardComponent, and
 * renders it inside the <router-outlet> element in AppComponent.
 *
 * Lazy loading: instead of shipping ALL component code upfront (large initial
 * download), we load each feature's code only when the user first visits that
 * route. This dramatically improves initial page load time — critical for
 * enterprise apps with many features.
 *
 * Routes is an array of Route objects — each Route is:
 * { path: 'url-segment', loadComponent: () => import(...) }
 */
import { Routes } from '@angular/router';

/**
 * routes — the application's navigation map.
 * Each entry defines one URL path and which component to show there.
 */
export const routes: Routes = [

  // ── Default Route ────────────────────────────────────────────────────────
  // When the user visits "/" (the root URL), redirect them to /dashboard.
  // pathMatch: 'full' means the entire URL must be "/" — not just "starts with /".
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full',
  },

  // ── Health Check Route ────────────────────────────────────────────────────
  // /health — shows the HealthComponent which calls /api/health and proves
  // the Angular ↔ Spring Boot connection is working.
  // loadComponent uses a dynamic import — the component code is only downloaded
  // when the user first navigates to /health (lazy loading).
  {
    path: 'health',
    loadComponent: () =>
      import('./features/health/health.component').then(
        (m) => m.HealthComponent
      ),
    // title — sets the browser tab title automatically (Angular 14+ feature)
    title: 'System Health — LOS',
  },

  // ── Dashboard Route ───────────────────────────────────────────────────────
  // /dashboard — the main landing page after login, showing KPIs and loan summaries.
  // We'll build this component in a later phase.
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
    title: 'Dashboard — LOS',
  },

  // ── Loan Application Route ────────────────────────────────────────────────
  // /loans — multi-step loan application form (built in a later phase)
  {
    path: 'loans',
    loadComponent: () =>
      import('./features/loan-application/loan-application.component').then(
        (m) => m.LoanApplicationComponent
      ),
    title: 'Loan Application — LOS',
  },

  // ── KYC Route ────────────────────────────────────────────────────────────
  // /kyc — Know Your Customer document upload flow (built in a later phase)
  {
    path: 'kyc',
    loadComponent: () =>
      import('./features/kyc/kyc.component').then((m) => m.KycComponent),
    title: 'KYC Documents — LOS',
  },

  // ── Wildcard / 404 Route ──────────────────────────────────────────────────
  // Any URL that doesn't match the above routes falls here.
  // "**" = wildcard — catches everything not already matched.
  // We redirect to dashboard; in Phase 2 we'll add a proper 404 page.
  {
    path: '**',
    redirectTo: '/dashboard',
  },
];
