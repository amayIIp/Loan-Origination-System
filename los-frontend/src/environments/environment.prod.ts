/**
 * environment.prod.ts — production environment configuration.
 *
 * Angular's build system replaces environment.ts with this file
 * automatically when building with --configuration production.
 * Update apiUrl to your actual deployed backend URL before going live.
 */
export const environment = {
  // production: true disables Angular's dev-mode checks — smaller bundle, faster runtime
  production: true,

  // Replace this with your real production API gateway or backend URL.
  // Example: 'https://api.los.yourcompany.com'
  // NEVER leave this as localhost in a production build.
  apiUrl: 'https://api.los.yourcompany.com',

  appName: 'LOS — Loan Origination System',

  version: '1.0.0',
};
