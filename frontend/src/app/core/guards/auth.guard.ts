// frontend/src/app/core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/*
 * FUNCTIONAL ROUTE GUARD:
 * This protects specific URLs (like /dashboard) from being accessed by users who aren't logged in.
 * In Angular 17, guards are just functions.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If the user's in-memory state shows they are logged in as staff...
  if (authService.isLoggedIn() && authService.isStaff()) {
    // Allow them to navigate to the route.
    return true;
  }

  // Otherwise, block the navigation and redirect them to the login page.
  router.navigate(['/login']);
  return false;
};