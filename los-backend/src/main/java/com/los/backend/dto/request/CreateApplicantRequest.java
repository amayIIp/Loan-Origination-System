package com.los.backend.dto.request;

import com.los.backend.model.enums.EmploymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicantRequest {

    

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be 2–100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be 1–100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address (e.g., name@domain.com)")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be exactly 10 digits (no spaces or dashes)")
    private String phone;

    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    
    @NotBlank(message = "PAN number is required")
    @Pattern(
        regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}",
        message = "PAN must be in format AAAAA9999A (5 uppercase letters, 4 digits, 1 uppercase letter)"
    )
    private String panNumber;

    

    @NotNull(message = "Address is required")
    @Valid
    private AddressRequest address;

    

    @NotNull(message = "Employment information is required")
    @Valid
    private EmploymentInfoRequest employmentInfo;

    

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressRequest {

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 must not exceed 200 characters")
        private String line1;

        @Size(max = 200, message = "Address line 2 must not exceed 200 characters")
        private String line2;   

        @NotBlank(message = "City is required")
        @Size(max = 100)
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 100)
        private String state;

        @NotBlank(message = "PIN code is required")
        @Pattern(regexp = "\\d{6}", message = "PIN code must be exactly 6 digits")
        private String pincode;

        @NotBlank(message = "Country is required")
        private String country;
    }

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentInfoRequest {

        @NotNull(message = "Employment type is required")
        private EmploymentType employmentType;

        @Size(max = 200, message = "Employer name must not exceed 200 characters")
        private String employerName;    

        
        @NotNull(message = "Monthly income is required")
        @DecimalMin(value = "1.00", message = "Monthly income must be greater than ₹0")
        @Digits(integer = 12, fraction = 2, message = "Monthly income must be a valid monetary amount")
        private BigDecimal monthlyIncome;

        
        @DecimalMin(value = "0.00", message = "Total EMI cannot be negative")
        @Digits(integer = 12, fraction = 2)
        private BigDecimal totalMonthlyEmi;

        
        @Min(value = 300, message = "Credit score must be between 300 and 900")
        @Max(value = 900, message = "Credit score must be between 300 and 900")
        private Integer creditScore;

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 60, message = "Years of experience cannot exceed 60")
        private Integer yearsOfExperience;
    }
}
