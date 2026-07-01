package com.los.backend.dto.response;

import com.los.backend.model.enums.EmploymentType;
import com.los.backend.model.enums.KycDocumentType;
import com.los.backend.model.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantResponse {

    
    private String id;

    private String firstName;
    private String lastName;

    
    private String fullName;

    private String email;
    private String phone;
    private LocalDate dateOfBirth;

    
    private String maskedPanNumber;

    
    private AddressResponse address;

    
    private EmploymentInfoResponse employmentInfo;

    
    private List<KycDocumentResponse> kycDocuments;

    
    private boolean isActive;

    
    private Instant createdAt;

    
    private Instant updatedAt;

    

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String pincode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentInfoResponse {
        private EmploymentType employmentType;
        private String employerName;
        
        private BigDecimal monthlyIncome;
        private BigDecimal totalMonthlyEmi;
        
        private Integer creditScore;
        private Integer yearsOfExperience;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycDocumentResponse {
        private KycDocumentType documentType;
        
        private String fileUrl;
        private Instant uploadedAt;
        private VerificationStatus verificationStatus;
        
        private String rejectionReason;
        private Instant verifiedAt;
    }
}
