/**
 * app.component.ts — the root Angular component.
 *
 * What is a Component? (beginner explanation)
 * ────────────────────────────────────────────
 * A component is the basic building block of an Angular application.
 * It consists of three parts:
 * 1. A TypeScript class — handles data and logic
 * 2. An HTML template — defines what the user sees
 * 3. CSS/SCSS styles — controls how it looks
 *
 * AppComponent is special — it is the ROOT component. Every other component
 * in the app is nested inside it (directly or indirectly). Think of it as
 * the outermost shell of the application.
 *
 * "Standalone" component (Angular 17+):
 * ───────────────────────────────────────
 * Traditional Angular required every component to be registered inside an NgModule.
 * Standalone components are self-contained — they declare their own imports
 * directly, making them simpler to understand and test in isolation.
 */

// Component decorator — marks this class as an Angular component and
// attaches the template, styles, and configuration to it.
import { Component } from '@angular/core';

// RouterOutlet — the Angular directive that acts as a "placeholder" in the HTML.
// When the user navigates to a route, Angular renders the matching component
// inside <router-outlet>. Without this import, <router-outlet> would be unknown.
import { RouterOutlet } from '@angular/router';

// RouterLink — lets us use [routerLink]="/path" in templates for navigation
// that works with the Angular router (vs plain <a href> which does full page reload)
import { RouterLink } from '@angular/router';

/**
 * @Component decorator — configuration metadata for this component.
 */
@Component({
  // selector — the custom HTML tag that represents this component.
  // In index.html we have <los-root></los-root> — Angular replaces it with this component.
  // "los-" prefix (set in angular.json) is our project-wide component prefix.
  selector: 'los-root',

  // standalone: true — this component manages its own dependencies (no NgModule needed)
  standalone: true,

  // imports — other Angular features this component's template directly uses.
  // We MUST import these here or Angular will throw "unknown element/directive" errors.
  imports: [
    RouterOutlet, // Enables <router-outlet> in the template
    RouterLink,   // Enables [routerLink] on <a> tags in the template
  ],

  // templateUrl — points to the separate HTML file that defines this component's UI.
  // Keeping the template in its own file keeps the TypeScript class clean and focused.
  templateUrl: './app.component.html',

  // styleUrls — scoped styles; these classes only affect THIS component's template.
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  // title — a simple property displayed in the browser tab / header.
  // In a later phase this will come from the environment file and user context.
  title = 'LOS — Loan Origination System';

  // currentYear — used in the footer copyright notice; computed once at class instantiation.
  currentYear = new Date().getFullYear();
}
