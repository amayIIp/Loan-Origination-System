


import { Injectable } from '@angular/core';



import { HttpClient, HttpErrorResponse } from '@angular/common/http';




import { Observable, throwError } from 'rxjs';





import { catchError, retry, tap } from 'rxjs/operators';


import { HealthResponse } from '../models/health.model';



import { environment } from '../../../environments/environment';


@Injectable({
  providedIn: 'root',
})
export class HealthService {

  
  private readonly apiUrl = `${environment.apiUrl}/health`;

  
  constructor(private readonly http: HttpClient) {}

  
  getHealth(): Observable<HealthResponse> {
    return this.http
      
      
      
      .get<HealthResponse>(this.apiUrl)
      .pipe(
        

        
        
        
        retry(1),

        
        
        tap((response) => {
          console.debug('[HealthService] Backend health response received:', response);
        }),

        
        
        catchError(this.handleError),
      );
  }

  
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unexpected error occurred. Please try again.';

    if (error.status === 0) {
      
      
      errorMessage = `Cannot reach the backend server. Is Spring Boot running on port 8080?
                      Technical detail: ${error.message}`;
      console.error('[HealthService] Network/CORS error — backend unreachable:', error);
    } else {
      
      
      
      errorMessage = `Backend returned HTTP ${error.status}: ${error.error?.message ?? error.message}`;
      console.error(`[HealthService] HTTP ${error.status} error from backend:`, error.error);
    }

    
    
    return throwError(() => new Error(errorMessage));
  }
}
