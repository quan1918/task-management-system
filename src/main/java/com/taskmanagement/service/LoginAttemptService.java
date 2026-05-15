package com.taskmanagement.service;

import com.taskmanagement.util.RedisHealthGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final String ATTEMPT_PREFIX = "login_attempts:";
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String LOCKED_PREFIX = "login_locked:";

    private final StringRedisTemplate redisTemplate;
    private final RedisHealthGuard redisHealthGuard;

    /* Gọi khi login thất bại */
    public void recordFailure(String username) {
        redisHealthGuard.executeVoid(() -> {
            String attemptsKey = ATTEMPT_PREFIX + username;
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, LOCK_DURATION);
            if (attempts != null && attempts >= MAX_ATTEMPTS) {
                redisTemplate.opsForValue().set(LOCKED_PREFIX + username, "1", LOCK_DURATION);
                log.warn("Account locked due to {} failed attempts: {}", MAX_ATTEMPTS, username);
            }
        }, "recordFailure");
    }

    /* Gọi khi login thành công */
    public void recordSuccess(String username) {
        redisHealthGuard.executeVoid(() -> {
            redisTemplate.delete(ATTEMPT_PREFIX + username);
            redisTemplate.delete(LOCKED_PREFIX + username);
        }, "recordSuccess");
    }

    /* Kiểm tra trước khi authentication */
    public boolean isLocked(String username) {
        return redisHealthGuard.executeWithFallback(
            () -> Boolean.TRUE.equals(redisTemplate.hasKey(LOCKED_PREFIX + username)),
            false,
            "isLocked"
        );
    }

    /* Trả về số phút còn lại bị khoá */
    public long getRemainingLockSeconds(String username) {
        return redisHealthGuard.executeWithFallback(
            () -> {
                Long ttl = redisTemplate.getExpire(LOCKED_PREFIX + username);
                return ttl != null && ttl > 0 ? ttl : 0;
            },
            0L,
            "getRemainingLockSeconds"
        );
    }
}
