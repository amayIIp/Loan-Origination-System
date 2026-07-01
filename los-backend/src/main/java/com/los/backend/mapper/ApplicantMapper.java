package com.los.backend.mapper;

import com.los.backend.dto.request.CreateApplicantRequest;
import com.los.backend.dto.response.ApplicantResponse;
import com.los.backend.model.Address;
import com.los.backend.model.Applicant;
import com.los.backend.model.EmploymentInfo;
import com.los.backend.model.KycDocument;
import com.los.backend.model.enums.VerificationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class ApplicantMapper {

    

    
    public Applicant toModel(CreateApplicantRequest req) {
        
        Address address = Address.builder()
            .line1(req.getAddress().getLine1())
            .line2(req.getAddress().getLine2())
            .city(req.getAddress().getCity())
            .state(req.getAddress().getState())
            .pincode(req.getAddress().getPincode())
            
            
            .country(req.getAddress().getCountry() != null ? req.getAddress().getCountry() : "India")
            .build();

        
        CreateApplicantRequest.EmploymentInfoRequest empReq = req.getEmploymentInfo();
        EmploymentInfo employmentInfo = EmploymentInfo.builder()
            .employmentType(empReq.getEmploymentType())
            .employerName(empReq.getEmployerName())
            .monthlyIncome(empReq.getMonthlyIncome())
            
            .totalMonthlyEmi(
                empReq.getTotalMonthlyEmi() != null
                    ? empReq.getTotalMonthlyEmi()
                    : BigDecimal.ZERO
            )
            .creditScore(empReq.getCreditScore())
            .yearsOfExperience(empReq.getYearsOfExperience())
            .build();

        
        return Applicant.builder()
            .firstName(req.getFirstName().trim())      
            .lastName(req.getLastName().trim())
            .email(req.getEmail().toLowerCase().trim()) 
            .phone(req.getPhone().trim())
            .dateOfBirth(req.getDateOfBirth())
            .panNumber(req.getPanNumber().toUpperCase().trim()) 
            .address(address)
            .employmentInfo(employmentInfo)
            
            
            .build();
    }

    

    
    public ApplicantResponse toResponse(Applicant applicant) {
        return ApplicantResponse.builder()
            .id(applicant.getId())
            .firstName(applicant.getFirstName())
            .lastName(applicant.getLastName())
            .fullName(applicant.getFullName())   
            .email(applicant.getEmail())
            .phone(applicant.getPhone())
            .dateOfBirth(applicant.getDateOfBirth())
            
            .maskedPanNumber(maskPan(applicant.getPanNumber()))
            .address(mapAddress(applicant.getAddress()))
            .employmentInfo(mapEmploymentInfo(applicant.getEmploymentInfo()))
            .kycDocuments(mapKycDocuments(applicant.getKycDocuments()))
            .isActive(applicant.isActive())
            .createdAt(applicant.getCreatedAt())
            .updatedAt(applicant.getUpdatedAt())
            .build();
    }

    
    public List<ApplicantResponse> toResponseList(List<Applicant> applicants) {
        if (applicants == null) return Collections.emptyList();
        
        return applicants.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    

    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) return "N/A";
        
        return "XXXXXX" + pan.substring(6);
    }

    
    private ApplicantResponse.AddressResponse mapAddress(Address address) {
        if (address == null) return null;
        return ApplicantResponse.AddressResponse.builder()
            .line1(address.getLine1())
            .line2(address.getLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .build();
    }

    
    private ApplicantResponse.EmploymentInfoResponse mapEmploymentInfo(EmploymentInfo info) {
        if (info == null) return null;
        return ApplicantResponse.EmploymentInfoResponse.builder()
            .employmentType(info.getEmploymentType())
            .employerName(info.getEmployerName())
            .monthlyIncome(info.getMonthlyIncome())
            .totalMonthlyEmi(info.getTotalMonthlyEmi())
            .creditScore(info.getCreditScore())
            .yearsOfExperience(info.getYearsOfExperience())
            .build();
    }

    
    private List<ApplicantResponse.KycDocumentResponse> mapKycDocuments(List<KycDocument> docs) {
        if (docs == null) return Collections.emptyList();
        return docs.stream()
            .map(doc -> ApplicantResponse.KycDocumentResponse.builder()
                .documentType(doc.getDocumentType())
                .fileUrl(doc.getFileUrl())
                .uploadedAt(doc.getUploadedAt())
                .verificationStatus(doc.getVerificationStatus())
                .rejectionReason(doc.getRejectionReason())
                .verifiedAt(doc.getVerifiedAt())
                .build())
            .collect(Collectors.toList());
    }

    
    public LoanApplicationResponse.ApplicantSummary buildApplicantSummary(Applicant applicant) {
        if (applicant == null) return null;

        
        long verifiedCount = applicant.getKycDocuments() == null ? 0L :
            applicant.getKycDocuments().stream()
                .filter(d -> VerificationStatus.VERIFIED.equals(d.getVerificationStatus()))
                .count();

        
        long pendingOrRejectedCount = applicant.getKycDocuments() == null ? 0L :
            applicant.getKycDocuments().stream()
                .filter(d -> !VerificationStatus.VERIFIED.equals(d.getVerificationStatus()))
                .count();

        return LoanApplicationResponse.ApplicantSummary.builder()
            .id(applicant.getId())
            .fullName(applicant.getFullName())
            .email(applicant.getEmail())
            .phone(applicant.getPhone())
            .maskedPanNumber(maskPan(applicant.getPanNumber()))
            .creditScore(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getCreditScore() : null)
            .monthlyIncome(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getMonthlyIncome() : null)
            .totalMonthlyEmi(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getTotalMonthlyEmi() : BigDecimal.ZERO)
            .employmentType(applicant.getEmploymentInfo() != null
                ? applicant.getEmploymentInfo().getEmploymentType().name() : null)
            .verifiedKycDocumentCount(verifiedCount)
            .pendingOrRejectedKycCount(pendingOrRejectedCount)
            .build();
    }
}




