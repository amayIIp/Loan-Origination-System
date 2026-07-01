
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';



@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private baseUrl = 'http://localhost:8080/api/auth';

  
  isLoggedIn = signal<boolean>(false);
  userRole = signal<string | null>(null);

  
  login(credentials: any): Observable<any> {
    
    return this.http.post<any>(`${this.baseUrl}/login`, credentials).pipe(
      tap(response => {
        
        this.isLoggedIn.set(true);
        this.userRole.set(response.role);
      })
    );
  }

  
  logout() {
    this.http.post(`${this.baseUrl}/logout`, {}).subscribe({
      next: () => {
        this.isLoggedIn.set(false);
        this.userRole.set(null);
        
        this.router.navigate(['/login']);
      }
    });
  }

  
  isAdmin(): boolean {
    return this.userRole() === 'ROLE_ADMIN';
  }

  
  isStaff(): boolean {
    return this.userRole() === 'ROLE_LOAN_OFFICER' || this.userRole() === 'ROLE_ADMIN';
  }
}