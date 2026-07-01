package com.los.backend.model;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;




import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Document(collection = "applicants")
public class Applicant {

    
    @Id
    private String id;

    

    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Field("first_name")
    private String firstName;

    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Field("last_name")
    private String lastName;

    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Indexed(unique = true)
    @Field("email")
    private String email;

    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be a 10-digit number")
    @Field("phone")
    private String phone;

    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Field("date_of_birth")
    private LocalDate dateOfBirth;

    
    @NotBlank(message = "PAN number is required")
    
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "PAN must be in the format AAAAA9999A")
    @Indexed(unique = true)
    @Field("pan_number")
    private String panNumber;

    

    
    @NotNull(message = "Address is required")
    @Valid
    @Field("address")
    private Address address;

    
    @NotNull(message = "Employment information is required")
    @Valid
    @Field("employment_info")
    private EmploymentInfo employmentInfo;

    
    @Builder.Default
    @Field("kyc_documents")
    private List<@Valid KycDocument> kycDocuments = new ArrayList<>();

    
    
    

    
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    
    @Builder.Default
    @Field("is_active")
    private boolean isActive = true;

    

    
    public String getFullName() {
        
        return firstName + " " + lastName;
    }
}
