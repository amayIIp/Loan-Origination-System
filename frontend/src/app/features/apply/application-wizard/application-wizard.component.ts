// frontend/src/app/features/apply/application-wizard/application-wizard.component.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { ApplicationService } from '../../../core/services/application.service';
import { catchError, concatMap, tap, finalize } from 'rxjs/operators';
import { throwError } from 'rxjs';

/*
 * REACTIVE FORMS IN ANGULAR:
 * Reactive forms let us define the structure of our form, along with all its validation rules, 
 * entirely in TypeScript. This gives us tight control over when and how validation runs, 
 * and makes it easy to track the form's state (valid, invalid, dirty, untouched).
 */

@Component({
  selector: 'app-application-wizard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './application-wizard.component.html'
})
export class ApplicationWizardComponent {
  // Angular's form builder tool to easily construct complex form groups.
  private fb = inject(FormBuilder);
  // Service to talk to our backend API.
  private appService = inject(ApplicationService);
  // Router to navigate between pages (e.g., redirecting to the status page after success).
  private router = inject(Router);

  // Keep track of which step (1-4) the user is currently on.
  currentStep = signal<number>(1);
  // Keep track of whether the form is currently submitting to show a loading spinner.
  isSubmitting = signal<boolean>(false);
  // Store any global error messages to display to the user.
  errorMessage = signal<string | null>(null);

  // Store the actual file objects selected by the user for upload.
  kycFiles: { [key: string]: File } = {};
  // Store thumbnail URLs so the user can preview the images they selected.
  kycPreviews: { [key: string]: string | null } = {};
  // Store validation errors specific to the file uploads.
  fileErrors: { [key: string]: string | null } = {};

  // Define the master form group that contains all steps as nested child groups.
  wizardForm = this.fb.group({
    // Step 1: Personal Details
    personal: this.fb.group({
      // Name is required.
      name: ['', Validators.required],
      // Email is required and must match a standard email format.
      email: ['', [Validators.required, Validators.email]],
      // Phone is required.
      phone: ['', Validators.required],
      // Date of birth is required, and must pass our custom minimum age check.
      dob: ['', [Validators.required, this.minimumAgeValidator(18)]],
      // Address is required.
      address: ['', Validators.required],
      // National ID is required.
      nationalId: ['', Validators.required]
    }),
    
    // Step 2: Loan Details
    loan: this.fb.group({
      // Loan amount is required and must be at least 100.
      amount: [0, [Validators.required, Validators.min(100)]],
      // Tenure is required and must be at least 1 month.
      tenureMonths: [12, [Validators.required, Validators.min(1)]],
      // Purpose of the loan is required.
      purpose: ['', Validators.required],
      // Declared monthly income must be at least 1.
      monthlyIncome: [0, [Validators.required, Validators.min(1)]]
    }, { validators: this.loanToIncomeWarningValidator }) // Attach a cross-field validator to the whole group.
  });

  // Custom Validator: Checks if the applicant is at least the required age.
  minimumAgeValidator(minAge: number) {
    // Return a function that Angular forms can call to check a specific control.
    return (control: AbstractControl): ValidationErrors | null => {
      // If there's no date entered yet, we don't trigger the age error (let the 'required' validator handle it).
      if (!control.value) return null;
      
      // Convert the entered string into a JavaScript Date object.
      const dob = new Date(control.value);
      // Calculate how many milliseconds old they are.
      const ageDiffMs = Date.now() - dob.getTime();
      // Convert milliseconds into a mathematical age in years (roughly).
      const ageDate = new Date(ageDiffMs);
      const calculatedAge = Math.abs(ageDate.getUTCFullYear() - 1970);
      
      // If their calculated age is less than the minimum, return an error object.
      if (calculatedAge < minAge) {
        return { underage: { requiredAge: minAge, actualAge: calculatedAge } };
      }
      // If they pass, return null (meaning no errors).
      return null;
    };
  }

  // Custom Validator: Compares loan amount to monthly income to warn if it's too high.
  // Note: This returns an error object, but we will treat it as a warning in the UI, not blocking submission.
  loanToIncomeWarningValidator(group: AbstractControl): ValidationErrors | null {
    // Extract the amount value safely.
    const amount = group.get('amount')?.value || 0;
    // Extract the income value safely.
    const income = group.get('monthlyIncome')?.value || 0;
    
    // If income is valid and the loan is more than 24 times their monthly income.
    if (income > 0 && (amount / income) > 24) {
      // Return a custom error/warning flag.
      return { highRatioWarning: true };
    }
    // Otherwise, it's fine.
    return null;
  }

