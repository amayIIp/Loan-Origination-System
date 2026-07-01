package com.lending.bre.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {
    
    
    
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    
    private final long EXPIRATION_TIME = 3600000;

    
    public String generateToken(String email, String role) {
        
        return Jwts.builder()
                
                .setSubject(email)
                
                .claim("role", role)
                
                .setIssuedAt(new Date())
                
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                
                .signWith(key)
                .compact();
    }

    
    public String extractEmail(String token) {
        
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    
    public String extractRole(String token) {
        
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }

    
    public boolean validateToken(String token) {
        try {
            
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            
            return false;
        }
    }
}