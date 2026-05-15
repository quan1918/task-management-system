package com.taskmanagement.security;

import com.taskmanagement.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JwtTokenProvider - Core component để tạo và validate JWT tokens
 * 
 * Responsibilities:
 * 1. Generate JWT access token từ user authentication
 * 2. Generate JWT refresh token
 * 3. Validate token signature và expiration
 * 4. Extract claims (username, roles) từ token
 * 
 * Security Notes:
 * - Dùng HMAC-SHA256 algorithm (HS256)
 * - Secret key ≥ 256 bits (32 bytes)
 * - Token signed để đảm bảo integrity (không bị tamper)
 * - Token có expiration để limit lifetime
 * 
 * Flow:
 * 1. User login → AuthService call generateToken()
 * 2. Token sent to client
 * 3. Client gửi token trong header: "Authorization: Bearer <token>"
 * 4. JwtAuthenticationFilter call validateToken() và extractUsername()
 * 5. Spring Security load user và set authentication context
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;

    /**
     * Secret key để sign tokens
     * 
     * PHẢI dùng SecretKey object (không phải String)
     * Tạo từ byte[] secret trong JwtProperties
     */
    private SecretKey secretKey;

    /**
     * PostConstruct - Initialize secret key sau khi bean được tạo
     * 
     * Tại sao ở đây?
     * - jwtProperties.getSecretBytes() cần được inject trước
     * - SecretKey creation cần chạy 1 lần duy nhất
     * - Fail fast nếu secret invalid
     */
    @PostConstruct
    public void init() {
        try {
            // Tạo SecretKey từ byte array
            this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretBytes());

            log.info("JWT Secret Key initialized successfully");
            log.debug("Secret key algorithm: {}", secretKey.getAlgorithm());
            log.debug("Secret key format: {}", secretKey.getFormat());
        } catch (Exception e) {
            log.error("Failed to initialize JWT Secret Key", e);
            throw new IllegalStateException("Invalid JWT secret configuration", e);
        }
    }

    /**
     * generateToken - Tạo JWT access token từ Authentication object
     * 
     * Token sẽ chứa:
     * - sub (subject): username
     * - roles: comma-separated roles (ROLE_ADMIN,ROLE_USER)
     * - iat (issued at): thời điểm tạo token
     * - exp (expiration): thời điểm token hết hạn
     * - iss (issuer): "task-management-system"
     */
    public String generateAccessToken(Authentication authentication) {
        
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        return buildAccessToken(authentication.getName(), roles);
    }
    
    public String generateAccessToken(String username, String roles) {
        return buildAccessToken(username, roles);
    }

    private String buildAccessToken(String username, String roles) {
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        String jti = UUID.randomUUID().toString(); 

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(secretKey)
                .compact();
    }

    /**
     * generateRefreshToken - Tạo refresh token với expiration dài hơn
     * 
     * Refresh token dùng để:
     * - Lấy access token mới khi access token hết hạn
     * - Tránh user phải login lại mỗi 24h
     * 
     * Khác với access token:
     * - Chỉ chứa username (KHÔNG chứa roles)
     * - Expiration dài hơn (7 days vs 24 hours)
     * - Chỉ dùng cho endpoint /api/auth/refresh
     */
    public String generateRefreshToken(Authentication authentication) {
        return buildRefreshToken(authentication.getName());
    }

    public String generateRefreshToken(String username) {
        return buildRefreshToken(username);
    }

    private String buildRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(secretKey)
                .compact();
    }
    /**
     * validateToken - Validate JWT token
     * 
     * Kiểm tra:
     * 1. Signature có hợp lệ không (token có bị tamper không)
     * 2. Token có hết hạn chưa (expired?)
     * 3. Format có đúng không (malformed?)
     * 
     * Exceptions handled:
     * - MalformedJwtException: Token format sai
     * - ExpiredJwtException: Token đã hết hạn
     * - UnsupportedJwtException: Token type không support
     * - IllegalArgumentException: Token null hoặc empty
     * - SignatureException: Signature không match (token bị sửa)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException | io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public String extractJti(String token) {
        try {
            return extractAllClaims(token).getId();
        } catch (Exception e) {
            log.error("Failed to extract jti from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractAllClaims(token).getExpiration();
        } catch (Exception e) {
            log.error("Failed to extract expiration from JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * extractAllClaims - Extract tất cả claims từ token
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.error("Failed to extract claims from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * getTokenExpirationInMillis - Lấy thời gian còn lại của token
     */
    public long getTokenExpirationInMillis(String token) {
        try {
            Date expiration = extractExpiration(token);
            if (expiration == null) {
                return 0;
            }
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            log.error("Failed to get JWT token expiration in millis: {}", e.getMessage());
            return 0;
        }
    }
}
