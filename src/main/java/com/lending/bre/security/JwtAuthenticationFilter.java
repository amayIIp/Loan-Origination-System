package com.lending.bre.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// This filter intercepts every incoming HTTP request to check for a valid JWT in the cookies.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Inject our utility class for handling tokens.
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // The main logic executed for every single request.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = null;
        
        // Check all cookies sent by the frontend.
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                // If we find our specific JWT cookie, extract its value.
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        // If a token exists and it is mathematically valid.
        if (token != null && jwtUtil.validateToken(token)) {
            // Extract the user's email and role from the token.
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            // Create a Spring Security authentication object representing the logged-in user.
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.singletonList(new SimpleGrantedAuthority(role))
            );
            
            // Tell Spring Security: "This user is officially authenticated for this request."
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // Continue the request down the chain to the controller.
        filterChain.doFilter(request, response);
    }
}