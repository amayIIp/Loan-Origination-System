



import { Component } from '@angular/core';




import { RouterOutlet } from '@angular/router';



import { RouterLink } from '@angular/router';


@Component({
  
  
  
  selector: 'los-root',

  
  standalone: true,

  
  
  imports: [
    RouterOutlet, 
    RouterLink,   
  ],

  
  
  templateUrl: './app.component.html',

  
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  
  
  title = 'LOS — Loan Origination System';

  
  currentYear = new Date().getFullYear();
}
