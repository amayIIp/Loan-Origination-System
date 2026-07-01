// frontend/src/environments/environment.prod.ts

/*
 * PRODUCTION ENVIRONMENT
 * When you run `ng build --configuration=production`, Angular automatically swaps out 
 * environment.ts for THIS file. 
 */
export const environment = {
  // Flag indicating this IS the production build.
  production: true,
  
  // Notice this is relative. By using just '/api', we tell the browser to send requests 
  // to the EXACT SAME domain that is hosting the frontend. If the frontend is hosted on Vercel, 
  // we will configure Vercel to route '/api' traffic to our Render backend!
  // Alternatively, if you don't use a Vercel proxy, you hardcode your Render URL here: 
  // apiUrl: 'https://los-backend.onrender.com/api'
  apiUrl: 'https://YOUR_BACKEND_APP_NAME.onrender.com/api'
};
