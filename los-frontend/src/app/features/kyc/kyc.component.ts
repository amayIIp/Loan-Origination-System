
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'los-kyc',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-los-surface flex items-center justify-center">
      <div class="text-center">
        <span class="text-6xl block mb-4">🪪</span>
        <h1 class="text-3xl font-bold text-los-primary mb-2">KYC Documents</h1>
        <p class="text-gray-500">Know Your Customer document upload and verification — coming in Phase 4.</p>
      </div>
    </div>
  `,
})
export class KycComponent {}
