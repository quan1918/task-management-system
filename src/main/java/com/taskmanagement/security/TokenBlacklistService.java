package com.taskmanagement.security;

import com.taskmanagement.util.RedisHealthGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * TokenBlacklistService - Redis-backed blacklist for revoked access token JTIs.
 *
 * When a user logs out, the access token's jti is stored in Redis with a TTL
 * equal to the token's remaining lifetime. Once the token would have expired
 * naturally, the Redis entry is also gone — no unbounded storage growth.
 *
 * The JwtAuthenticationFilter checks this service before accepting any token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    
    private final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final RedisHealthGuard redisHealthGuard;

    /**
     * Add a token's jti to the blacklist with a TTL
     */
    public void blacklist(String jti, Long ttlMillis) {
        if (ttlMillis <= 0) {
            // Token already expired — no need to blacklist
            log.debug("Token jti={} already expired, skipping blacklist", jti);
            return;
        }
        redisHealthGuard.executeVoid(() -> {
            String key = BLACKLIST_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "1", ttlMillis, TimeUnit.MILLISECONDS);
            log.info("Access token blacklisted: jti={}, ttl={}ms", jti, ttlMillis);
            }, "blacklist"
        );
    }

    /**
     * Check if a token's jti is blacklisted
     */
    public boolean isBlacklisted(String jti) {
        return redisHealthGuard.executeWithFallback(
            () -> Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti)),
            false,
            "isBlacklisted"
        );
    }
}
