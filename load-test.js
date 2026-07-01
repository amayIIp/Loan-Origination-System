// load-test.js
// Run with: k6 run load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * K6 LOAD TESTING SCRIPT
 * This script simulates 50 concurrent loan officers frantically refreshing the 
 * "All Applications" dashboard to measure the API's P50 (median) and P95 latency.
 * We use this to establish a baseline before adding database indexes, and then run 
 * it again to prove the index actually improved performance.
 */

export const options = {
  // Simulate 50 simultaneous users.
  vus: 50,
  // Run the test for 30 seconds.
  duration: '30s',
  thresholds: {
    // We want 95% of requests to complete in under 500 milliseconds.
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  // Simulate an API call with filters (status = APPROVED). 
  // This causes a full Collection Scan (COLLSCAN) in MongoDB if there is no index!
  const url = 'http://localhost:8080/api/applications?status=APPROVED';
  
  // Send the GET request.
  const res = http.get(url);

  // Assert that the server responded with a 200 OK.
  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  // Pause for a short random time to simulate human behavior before making the next request.
  sleep(Math.random() * 0.5);
}