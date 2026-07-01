
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { ApplicationService } from '../../../core/services/application.service';
import { catchError, concatMap, tap, finalize } from 'rxjs/operators';
import { throwError } from 'rxjs';



@Component({
  selector: 'app-application-wizard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './application-wizard.component.html'
})
export class ApplicationWizardComponent {
  
  private fb = inject(FormBuilder);
  
  private appService = inject(ApplicationService);
  
  private router = inject(Router);

  
  currentStep = signal<number>(1);
  
  isSubmitting = signal<boolean>(false);
  
  errorMessage = signal<string | null>(null);

  
  kycFiles: { [key: string]: File } = {};
  
  kycPreviews: { [key: string]: string | null } = {};
  
  fileErrors: { [key: string]: string | null } = {};

  
  wizardForm = this.fb.group({
    
    personal: this.fb.group({
      
      name: ['', Validators.required],
      
      email: ['', [Validators.required, Validators.email]],
      
      phone: ['', Validators.required],
      
      dob: ['', [Validators.required, this.minimumAgeValidator(18)]],
      
      address: ['', Validators.required],
      
      nationalId: ['', Validators.required]
    }),
    
    
    loan: this.fb.group({
      
      amount: [0, [Validators.required, Validators.min(100)]],
      
      tenureMonths: [12, [Validators.required, Validators.min(1)]],
      
      purpose: ['', Validators.required],
      
      monthlyIncome: [0, [Validators.required, Validators.min(1)]]
    }, { validators: this.loanToIncomeWarningValidator }) 
  });

  
  minimumAgeValidator(minAge: number) {
    
    return (control: AbstractControl): ValidationErrors | null => {
      
      if (!control.value) return null;
      
      
      const dob = new Date(control.value);
      
      const ageDiffMs = Date.now() - dob.getTime();
      
      const ageDate = new Date(ageDiffMs);
      const calculatedAge = Math.abs(ageDate.getUTCFullYear() - 1970);
      
      
      if (calculatedAge < minAge) {
        return { underage: { requiredAge: minAge, actualAge: calculatedAge } };
      }
      
      return null;
    };
  }

  
  
  loanToIncomeWarningValidator(group: AbstractControl): ValidationErrors | null {
    
    const amount = group.get('amount')?.value || 0;
    
    const income = group.get('monthlyIncome')?.value || 0;
    
    
    if (income > 0 && (amount / income) > 24) {
      
      return { highRatioWarning: true };
    }
    
    return null;
  }

  
  nextStep() {
    
    this.errorMessage.set(null);

    
    if (this.currentStep() === 1) {
      
      this.wizardForm.get('personal')?.markAllAsTouched();
      
      if (this.wizardForm.get('personal')?.valid) {
        this.currentStep.set(2);
      }
    } 
    
    else if (this.currentStep() === 2) {
      this.wizardForm.get('loan')?.markAllAsTouched();
      if (this.wizardForm.get('loan')?.valid) {
        this.currentStep.set(3);
      }
    }
    
    else if (this.currentStep() === 3) {
      
      if (!this.kycFiles['idProof']) {
        this.errorMessage.set('ID Proof document is required to proceed.');
        return;
      }
      
      if (Object.values(this.fileErrors).some(err => err !== null)) {
        this.errorMessage.set('Please fix document errors before proceeding.');
        return;
      }
      
      this.currentStep.set(4);
    }
  }

  
  prevStep() {
    
    this.currentStep.update(s => Math.max(1, s - 1));
  }
  
  
  goToStep(step: number) {
    this.currentStep.set(step);
  }

  
  onFileSelect(event: Event, docType: string) {
    
    const input = event.target as HTMLInputElement;
    
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      
      
      const maxSize = 5 * 1024 * 1024;
      if (file.size > maxSize) {
        
        this.fileErrors[docType] = 'File size exceeds 5MB limit.';
        
        delete this.kycFiles[docType];
        this.kycPreviews[docType] = null;
        return;
      }

      
      const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
      if (!allowedTypes.includes(file.type)) {
        this.fileErrors[docType] = 'Only PDF, JPG, and PNG files are allowed.';
        delete this.kycFiles[docType];
        this.kycPreviews[docType] = null;
        return;
      }

      
      this.fileErrors[docType] = null;
      this.kycFiles[docType] = file;

      
      if (file.type.startsWith('image/')) {
        
        const reader = new FileReader();
        
        reader.onload = () => this.kycPreviews[docType] = reader.result as string;
        
        reader.readAsDataURL(file);
      } else {
        
        this.kycPreviews[docType] = null;
      }
    }
  }

  
  submitApplication() {
    
    if (this.wizardForm.invalid) return;

    
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    
    const personalData = this.wizardForm.value.personal as any;
    const loanData = this.wizardForm.value.loan as any;

    let createdApplicantId: string;

    
    this.appService.createApplicant(personalData).pipe(
      tap(applicantResp => {
        
        createdApplicantId = applicantResp.id;
      }),
      
      concatMap(applicantResp => {
        
        const appPayload = { ...loanData, applicantId: applicantResp.id };
        
        return this.appService.createApplication(appPayload);
      }),
      
      concatMap(applicationResp => {
        
        const formData = new FormData();
        
        Object.keys(this.kycFiles).forEach(key => {
           formData.append(key, this.kycFiles[key]);
        });
        
        return this.appService.uploadKycDocuments(createdApplicantId, formData).pipe(
          
          tap(() => applicationResp.id)
        );
      }),
      
      catchError(err => {
        
        console.error('Submission failed', err);
        
        this.errorMessage.set('Submission failed. Please check your connection or try again later.');
        
        return throwError(() => err);
      }),
      
      finalize(() => {
        
        this.isSubmitting.set(false);
      })
    ).subscribe({
      
      next: (appId: string) => {
        
        this.router.navigate(['/status', appId]);
      }
    });
  }

  
  hasError(groupName: 'personal' | 'loan', fieldName: string, errorName: string): boolean {
    const control = this.wizardForm.get(groupName)?.get(fieldName);
    
    return !!(control && control.touched && control.hasError(errorName));
  }
}
