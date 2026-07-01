
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicantDto, ApplicantResponseDto, ApplicationDto, ApplicationResponseDto } from '../models/application.models';




@Injectable({ providedIn: 'root' })
export class ApplicationService {
  
  private http = inject(HttpClient);
  
  
  private baseUrl = 'http://localhost:8080/api';

  
  createApplicant(applicant: ApplicantDto): Observable<ApplicantResponseDto> {
    
    return this.http.post<ApplicantResponseDto>(`${this.baseUrl}/applicants`, applicant);
  }

  
  createApplication(application: ApplicationDto): Observable<ApplicationResponseDto> {
    
    return this.http.post<ApplicationResponseDto>(`${this.baseUrl}/applications`, application);
  }

  
  uploadKycDocuments(applicantId: string, formData: FormData): Observable<any> {
    
    return this.http.post(`${this.baseUrl}/applicants/${applicantId}/kyc`, formData);
  }

  
  getApplication(applicationId: string): Observable<ApplicationResponseDto> {
    
    return this.http.get<ApplicationResponseDto>(`${this.baseUrl}/applications/${applicationId}`);
  }
}
