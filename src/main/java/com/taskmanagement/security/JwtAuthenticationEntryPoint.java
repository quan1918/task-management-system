package com.taskmanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.dto.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JwtAuthenticationEntryPoint - Custom 401 Unauthorized handler
 * 
 * Triggered when:
 * - No JWT token in request header (protected endpoint)
 * - JWT token invalid (expired, malformed, wrong signature)
 * - Authentication failed
 * 
 * Flow:
 * Request (no token) → JwtAuthenticationFilter (skip authentication)
 *                   → Controller checks SecurityContext
 *                   → No Authentication found
 *                   → JwtAuthenticationEntryPoint.commence()
 *                   → Return 401 with custom error response
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * commence - Handle unauthorized access
     * 
     * Called automatically by Spring Security khi:
     * - User access protected endpoint without authentication
     * - JWT token validation fails
     */
    @Override
    public void commence(HttpServletRequest request, 
                            HttpServletResponse response,
                            AuthenticationException authException
                        ) throws IOException, ServletException {

        log.warn("Unauthorized access attempt: {} - {}", request.getRequestURI(), authException.getMessage());
    
        // Build error response
        ErrorResponse errorResponse = ErrorResponse.builder()
            .success(false)
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message("Authentication is required to access this resource.")
            .path(request.getRequestURI())
            .build();
        
        // Set response properties
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Write error response as JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
