package com.los.backend.controller;

import com.los.backend.dto.request.AttachKycDocumentRequest;
import com.los.backend.dto.request.CreateApplicantRequest;
import com.los.backend.dto.response.ApplicantResponse;
import com.los.backend.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/applicants")
@Slf4j
public class ApplicantController {

    
    private final ApplicantService applicantService;

    public ApplicantController(ApplicantService applicantService) {
        this.applicantService = applicantService;
    }

    
    
    

    
    @PostMapping
    public ResponseEntity<ApplicantResponse> createApplicant(
            @Valid @RequestBody CreateApplicantRequest request) {

        
        ApplicantResponse response = applicantService.createApplicant(request);

        
        
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()           
            .path("/{id}")                  
            .buildAndExpand(response.getId()) 
            .toUri();

        
        return ResponseEntity.created(location).body(response);
    }

    
    
    

    
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantResponse> getApplicant(@PathVariable String id) {
        return ResponseEntity.ok(applicantService.getApplicantById(id));
    }

    
    
    

    
    @PostMapping("/{id}/kyc")
    public ResponseEntity<ApplicantResponse> attachKycDocument(
            @PathVariable String id,
            @Valid @RequestBody AttachKycDocumentRequest request) {

        return ResponseEntity.ok(applicantService.attachKycDocument(id, request));
    }
}
