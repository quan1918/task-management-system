package com.taskmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStartupVerifier {

    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyRedisConnection() {
        log.info("========================================");
        log.info(" REDIS CONNECTION VERIFICATION");
        log.info("========================================");
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if ("PONG".equalsIgnoreCase(pong)) {
                log.info(" Redis connection: OK (received PONG)");
            } else {
                log.warn(" Redis connection: Unexpected response = {}", pong);
            }
        } catch (Exception e) {
            log.warn(" Redis connection: FAILED — {}", e.getMessage());
            log.warn(" App will run in degraded mode (blacklist + brute-force protection disabled)");
        }
        log.info("========================================");
    }
}