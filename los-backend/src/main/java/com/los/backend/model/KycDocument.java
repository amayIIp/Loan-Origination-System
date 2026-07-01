package com.los.backend.model;




import com.los.backend.model.enums.KycDocumentType;
import com.los.backend.model.enums.VerificationStatus;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import org.springframework.data.mongodb.core.mapping.Field;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import java.time.Instant;



@Data


@Builder


@NoArgsConstructor


@AllArgsConstructor
public class KycDocument {

    
    @NotNull(message = "Document type is required")
    
    
    @Field("document_type")
    private KycDocumentType documentType;

    
    @NotBlank(message = "File URL is required")
    @Field("file_url")
    private String fileUrl;

    
    @Field("uploaded_at")
    
    
    @Builder.Default
    private Instant uploadedAt = Instant.now();

    
    @NotNull(message = "Verification status is required")
    @Field("verification_status")
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    
    @Field("rejection_reason")
    private String rejectionReason;

    
    @Field("verified_at")
    private Instant verifiedAt;
}
