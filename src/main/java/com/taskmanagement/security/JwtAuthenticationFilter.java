package com.taskmanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import io.jsonwebtoken.JwtException;

/**
 * JwtAuthenticationFilter - Filter để authenticate requests với JWT token
 * 
 * Extends OncePerRequestFilter:
 * - Đảm bảo filter chỉ chạy 1 lần per request
 * - Tránh duplicate authentication
 * 
 * Responsibilities:
 * 1. Extract JWT token từ Authorization header
 * 2. Validate token (signature, expiration)
 * 3. Extract username từ token
 * 4. Load UserDetails từ database
 * 5. Create Authentication object
 * 6. Set Authentication vào SecurityContext
 * 
 * Flow:
 * Request → Extract token → Validate → Load user → Set auth → Continue chain
 * 
 * Request Header Format:
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * Security Notes:
 * - Filter chạy cho MỌI request (kể cả public endpoints)
 * - Public endpoints: Skip authentication (không có token)
 * - Protected endpoints: Require valid token
 * - Invalid token: Clear SecurityContext, return 401
 */

@Component
@RequiredArgsConstructor
@Slf4j

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * doFilterInternal - Main filter logic
     * 
     * Called cho MỌI HTTP request
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain để continue
     * @throws ServletException, IOException
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. Extract Jwt token từ Authorization header
            String jwt = extractJwtFromRequest(request);
            
            // 2. Check if token exists and is valid
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Blacklist check: Nếu token bị blacklist (đã logout), reject luôn
                String jti = jwtTokenProvider.extractJti(jwt);
                if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                    log.warn("Rejected blacklisted token: jti={}", jti);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 3. Extract username từ token
                String username = jwtTokenProvider.extractUsername(jwt);

                log.debug("Jwt Token valid for user: {}", username);

                // 4. Load UserDetails từ database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Create Authentication object
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );

                // 6. Set details (Ip address, session ID, .etc)
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 7. Set Authentication vào SecurityContext
                // SecurityContextHolder là thread-local storage 
                // Lưu authentication info cho request 
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.error("Unexpected error during JWT authentication: {}", ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
        }
        // 8. Continue filter chain
        // QUAN TRỌNG: Phải gọi filterChain.doFilter()
        // Nếu không, request sẽ bị block ở đây
        filterChain.doFilter(request, response);
    }

     /**
     * extractJwtFromRequest - Extract JWT token từ Authorization header
     * 
     * Header format:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * Example:
     * Header: "Authorization: Bearer abc123"
     * Return: "abc123"
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // Lấy header Authorization
        String bearerToken = request.getHeader("Authorization");

        // Check if header exists và có format đúng
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix
            String token = bearerToken.substring(7);
            
            log.debug("JWT token extracted from request");
            return token;
        }

        log.debug("No JWT token found in request");
        return null;
    }

    /**
     * shouldNotFilter - Quyết định có nên skip filter cho request này không
     * 
     * Use case:
     * - Skip filter cho public endpoints (login, register)
     * - Skip filter cho static resources (CSS, JS, images)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // List of public paths không cần authentication
        boolean isPublicPath = path.startsWith("/api/v1/auth/login") ||
                                 path.startsWith("/api/v1/auth/register") ||
                                 path.startsWith("/api/v1/auth/refresh") ||
                                 path.startsWith("/swagger-ui/") ||
                                 path.startsWith("/v3/api-docs") ||
                                 path.startsWith("/actuator/health") ||
                                 path.startsWith("/ws");
        if (isPublicPath) {
            log.debug("Skipping JwtAuthenticationFilter for public path: {}", path);
        }

        return isPublicPath;
    }
}