package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;


@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    
    private String email;
    
    
    private String password;
    
    
    private String role;
    
    
    private Instant createdAt;

    public User() {}

    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = Instant.now();
    }

    
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}