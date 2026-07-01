


import http from 'k6/http';
import { check, sleep } from 'k6';



export const options = {
  
  vus: 50,
  
  duration: '30s',
  thresholds: {
    
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  
  
  const url = 'http://localhost:8080/api/applications?status=APPROVED';
  
  
  const res = http.get(url);

  
  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  
  sleep(Math.random() * 0.5);
}