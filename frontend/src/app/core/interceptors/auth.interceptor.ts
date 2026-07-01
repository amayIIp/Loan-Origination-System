// frontend/src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';

/*
 * FUNCTIONAL HTTP INTERCEPTOR:
 * In Angular 17+, interceptors are plain functions. This function intercepts every single HTTP 
 * request leaving our application before it reaches the network.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // We clone the outgoing request and modify it.
  const modifiedReq = req.clone({
    // CRUCIAL FOR SECURITY: `withCredentials: true` tells the browser to include our secure 
    // HttpOnly cookies (which contain our JWT) in this cross-origin request to the backend.
    withCredentials: true
  });
  
  // Pass the modified request down the chain to actually be sent.
  return next(modifiedReq);
};