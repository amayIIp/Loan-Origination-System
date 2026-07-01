package com.los.backend.service;

import com.los.backend.dto.request.AttachKycDocumentRequest;
import com.los.backend.dto.request.CreateApplicantRequest;
import com.los.backend.dto.response.ApplicantResponse;
import com.los.backend.exception.DuplicateResourceException;
import com.los.backend.exception.ResourceNotFoundException;
import com.los.backend.mapper.ApplicantMapper;
import com.los.backend.model.Applicant;
import com.los.backend.model.KycDocument;
import com.los.backend.repository.ApplicantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;


@Service  
@Slf4j    
public class ApplicantService {

    
    private final ApplicantRepository applicantRepository;

    
    private final ApplicantMapper applicantMapper;

    
    public ApplicantService(ApplicantRepository applicantRepository,
                            ApplicantMapper applicantMapper) {
        this.applicantRepository = applicantRepository;
        this.applicantMapper     = applicantMapper;
    }

    
    
    

    
    public ApplicantResponse createApplicant(CreateApplicantRequest request) {
        
        
        
        
        log.info("[ApplicantService] CREATE APPLICANT attempt | email={}", request.getEmail());

        
        
        
        
        if (applicantRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            log.warn("[ApplicantService] Duplicate email detected | email={}", request.getEmail());
            throw new DuplicateResourceException("Applicant", "email", request.getEmail());
        }

        
        
        
        if (applicantRepository.existsByPanNumber(request.getPanNumber().toUpperCase().trim())) {
            
            
            log.warn("[ApplicantService] Duplicate PAN detected | email={}", request.getEmail());
            throw new DuplicateResourceException("Applicant", "panNumber",
                "***MASKED*** (PAN already registered)");
        }

        
        
        
        
        
        Applicant applicant = applicantMapper.toModel(request);

        
        
        
        Applicant savedApplicant = applicantRepository.save(applicant);

        
        log.info("[ApplicantService] CREATE APPLICANT success | applicantId={} | email={}",
                 savedApplicant.getId(), savedApplicant.getEmail());

        
        
        
        return applicantMapper.toResponse(savedApplicant);
    }

    
    
    

    
    public ApplicantResponse getApplicantById(String applicantId) {
        log.debug("[ApplicantService] GET APPLICANT | applicantId={}", applicantId);

        Applicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", "id", applicantId));

        return applicantMapper.toResponse(applicant);
    }

    
    public Applicant getApplicantModelById(String applicantId) {
        return applicantRepository.findById(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", "id", applicantId));
    }

    
    
    

    
    public ApplicantResponse attachKycDocument(String applicantId, AttachKycDocumentRequest request) {
        log.info("[ApplicantService] ATTACH KYC DOCUMENT | applicantId={} | docType={}",
                 applicantId, request.getDocumentType());

        
        Applicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", "id", applicantId));

        
        KycDocument newDoc = KycDocument.builder()
            .documentType(request.getDocumentType())
            .fileUrl(request.getFileUrl())
            .uploadedAt(Instant.now())   
            
            
            
            .rejectionReason(null)       
            .verifiedAt(null)            
            .build();

        
        
        if (applicant.getKycDocuments() == null) {
            applicant.setKycDocuments(new ArrayList<>());
        }

        
        applicant.getKycDocuments().add(newDoc);

        
        
        Applicant updated = applicantRepository.save(applicant);

        log.info("[ApplicantService] ATTACH KYC DOCUMENT success | applicantId={} | docType={} | totalDocs={}",
                 applicantId, request.getDocumentType(), updated.getKycDocuments().size());

        return applicantMapper.toResponse(updated);
    }

    
    public boolean existsById(String applicantId) {
        return applicantRepository.existsById(applicantId);
    }
}
