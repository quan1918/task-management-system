package com.taskmanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * CustomAccessDeniedHandler - Handles 403 Forbidden errors
 * 
 * Triggered when:
 * - User is authenticated BUT lacks required role/permission
 * - Example: USER role trying to access ADMIN-only endpoint
 * - @PreAuthorize("hasRole('ADMIN')") fails
 * 
 * Key Difference from 401:
 * - 401 Unauthorized: Authentication failed (invalid/missing token)
 * - 403 Forbidden: Authentication succeeded but authorization failed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
        ) throws IOException, ServletException {

        log.warn("Access denied: {}", accessDeniedException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .build();
        
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
