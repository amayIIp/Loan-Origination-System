/**
 * loan-application.component.ts — placeholder Loan Application component.
 * Full multi-step reactive form will be built in Phase 3.
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'los-loan-application',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-los-surface flex items-center justify-center">
      <div class="text-center">
        <span class="text-6xl block mb-4">📝</span>
        <h1 class="text-3xl font-bold text-los-primary mb-2">Loan Application</h1>
        <p class="text-gray-500">Multi-step application form with real-time validation — coming in Phase 3.</p>
      </div>
    </div>
  `,
})
export class LoanApplicationComponent {}
