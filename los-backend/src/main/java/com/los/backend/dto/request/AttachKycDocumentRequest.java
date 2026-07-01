package com.los.backend.dto.request;

import com.los.backend.model.enums.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachKycDocumentRequest {

    
    @NotNull(message = "Document type is required (e.g., ID_PROOF, ADDRESS_PROOF, INCOME_PROOF)")
    private KycDocumentType documentType;

    
    @NotBlank(message = "File URL is required — upload the file first, then register the URL here")
    @URL(message = "File URL must be a valid URL (must start with http:// or https://)")
    private String fileUrl;

    
    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    private String originalFileName;

    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
