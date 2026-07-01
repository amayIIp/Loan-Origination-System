package com.los.backend.model;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200, message = "Address line 1 must not exceed 200 characters")
    @Field("line1")
    private String line1;

    
    @Size(max = 200, message = "Address line 2 must not exceed 200 characters")
    @Field("line2")
    private String line2;

    
    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Field("city")
    private String city;

    
    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Field("state")
    private String state;

    
    @NotBlank(message = "PIN code is required")
    
    @Pattern(regexp = "\\d{6}", message = "PIN code must be exactly 6 digits")
    @Field("pincode")
    private String pincode;

    
    @NotBlank(message = "Country is required")
    @Field("country")
    @Builder.Default
    private String country = "India";
}
