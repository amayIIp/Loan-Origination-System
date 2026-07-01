package com.lending.bre.controller;

import com.lending.bre.model.User;
import com.lending.bre.repository.UserRepository;
import com.lending.bre.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

// Exposes public endpoints for authentication (login and registration).
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Ask Spring to inject dependencies.
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // A simple DTO (Data Transfer Object) for receiving login/register requests.
    public static class AuthRequest {
        public String email;
        public String password;
        public String role; // Optional, used during registration.
    }

    // Allows Admins to create new Loan Officer accounts.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        // Prevent duplicate emails.
        if (userRepository.findByEmail(request.email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        
        // Ensure the password is mathematically hashed (BCrypt) before saving to MongoDB.
        String hashedPassword = passwordEncoder.encode(request.password);
        
        // Default to a Loan Officer if no role is provided.
        String role = request.role != null ? request.role : "ROLE_LOAN_OFFICER";
        
        // Save the new staff member.
        User user = new User(request.email, hashedPassword, role);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    // Handles user login.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        // Find the user by their email.
        Optional<User> optionalUser = userRepository.findByEmail(request.email);
        
        // If the user exists and the raw password matches the BCrypt hash stored in the DB.
        if (optionalUser.isPresent() && passwordEncoder.matches(request.password, optionalUser.get().getPassword())) {
            User user = optionalUser.get();
            
            // Generate a secure JWT for the user.
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            
            // Create an HttpOnly cookie to store the JWT. 
            // WHY HTTPONLY? It prevents JavaScript (like Cross-Site Scripting attacks) from stealing the token.
            // Never store JWTs in localStorage!
            Cookie cookie = new Cookie("JWT_TOKEN", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            // In production, ALWAYS set this to true so the cookie only travels over HTTPS!
            // cookie.setSecure(true);
            
            // Ask the browser to save the cookie.
            response.addCookie(cookie);
            
            // Return safe user details (NO PASSWORD or raw token) to the frontend.
            return ResponseEntity.ok(Map.of("email", user.getEmail(), "role", user.getRole()));
        }
        
        // If login fails, return a generic 401 Unauthorized error.
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    // Endpoint to log out by clearing the cookie.
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Create an empty cookie that expires instantly to overwrite the valid one.
        Cookie cookie = new Cookie("JWT_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out");
    }
}