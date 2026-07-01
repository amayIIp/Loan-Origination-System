/**
 * health.component.ts — Angular component that displays the backend health status.
 *
 * This component's purpose:
 * ─────────────────────────
 * 1. On load, call HealthService.getHealth() to hit /api/health on the backend.
 * 2. Display the response (status, timestamp, version) in the template.
 * 3. Display a clear error message if the backend is unreachable.
 *
 * This component proves end-to-end connectivity:
 *   Browser → Angular (4200) → proxy → Spring Boot (8080) → response → Angular → rendered HTML
 *
 * Change Detection Strategy — OnPush (beginner explanation):
 * ────────────────────────────────────────────────────────────
 * Angular normally checks ALL components for changes on every event (click, timer, etc.).
 * "OnPush" tells Angular: "Only re-render this component when its @Input() properties change
 * OR when we explicitly call markForCheck()." This is a performance optimisation — it
 * avoids unnecessary DOM updates, especially in large applications with many components.
 */

import {
  Component,           // Marks this class as an Angular component
  OnInit,              // Lifecycle hook interface — enforces that we implement ngOnInit()
  OnDestroy,           // Lifecycle hook — enforces ngOnDestroy() for cleanup
  ChangeDetectionStrategy, // Enum with Default and OnPush options
  ChangeDetectorRef,   // Allows us to manually trigger re-rendering with OnPush strategy
  signal,              // Angular 17+ reactive primitives — simpler state management
  computed,            // Derived reactive value from signals
} from '@angular/core';

// CommonModule — provides @if, @for, @switch template control flow
// In Angular 17+ with standalone components, we import directly here
import { CommonModule } from '@angular/common';

// The service that makes the HTTP call to /api/health
import { HealthService } from '../../core/services/health.service';

// The TypeScript interface describing the response shape
import { HealthResponse } from '../../core/models/health.model';

// Subject is an RxJS utility used to emit a "signal" that cancels subscriptions —
// prevents memory leaks when the component is destroyed (navigated away from).
import { Subject } from 'rxjs';

// takeUntil — automatically unsubscribes from an Observable when the Subject emits.
// This is the standard pattern for preventing memory leaks in Angular services.
import { takeUntil } from 'rxjs/operators';

/**
 * @Component — configuration for this standalone component.
 */
@Component({
  // The custom HTML tag for this component (used in routes/parent templates)
  selector: 'los-health',

  // Standalone: manages its own imports, no NgModule required
  standalone: true,

  // Import CommonModule for @if/@for template syntax
  imports: [CommonModule],

  // Template and styles defined in separate files (separation of concerns)
  templateUrl: './health.component.html',
  styleUrls: ['./health.component.scss'],

  // OnPush — only re-render when Signals change or markForCheck() is called.
  // Safe here because all state is managed through Angular Signals.
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HealthComponent implements OnInit, OnDestroy {

  // ── Reactive State (Angular 17 Signals) ─────────────────────────────────
  // What are Signals? (beginner explanation)
  // A Signal is a reactive container for a value. When the signal's value changes,
  // Angular automatically knows which parts of the template depend on it
  // and re-renders ONLY those parts — more efficient than the old zone.js approach.

  /** Whether an HTTP request is currently in flight — drives the loading spinner */
  isLoading = signal<boolean>(false);

  /** The successful health response from the backend — null until a response arrives */
  healthData = signal<HealthResponse | null>(null);

  /** Error message string — null when no error, populated when request fails */
  errorMessage = signal<string | null>(null);

  /**
   * statusClass — a computed signal derived from healthData.
   * Computed signals automatically recalculate whenever their dependencies change.
   * This returns the Tailwind CSS class for the status badge colour.
   */
  statusClass = computed(() => {
    const data = this.healthData(); // Read the current healthData signal value
    if (!data) return 'bg-gray-100 text-gray-600'; // No data yet — neutral grey
    return data.status === 'UP'
      ? 'bg-green-100 text-green-700 border border-green-300' // UP = green badge
      : 'bg-red-100 text-red-700 border border-red-300';      // DOWN = red badge
  });

  /**
   * lastChecked — tracks when we last polled the backend.
   * Displayed in the UI so users know how fresh the data is.
   */
  lastChecked = signal<Date | null>(null);

  // ── Subscription Management ─────────────────────────────────────────────
  // destroy$ is a Subject we complete() in ngOnDestroy().
  // takeUntil(this.destroy$) in our Observable subscription automatically
  // unsubscribes when destroy$ emits — prevents memory leaks.
  private readonly destroy$ = new Subject<void>();

  /**
   * Constructor — Angular injects our dependencies here.
   *
   * @param healthService — the service that makes the HTTP call (injected by Angular DI)
   * @param cdr — ChangeDetectorRef lets us trigger re-render manually with OnPush strategy
   */
  constructor(
    private readonly healthService: HealthService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  /**
   * ngOnInit — Angular lifecycle hook called once, immediately after the component
   * is created and its inputs are set. This is the correct place to start
   * data fetching — NOT in the constructor (constructor is only for DI).
   */
  ngOnInit(): void {
    // Kick off the health check as soon as the component mounts
    this.fetchHealth();
  }

  /**
   * fetchHealth — calls HealthService to GET /api/health and updates Signals
   * with the result (or the error).
   */
  fetchHealth(): void {
    // Show the loading spinner and clear any previous error
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.healthData.set(null);

    this.healthService
      .getHealth()
      .pipe(
        // takeUntil — automatically cancels this subscription when the component
        // is destroyed (when the user navigates away). Without this, the Observable
        // would keep the component alive in memory even after it's "gone" — a memory leak.
        takeUntil(this.destroy$),
      )
      .subscribe({
        // next — called when the HTTP request succeeds
        next: (response: HealthResponse) => {
          this.healthData.set(response);              // Store the backend response
          this.lastChecked.set(new Date());           // Record the current time
          this.isLoading.set(false);                  // Hide the loading spinner
          this.cdr.markForCheck();                    // Tell Angular: re-render this component
        },

        // error — called when the HTTP request fails (network error or HTTP 4xx/5xx)
        error: (err: Error) => {
          this.errorMessage.set(err.message);         // Display the error message
          this.isLoading.set(false);                  // Hide the loading spinner
          this.cdr.markForCheck();                    // Tell Angular: re-render with error state
        },
      });
  }

  /**
   * ngOnDestroy — Angular lifecycle hook called when the component is removed from the DOM
   * (e.g., user navigates to another route).
   * We complete destroy$ here, which triggers takeUntil and cancels all subscriptions.
   */
  ngOnDestroy(): void {
    // Emit one value and then complete the Subject — this causes takeUntil to
    // unsubscribe from all Observables that used takeUntil(this.destroy$).
    this.destroy$.next();
    this.destroy$.complete();
  }
}
