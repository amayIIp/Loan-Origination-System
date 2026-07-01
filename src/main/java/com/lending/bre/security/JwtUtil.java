package com.lending.bre.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// A utility class to generate and validate JSON Web Tokens (JWTs).
@Component
public class JwtUtil {
    
    // A secure, randomly generated secret key used to sign the JWT. 
    // In production, this MUST be loaded from an environment variable!
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // The token is valid for 1 hour (3600000 milliseconds).
    private final long EXPIRATION_TIME = 3600000;

    // Create a new JWT for an authenticated user.
    public String generateToken(String email, String role) {
        // Build the JWT with the user's email, role, and expiration date, signed with our secret key.
        return Jwts.builder()
                // 'Subject' is usually the main identifier (email).
                .setSubject(email)
                // 'Claim' allows us to attach custom data, like the user's role.
                .claim("role", role)
                // Set the exact time the token was created.
                .setIssuedAt(new Date())
                // Set the exact time the token expires.
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // Sign it cryptographically so it cannot be tampered with.
                .signWith(key)
                .compact();
    }

    // Extract the user's email from a token.
    public String extractEmail(String token) {
        // Parse the token using our secret key, extract the payload (claims), and get the Subject (email).
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    // Extract the user's role from a token.
    public String extractRole(String token) {
        // Parse the token and extract the custom "role" claim.
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }

    // Check if the token is valid (signed correctly and not expired).
    public boolean validateToken(String token) {
        try {
            // If this line executes without throwing an exception, the token is valid.
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // The token was tampered with, expired, or malformed.
            return false;
        }
    }
}