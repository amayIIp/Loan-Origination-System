/**
 * postcss.config.js — PostCSS configuration file.
 *
 * What is PostCSS? (beginner explanation)
 * ────────────────────────────────────────
 * PostCSS is a tool that transforms CSS using JavaScript plugins.
 * Angular's build system (webpack/esbuild) sends our SCSS through PostCSS
 * as a processing step before the final CSS lands in the browser.
 *
 * We need two PostCSS plugins to make Tailwind work in Angular:
 * 1. tailwindcss  — generates all the utility classes based on our config + usage scan
 * 2. autoprefixer — automatically adds vendor prefixes (-webkit-, -moz-, etc.)
 *                   so CSS properties work consistently across all browsers
 *
 * Angular reads this file automatically during build — no manual wiring needed.
 */
module.exports = {
  plugins: {
    // tailwindcss — the main Tailwind engine; reads tailwind.config.js to know
    // what classes to generate and which files to scan for class usage.
    tailwindcss: {},

    // autoprefixer — adds browser-specific prefixes so things like
    // "display: grid" or "backdrop-filter" work in older browsers too.
    // It reads the target browser list from package.json > "browserslist" if present.
    autoprefixer: {},
  },
};
