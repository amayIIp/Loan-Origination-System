// frontend/src/app/core/services/auth.service.ts
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

/*
 * AUTHENTICATION SERVICE:
 * This service manages logging in, logging out, and storing the user's role in memory.
 * 
 * SECURITY EXPLANATION:
 * We DO NOT store the JWT token in `localStorage`. LocalStorage is accessible to any JavaScript 
 * running on the page, making it highly vulnerable to Cross-Site Scripting (XSS) attacks. 
 * Instead, our backend sets an "HttpOnly Cookie". The browser automatically attaches this cookie 
 * to every request securely, and JavaScript cannot read it. 
 * We only store harmless state (like `isLoggedIn` and `role`) in Angular's memory (Signals).
 */

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private baseUrl = 'http://localhost:8080/api/auth';

  // Signals to track user state reactively across the UI.
  isLoggedIn = signal<boolean>(false);
  userRole = signal<string | null>(null);

  // Send credentials to backend.
  login(credentials: any): Observable<any> {
    // We send a POST request. The backend will return user details and set an HttpOnly cookie.
    return this.http.post<any>(`${this.baseUrl}/login`, credentials).pipe(
      tap(response => {
        // Upon success, update our in-memory state.
        this.isLoggedIn.set(true);
        this.userRole.set(response.role);
      })
    );
  }

  // Ask backend to clear the cookie, then clear memory.
  logout() {
    this.http.post(`${this.baseUrl}/logout`, {}).subscribe({
      next: () => {
        this.isLoggedIn.set(false);
        this.userRole.set(null);
        // Redirect to login page.
        this.router.navigate(['/login']);
      }
    });
  }

  // Check if the current user is an admin.
  isAdmin(): boolean {
    return this.userRole() === 'ROLE_ADMIN';
  }

  // Check if the current user is a loan officer or admin.
  isStaff(): boolean {
    return this.userRole() === 'ROLE_LOAN_OFFICER' || this.userRole() === 'ROLE_ADMIN';
  }
}