/**
 * dashboard.component.ts — placeholder Dashboard component.
 * Full implementation (KPI cards, loan pipeline, charts) will be built in Phase 2.
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'los-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- Placeholder dashboard — Phase 2 will add KPI cards and loan pipeline table -->
    <div class="min-h-screen bg-los-surface flex items-center justify-center">
      <div class="text-center">
        <span class="text-6xl block mb-4">📊</span>
        <h1 class="text-3xl font-bold text-los-primary mb-2">Dashboard</h1>
        <p class="text-gray-500">Loan pipeline, KPIs, and analytics — coming in Phase 2.</p>
      </div>
    </div>
  `,
})
export class DashboardComponent {}
