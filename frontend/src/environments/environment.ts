// frontend/src/environments/environment.ts

/*
 * LOCAL DEVELOPMENT ENVIRONMENT
 * This file is used when you run `ng serve` or a standard non-production Docker build.
 * It points the Angular app to the local backend.
 */
export const environment = {
  // Flag indicating this is NOT the production build.
  production: false,
  
  // Point to the local backend running via Docker Compose (or your IDE) on port 8080.
  // Note: Since we set up Nginx proxy in Docker, this could also just be '/api' locally, 
  // but pointing straight to 8080 is standard for local ng serve development.
  apiUrl: 'http://localhost:8080/api'
};
