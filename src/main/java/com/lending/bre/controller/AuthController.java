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


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    
    public static class AuthRequest {
        public String email;
        public String password;
        public String role; 
    }

    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        
        if (userRepository.findByEmail(request.email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        
        
        String hashedPassword = passwordEncoder.encode(request.password);
        
        
        String role = request.role != null ? request.role : "ROLE_LOAN_OFFICER";
        
        
        User user = new User(request.email, hashedPassword, role);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        
        Optional<User> optionalUser = userRepository.findByEmail(request.email);
        
        
        if (optionalUser.isPresent() && passwordEncoder.matches(request.password, optionalUser.get().getPassword())) {
            User user = optionalUser.get();
            
            
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            
            
            
            
            Cookie cookie = new Cookie("JWT_TOKEN", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            
            
            
            
            response.addCookie(cookie);
            
            
            return ResponseEntity.ok(Map.of("email", user.getEmail(), "role", user.getRole()));
        }
        
        
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        
        Cookie cookie = new Cookie("JWT_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out");
    }
}