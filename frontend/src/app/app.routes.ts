// frontend/src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'apply', loadComponent: () => import('./features/apply/application-wizard/application-wizard.component').then(m => m.ApplicationWizardComponent) },
  { path: 'status/:id', loadComponent: () => import('./features/status/status-tracker/status-tracker.component').then(m => m.StatusTrackerComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { 
    path: 'dashboard', 
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    // Apply our functional guard here. If this returns false, navigation is blocked.
    canActivate: [authGuard] 
  },
  { path: '', redirectTo: 'apply', pathMatch: 'full' },
  { path: '**', redirectTo: 'apply' }
];