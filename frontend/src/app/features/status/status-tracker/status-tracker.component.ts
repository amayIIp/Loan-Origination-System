
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ApplicationService } from '../../../core/services/application.service';
import { ApplicationResponseDto, RuleResultDto } from '../../../core/models/application.models';



@Component({
  selector: 'app-status-tracker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-tracker.component.html'
})
export class StatusTrackerComponent implements OnInit {
  
  private appService = inject(ApplicationService);
  
  private route = inject(ActivatedRoute);

  
  application = signal<ApplicationResponseDto | null>(null);
  
  isLoading = signal<boolean>(true);
  
  error = signal<string | null>(null);

  
  ngOnInit() {
    
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadApplicationStatus(id);
    } else {
      this.error.set('No application ID provided in the URL.');
      this.isLoading.set(false);
    }
  }

  
  loadApplicationStatus(id: string) {
    this.appService.getApplication(id).subscribe({
      next: (data) => {
        
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

  
  getStepNumber(status: string | undefined): number {
    switch (status) {
      case 'SUBMITTED': return 1;
      case 'UNDER_REVIEW': return 2;
      case 'APPROVED': return 4;
      case 'REJECTED': return 4;
      default: return 1;
    }
  }

  
  translateRule(rule: RuleResultDto): string {
    
    
    if (rule.passed) {
      return rule.reason;
    }

    
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
        
        return rule.reason;
    }
  }
}
