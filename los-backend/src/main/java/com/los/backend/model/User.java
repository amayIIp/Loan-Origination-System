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

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    
    @Id
    private String id;

    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, digits, underscores, and hyphens")
    @Indexed(unique = true)
    @Field("username")
    private String username;

    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Indexed(unique = true)
    @Field("email")
    private String email;

    
    @NotBlank(message = "Password hash is required")
    @Field("password_hash")
    private String passwordHash;

    
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Field("first_name")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Field("last_name")
    private String lastName;

    
    @NotNull
    @Size(min = 1, message = "User must have at least one role")
    @Field("roles")
    private List<String> roles;

    
    @Builder.Default
    @Field("is_active")
    private boolean isActive = true;

    
    @Field("last_login_at")
    private Instant lastLoginAt;

    
    @Size(max = 100)
    @Field("department")
    private String department;

    
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
