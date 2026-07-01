// frontend/src/app/app.component.ts
import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  template: `
    <!-- Main Navigation Bar -->
    <nav class="bg-indigo-600 shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex">
            <!-- App Branding -->
            <div class="flex-shrink-0 flex items-center">
              <span class="text-white font-bold text-xl">LOS System</span>
            </div>
            <!-- Applicant Public Links -->
            <div class="ml-6 flex items-center space-x-4">
              <a routerLink="/apply" class="text-indigo-100 hover:text-white px-3 py-2 rounded-md text-sm font-medium">Apply Now</a>
            </div>
          </div>
          
          <!-- Dynamic Staff Links (Role-Based UI) -->
          <div class="flex items-center space-x-4">
            <!-- If logged in as staff, show the Dashboard link. -->
            <a *ngIf="authService.isStaff()" routerLink="/dashboard" class="text-indigo-100 hover:text-white px-3 py-2 rounded-md text-sm font-medium">Staff Dashboard</a>
            
            <!-- If NOT logged in, show Staff Login link. -->
            <a *ngIf="!authService.isLoggedIn()" routerLink="/login" class="text-indigo-100 hover:text-white px-3 py-2 rounded-md text-sm font-medium">Staff Login</a>
            
            <!-- If logged in, show Logout button. -->
            <button *ngIf="authService.isLoggedIn()" (click)="logout()" class="bg-indigo-700 text-white px-3 py-2 rounded-md text-sm font-medium hover:bg-indigo-800">
              Logout ({{ authService.userRole()?.replace('ROLE_', '') }})
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Where the routed components (Wizard, Login, Dashboard) are injected -->
    <router-outlet></router-outlet>
  `
})
export class AppComponent {
  // Inject the AuthService so the HTML template can read the reactive isLoggedIn/userRole signals.
  authService = inject(AuthService);

  logout() {
    this.authService.logout();
  }
}