  // Go to the next step in the wizard, but only if the current step is valid.
  nextStep() {
    // Clear any previous global errors.
    this.errorMessage.set(null);

    // If we are on Step 1.
    if (this.currentStep() === 1) {
      // Mark all fields in this step as touched so validation messages show up.
      this.wizardForm.get('personal')?.markAllAsTouched();
      // If the step is valid, proceed to step 2.
      if (this.wizardForm.get('personal')?.valid) {
        this.currentStep.set(2);
      }
    } 
    // If we are on Step 2.
    else if (this.currentStep() === 2) {
      this.wizardForm.get('loan')?.markAllAsTouched();
      if (this.wizardForm.get('loan')?.valid) {
        this.currentStep.set(3);
      }
    }
    // If we are on Step 3 (File Uploads).
    else if (this.currentStep() === 3) {
      // Require at least an ID proof to proceed.
      if (!this.kycFiles['idProof']) {
        this.errorMessage.set('ID Proof document is required to proceed.');
        return;
      }
      // If there are any active file validation errors, stop them from proceeding.
      if (Object.values(this.fileErrors).some(err => err !== null)) {
        this.errorMessage.set('Please fix document errors before proceeding.');
        return;
      }
      // Everything is good, go to the final review step.
      this.currentStep.set(4);
    }
  }

  // Go back to the previous step.
  prevStep() {
    // Subtract 1 from the current step, down to a minimum of 1.
    this.currentStep.update(s => Math.max(1, s - 1));
  }
  
  // Jump directly to a specific step (used from the Review page).
  goToStep(step: number) {
    this.currentStep.set(step);
  }

  // Handle file selection from the native <input type="file"> element.
  onFileSelect(event: Event, docType: string) {
    // Grab the HTML element that triggered the event.
    const input = event.target as HTMLInputElement;
    // If they actually selected a file.
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      
      // 1. Client-Side Validation: Check File Size (Max 5MB)
      const maxSize = 5 * 1024 * 1024;
      if (file.size > maxSize) {
        // Record an error for this specific document type.
        this.fileErrors[docType] = 'File size exceeds 5MB limit.';
        // Clear the bad file out of memory.
        delete this.kycFiles[docType];
        this.kycPreviews[docType] = null;
        return;
      }

      // 2. Client-Side Validation: Check File Type
      const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
      if (!allowedTypes.includes(file.type)) {
        this.fileErrors[docType] = 'Only PDF, JPG, and PNG files are allowed.';
        delete this.kycFiles[docType];
        this.kycPreviews[docType] = null;
        return;
      }

      // If validation passes, clear errors and store the file.
      this.fileErrors[docType] = null;
      this.kycFiles[docType] = file;

      // If the file is an image, generate a thumbnail preview.
      if (file.type.startsWith('image/')) {
        // Use the browser's FileReader API to read the file into a base64 string.
        const reader = new FileReader();
        // When it finishes reading, save the result to our preview dictionary.
        reader.onload = () => this.kycPreviews[docType] = reader.result as string;
        // Start the reading process.
        reader.readAsDataURL(file);
      } else {
        // Not an image (probably PDF), so no thumbnail.
        this.kycPreviews[docType] = null;
      }
    }
  }

  // The final submit function that orchestrates the complex sequence of API calls.
  submitApplication() {
    // Double-check the form is valid just in case.
    if (this.wizardForm.invalid) return;

    // Turn on the loading spinner and clear previous errors.
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    // Extract raw data from the form.
    const personalData = this.wizardForm.value.personal as any;
    const loanData = this.wizardForm.value.loan as any;

    let createdApplicantId: string;

    // Step A: Create Applicant
    this.appService.createApplicant(personalData).pipe(
      tap(applicantResp => {
        // Save the new ID the backend generated for us.
        createdApplicantId = applicantResp.id;
      }),
      // Switch over to Step B: Create Application
      concatMap(applicantResp => {
        // Attach the new applicant ID to the loan payload.
        const appPayload = { ...loanData, applicantId: applicantResp.id };
        // Trigger the second API call.
        return this.appService.createApplication(appPayload);
      }),
      // Switch over to Step C: Upload KYC Documents
      concatMap(applicationResp => {
        // Prepare a special form-data object to send files over HTTP.
        const formData = new FormData();
        // Loop through whatever files we collected.
        Object.keys(this.kycFiles).forEach(key => {
           formData.append(key, this.kycFiles[key]);
        });
        // Trigger the third API call using the applicant ID we saved earlier.
        return this.appService.uploadKycDocuments(createdApplicantId, formData).pipe(
          // Pass along the application ID so we know where to redirect next.
          tap(() => applicationResp.id)
        );
      }),
      // Catch any errors that happen in ANY of the above steps.
      catchError(err => {
        // Log to console for debugging.
        console.error('Submission failed', err);
        // Show a generic, safe error message to the user.
        this.errorMessage.set('Submission failed. Please check your connection or try again later.');
        // Re-throw so the sequence stops.
        return throwError(() => err);
      }),
      // Regardless of success or failure, run this when everything is done.
      finalize(() => {
        // Turn off the loading spinner.
        this.isSubmitting.set(false);
      })
    ).subscribe({
      // If the entire sequence succeeds without errors.
      next: (appId: string) => {
        // Redirect the user to the status tracker page, passing the new application ID in the URL.
        this.router.navigate(['/status', appId]);
      }
    });
  }

  // Helper method to make checking field errors cleaner in the HTML template.
  hasError(groupName: 'personal' | 'loan', fieldName: string, errorName: string): boolean {
    const control = this.wizardForm.get(groupName)?.get(fieldName);
    // Return true only if the control has been touched (user left the field) AND it has the specific error.
    return !!(control && control.touched && control.hasError(errorName));
  }
}
