// frontend/src/app/core/services/application.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicantDto, ApplicantResponseDto, ApplicationDto, ApplicationResponseDto } from '../models/application.models';

/*
 * ANGULAR SERVICE:
 * This class handles all communication with our backend API. By isolating HTTP calls here, 
 * our UI components don't need to know the exact URLs or how to configure requests.
 * We use RxJS Observables to handle the async nature of network requests.
 */

// @Injectable means Angular can create and share this service across the whole app (providedIn: 'root').
@Injectable({ providedIn: 'root' })
export class ApplicationService {
  // Use Angular 17's inject() function to get the HttpClient instead of constructor injection.
  private http = inject(HttpClient);
  
  // The base URL where our Spring Boot backend is running.
  private baseUrl = 'http://localhost:8080/api';

  // Create a new applicant record in the database.
  createApplicant(applicant: ApplicantDto): Observable<ApplicantResponseDto> {
    // Send a POST request with the applicant data and expect an ApplicantResponseDto back.
    return this.http.post<ApplicantResponseDto>(`${this.baseUrl}/applicants`, applicant);
  }

  // Create a new loan application tied to an applicant.
  createApplication(application: ApplicationDto): Observable<ApplicationResponseDto> {
    // Send a POST request with the loan details and expect an ApplicationResponseDto back.
    return this.http.post<ApplicationResponseDto>(`${this.baseUrl}/applications`, application);
  }

  // Upload KYC (Know Your Customer) documents for a specific applicant.
  uploadKycDocuments(applicantId: string, formData: FormData): Observable<any> {
    // Send a POST request containing the multipart form data (the files).
    return this.http.post(`${this.baseUrl}/applicants/${applicantId}/kyc`, formData);
  }

  // Fetch the current status and details of an existing application by its ID.
  getApplication(applicationId: string): Observable<ApplicationResponseDto> {
    // Send a GET request to retrieve the data.
    return this.http.get<ApplicationResponseDto>(`${this.baseUrl}/applications/${applicationId}`);
  }
}
