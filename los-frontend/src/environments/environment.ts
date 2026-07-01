/**
 * environment.ts — development environment configuration.
 *
 * What are environment files? (beginner explanation)
 * ───────────────────────────────────────────────────
 * Different environments (local dev, staging, production) need different settings —
 * especially API URLs. Angular's build system swaps this file with environment.prod.ts
 * automatically when we run "ng build --configuration production".
 *
 * We import this file in services that need to know the API base URL,
 * so changing it here changes it everywhere at once — no hunting through files.
 */
export const environment = {
  // production: false tells the Angular runtime to enable dev-mode checks
  // (extra warnings, detailed error messages). Set to true only in prod.
  production: false,

  // apiUrl — the base URL for all backend REST API calls.
  // In development, Angular's proxy (proxy.conf.json) forwards /api/* to port 8080.
  // We use a relative path so the proxy intercepts it correctly.
  apiUrl: '/api',

  // appName — displayed in the browser tab title and header components
  appName: 'LOS — Loan Origination System',

  // version — shown in the UI footer; bump this with each release
  version: '1.0.0-dev',
};
