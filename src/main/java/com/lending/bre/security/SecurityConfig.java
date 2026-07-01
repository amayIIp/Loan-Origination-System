package com.lending.bre.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

// Tells Spring this class contains core security configuration.
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    // Configure how passwords should be hashed (BCrypt is the industry standard).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Define the security rules for all HTTP routes.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF because we are using stateless JWTs (though with cookies, SameSite/CSRF tokens are recommended in full prod).
            .csrf(csrf -> csrf.disable())
            // Configure CORS to only allow our Angular frontend, preventing malicious sites from making requests.
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://localhost:4200"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true); // Must be true so cookies are sent back and forth!
                return config;
            }))
            // We do not use server-side sessions; the JWT cookie holds all state.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Define who can access what URL paths.
            .authorizeHttpRequests(auth -> auth
                // Allow anyone to access the login/register endpoints.
                .requestMatchers("/api/auth/**").permitAll()
                // Allow applicants to submit data without logging in.
                .requestMatchers(HttpMethod.POST, "/api/applicants", "/api/applications", "/api/applicants/*/kyc").permitAll()
                // Allow checking status publically if they have the ID (as per Phase 1 design).
                .requestMatchers(HttpMethod.GET, "/api/applications/*").permitAll()
                
                // Only Admins can modify the Business Rule Engine thresholds.
                .requestMatchers("/api/credit-rules/**").hasAuthority("ROLE_ADMIN")
                
                // Only staff (Loan Officers or Admins) can list all applications or update their status.
                .requestMatchers("/api/applications/**").hasAnyAuthority("ROLE_LOAN_OFFICER", "ROLE_ADMIN")
                
                // Any other route requires the user to be authenticated.
                .anyRequest().authenticated()
            )
            // Insert our custom JWT filter BEFORE the standard Spring Security username/password filter.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}