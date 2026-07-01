package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

// Represents a staff member or user in the MongoDB "users" collection.
@Document(collection = "users")
public class User {
    // Unique identifier for the user.
    @Id
    private String id;
    
    // The user's email, which acts as their login username.
    private String email;
    
    // The Bcrypt-hashed password. NEVER store plain text passwords!
    private String password;
    
    // The role of the user, used for authorization (e.g., "ROLE_ADMIN", "ROLE_LOAN_OFFICER").
    private String role;
    
    // When the account was created.
    private Instant createdAt;

    public User() {}

    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}