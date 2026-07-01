package com.los.backend.controller;


import java.time.Instant;



import java.util.LinkedHashMap;
import java.util.Map;





import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



import org.springframework.http.ResponseEntity;



@RestController



@RequestMapping("/health")
public class HealthController {

    
    
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {

        
        
        Map<String, Object> response = new LinkedHashMap<>();

        
        
        response.put("status", "UP");

        
        
        response.put("service", "los-backend");

        
        response.put("description", "Loan Origination System — Backend API");

        
        
        response.put("version", "1.0.0");

        
        
        
        
        response.put("timestamp", Instant.now().toString());

        
        
        return ResponseEntity.ok(response);
    }
}
