/**
 * tailwind.config.js — Tailwind CSS configuration file.
 *
 * What is Tailwind CSS? (beginner explanation)
 * ─────────────────────────────────────────────
 * Tailwind is a "utility-first" CSS framework. Instead of writing custom CSS
 * class names like ".loan-card { padding: 16px; background: white; }",
 * you apply small pre-built utility classes directly in your HTML:
 *   <div class="p-4 bg-white shadow rounded-lg">
 * This keeps styling consistent, eliminates unused CSS, and speeds up development.
 *
 * The "content" array below is critical — it tells Tailwind which files to scan
 * for class names. Tailwind removes ("purges") any utility class NOT found in
 * these files from the final CSS bundle, keeping it tiny in production.
 */

/** @type {import('tailwindcss').Config} */
module.exports = {

  // content — file paths Tailwind scans to find which classes are actually used.
  // "**" = any folder depth, "*" = any filename.
  // We include all Angular HTML templates and TypeScript component files
  // (TypeScript files can contain class names in string literals too).
  content: [
    "./src/**/*.{html,ts}",       // All Angular templates and component files
    "./src/**/*.component.html",  // Explicit component templates (belt-and-suspenders)
    "./src/**/*.component.ts",    // TypeScript files that may contain dynamic class strings
  ],

  // theme — customise or extend Tailwind's default design system.
  theme: {
    extend: {
      // Custom brand colour palette for the LOS platform.
      // Use these in templates as: bg-los-primary, text-los-accent, etc.
      colors: {
        // Primary brand blue — used for buttons, links, active nav items
        'los-primary': '#1e3a5f',
        // Accent teal — used for highlights, badges, success states
        'los-accent': '#0d9488',
        // Danger red — used for rejection states, error messages
        'los-danger': '#dc2626',
        // Warning amber — used for PENDING/KYC_PENDING status badges
        'los-warning': '#d97706',
        // Surface light gray — card backgrounds, form panels
        'los-surface': '#f8fafc',
      },

      // Custom font family — Inter is a clean, professional sans-serif
      // ideal for financial/enterprise dashboards.
      fontFamily: {
        // Apply with class: font-los
        'los': ['Inter', 'system-ui', 'sans-serif'],
      },

      // Custom border radius — slight rounding for a modern, card-based UI
      borderRadius: {
        'los-card': '0.75rem', // 12px — matches common fintech dashboard aesthetics
      },

      // Custom box shadow — subtle depth for cards and modals
      boxShadow: {
        'los-card': '0 2px 8px 0 rgba(30, 58, 95, 0.08)',
        'los-modal': '0 8px 32px 0 rgba(30, 58, 95, 0.18)',
      },
    },
  },

  // plugins — Tailwind plugin extensions.
  // Add @tailwindcss/forms here in a later phase for better default form styling.
  plugins: [
    // require('@tailwindcss/forms'),  // Uncomment in Phase 2 for styled form inputs
    // require('@tailwindcss/typography'), // Uncomment for rich-text content areas
  ],
};
