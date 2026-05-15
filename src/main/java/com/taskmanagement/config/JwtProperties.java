package com.taskmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    
    private String secret;
    private Long expiration;
    private Long refreshExpiration;
    private String issuer = "task-management-system";
    private byte[] secretBytes;

    @PostConstruct
    public void init() {
        System.out.println("\n========================================");
        System.out.println(" JWT CONFIGURATION INITIALIZATION");
        System.out.println("========================================");
        
        validateSecret();
        decodeSecret();
        
        // Print configuration summary
        System.out.println(" JWT Configuration loaded successfully:");
        System.out.println("   Secret length: " + secret.length() + " characters");
        System.out.println("   Secret bytes: " + secretBytes.length + " bytes");
        System.out.println("   Access token expiration: " + formatDuration(expiration));
        System.out.println("   Refresh token expiration: " + formatDuration(refreshExpiration));
        System.out.println("   Issuer: " + issuer);
        System.out.println("========================================\n");
    }
    
    /**
     * validateSecret - Validates JWT secret configuration
     * 
     * Checks:
     * 1. Secret is not null or empty
     * 2. Secret is not using default development value (in production)
     * 3. Secret meets minimum length requirements
     * 
     * @throws IllegalStateException if validation fails
     */
    private void validateSecret() {
        // Check 1: Secret must exist
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
            "[SECURITY] JWT_SECRET environment variable is not set. " +
            "Generate one: openssl rand -base64 32");
        }
        
        // Từ chối bất kỳ secret nào trông giống default/dev
        if (secret.contains("OnlyForLocal")
                || secret.startsWith("devSecret")
                || secret.startsWith("DEV_")
                || secret.equalsIgnoreCase("changeme")
                || secret.equalsIgnoreCase("secret")) {
            throw new IllegalStateException(
                "[SECURITY] JWT_SECRET is a known weak/development placeholder. " +
                "Set a strong secret: openssl rand -base64 32");
        }

        if (secret.length() < 44) {
            // 32 bytes base64-encoded = ~44 ký tự
            throw new IllegalStateException(
                "[SECURITY] JWT_SECRET is too short: " + secret.length() + " chars. " +
                "Minimum: 44 chars (32 bytes base64). Generate: openssl rand -base64 32");
        }
    }
    
    /**
     * decodeSecret - Decodes base64 secret to byte array
     * 
     * Why decode?
     * - HMAC-SHA256 requires byte[] input
     * - Base64 string is easier to store/transfer
     * 
     * Flow:
     * Base64 string → Decode → byte[] → HMAC signing
     * 
     * Fallback:
     * If secret is not valid base64, treat as plain text (UTF-8 bytes)
     */
    private void decodeSecret() {
        try {
            this.secretBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // Plain-text secret — vẫn chấp nhận nhưng warn
            System.out.println("[SECURITY] JWT_SECRET is not valid base64. Using plain-text UTF-8. " +
                    "Prefer base64 for higher entropy: openssl rand -base64 32");
            this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "[SECURITY] Decoded JWT secret is " + secretBytes.length + " bytes. " +
                "Minimum required: 32 bytes (256 bits). Generate: openssl rand -base64 32");
        }
    }
    
    /**
     * formatDuration - Format milliseconds thành human-readable string
     * 
     * Helper method để hiển thị expiration time
     * 
     * @param millis Duration in milliseconds
     * @return Formatted string (e.g., "24 hours", "7 days")
     */
    private String formatDuration(Long millis) {
        if (millis == null) {
            return "not set";
        }
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
    
    /**
     * getSecretBytes - Returns decoded secret as byte array
     * 
     * Used by JwtTokenProvider to create SecretKey object
     * 
     * @return Decoded secret bytes (≥ 32 bytes recommended)
     */
    public byte[] getSecretBytes() {
        return secretBytes;
    }
}
