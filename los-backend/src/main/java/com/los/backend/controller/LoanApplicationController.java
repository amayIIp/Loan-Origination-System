package com.los.backend.controller;

import com.los.backend.dto.request.SubmitLoanApplicationRequest;
import com.los.backend.dto.request.UpdateLoanStatusRequest;
import com.los.backend.dto.response.LoanApplicationResponse;
import com.los.backend.model.enums.LoanStatus;
import com.los.backend.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/applications")
@Slf4j
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    
    
    

    
    @PostMapping
    public ResponseEntity<LoanApplicationResponse> submitApplication(
            @Valid @RequestBody SubmitLoanApplicationRequest request) {

        LoanApplicationResponse response = loanApplicationService.submitApplication(request);

        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    
    
    

    
    @GetMapping
    public ResponseEntity<LoanApplicationResponse.PagedResponse> listApplications(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        return ResponseEntity.ok(
            loanApplicationService.listApplications(status, page, size, sortBy)
        );
    }

    
    
    

    
    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponse> getApplication(@PathVariable String id) {
        return ResponseEntity.ok(loanApplicationService.getApplicationById(id));
    }

    
    
    

    
    @PatchMapping("/{id}/status")
    public ResponseEntity<LoanApplicationResponse> updateApplicationStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateLoanStatusRequest request) {

        return ResponseEntity.ok(
            loanApplicationService.updateApplicationStatus(id, request)
        );
    }
}
