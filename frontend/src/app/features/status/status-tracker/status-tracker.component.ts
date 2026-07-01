// frontend/src/app/features/status/status-tracker/status-tracker.component.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ApplicationService } from '../../../core/services/application.service';
import { ApplicationResponseDto, RuleResultDto } from '../../../core/models/application.models';

/*
 * STATUS TRACKER COMPONENT:
 * This page allows an applicant to check the status of their loan by passing an ID in the URL.
 * It translates complex backend technical jargon into friendly, readable text.
 */

@Component({
  selector: 'app-status-tracker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-tracker.component.html'
})
export class StatusTrackerComponent implements OnInit {
  // Service to talk to the backend.
  private appService = inject(ApplicationService);
  // Service to read data from the current URL (e.g., getting the :id part).
  private route = inject(ActivatedRoute);

  // A signal to hold the application data once we fetch it.
  application = signal<ApplicationResponseDto | null>(null);
  // A signal to hold loading state.
  isLoading = signal<boolean>(true);
  // A signal for error messages if the ID is wrong or server is down.
  error = signal<string | null>(null);

  // Angular lifecycle hook that runs when the component first loads.
  ngOnInit() {
    // Look at the URL and grab the 'id' parameter.
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadApplicationStatus(id);
    } else {
      this.error.set('No application ID provided in the URL.');
      this.isLoading.set(false);
    }
  }

  // Fetch the data from the backend.
  loadApplicationStatus(id: string) {
    this.appService.getApplication(id).subscribe({
      next: (data) => {
        // Save the successful response data to our signal.
        this.application.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load status', err);
        this.error.set('Could not find application. Please check your tracking ID.');
        this.isLoading.set(false);
      }
    });
  }

  // Helper method: Map internal backend state machine statuses to a numeric progress step (1-4).
  getStepNumber(status: string | undefined): number {
    switch (status) {
      case 'SUBMITTED': return 1;
      case 'UNDER_REVIEW': return 2;
      case 'APPROVED': return 4;
      case 'REJECTED': return 4;
      default: return 1;
    }
  }

  // Helper method: Translate technical rule names into human-readable explanations.
  translateRule(rule: RuleResultDto): string {
    // If they passed the rule, the default backend reason is usually fine. 
    // But if they failed, we want to soften the language.
    if (rule.passed) {
      return rule.reason;
    }

    // Map specific failure reasons to friendly text.
    switch (rule.ruleName) {
      case 'DebtToIncomeRatioRule':
        return 'Your current monthly obligations are high relative to your income.';
      case 'CreditScoreRule':
        return 'Your credit history does not currently meet our minimum requirements.';
      case 'AgeEligibilityRule':
        return 'You do not meet the minimum age requirement for this loan product.';
      case 'LoanToIncomeRatioRule':
        return 'The requested loan amount is too large compared to your declared income.';
      case 'EmploymentStatusRule':
        return 'We require active employment to proceed with this application.';
      default:
        // Fallback to whatever the backend sent if we don't recognize the rule.
        return rule.reason;
    }
  }
}
