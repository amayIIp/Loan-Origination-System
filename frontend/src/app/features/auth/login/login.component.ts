
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';


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

  
  errorMessage = signal<string | null>(null);

  
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  
  onSubmit() {
    if (this.loginForm.invalid) return;

    
    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        
        this.errorMessage.set(null);
        
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        
        this.errorMessage.set('Invalid email or password.');
      }
    });
  }
}