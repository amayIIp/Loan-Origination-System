
import { Routes } from '@angular/router';


export const routes: Routes = [

  
  
  
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full',
  },

  
  
  
  
  
  {
    path: 'health',
    loadComponent: () =>
      import('./features/health/health.component').then(
        (m) => m.HealthComponent
      ),
    
    title: 'System Health — LOS',
  },

  
  
  
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
    title: 'Dashboard — LOS',
  },

  
  
  {
    path: 'loans',
    loadComponent: () =>
      import('./features/loan-application/loan-application.component').then(
        (m) => m.LoanApplicationComponent
      ),
    title: 'Loan Application — LOS',
  },

  
  
  {
    path: 'kyc',
    loadComponent: () =>
      import('./features/kyc/kyc.component').then((m) => m.KycComponent),
    title: 'KYC Documents — LOS',
  },

  
  
  
  
  {
    path: '**',
    redirectTo: '/dashboard',
  },
];
