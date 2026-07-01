// frontend/src/app/features/auth/login/login.component.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/*
 * LOGIN COMPONENT:
 * Provides a UI for Loan Officers and Admins to authenticate.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Error signal to show invalid credentials message.
  errorMessage = signal<string | null>(null);

  // Define the login form with email and password fields.
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  // Handle the form submission.
  onSubmit() {
    if (this.loginForm.invalid) return;

    // Send the credentials via our AuthService.
    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        // Clear any errors.
        this.errorMessage.set(null);
        // On success, redirect the staff member to their protected dashboard.
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        // If login fails (e.g., 401 Unauthorized), show an error message.
        this.errorMessage.set('Invalid email or password.');
      }
    });
  }
}