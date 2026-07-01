

import {
  Component,           
  OnInit,              
  OnDestroy,           
  ChangeDetectionStrategy, 
  ChangeDetectorRef,   
  signal,              
  computed,            
} from '@angular/core';



import { CommonModule } from '@angular/common';


import { HealthService } from '../../core/services/health.service';


import { HealthResponse } from '../../core/models/health.model';



import { Subject } from 'rxjs';



import { takeUntil } from 'rxjs/operators';


@Component({
  
  selector: 'los-health',

  
  standalone: true,

  
  imports: [CommonModule],

  
  templateUrl: './health.component.html',
  styleUrls: ['./health.component.scss'],

  
  
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HealthComponent implements OnInit, OnDestroy {

  
  
  
  
  

  
  isLoading = signal<boolean>(false);

  
  healthData = signal<HealthResponse | null>(null);

  
  errorMessage = signal<string | null>(null);

  
  statusClass = computed(() => {
    const data = this.healthData(); 
    if (!data) return 'bg-gray-100 text-gray-600'; 
    return data.status === 'UP'
      ? 'bg-green-100 text-green-700 border border-green-300' 
      : 'bg-red-100 text-red-700 border border-red-300';      
  });

  
  lastChecked = signal<Date | null>(null);

  
  
  
  
  private readonly destroy$ = new Subject<void>();

  
  constructor(
    private readonly healthService: HealthService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  
  ngOnInit(): void {
    
    this.fetchHealth();
  }

  
  fetchHealth(): void {
    
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.healthData.set(null);

    this.healthService
      .getHealth()
      .pipe(
        
        
        
        takeUntil(this.destroy$),
      )
      .subscribe({
        
        next: (response: HealthResponse) => {
          this.healthData.set(response);              
          this.lastChecked.set(new Date());           
          this.isLoading.set(false);                  
          this.cdr.markForCheck();                    
        },

        
        error: (err: Error) => {
          this.errorMessage.set(err.message);         
          this.isLoading.set(false);                  
          this.cdr.markForCheck();                    
        },
      });
  }

  
  ngOnDestroy(): void {
    
    
    this.destroy$.next();
    this.destroy$.complete();
  }
}
