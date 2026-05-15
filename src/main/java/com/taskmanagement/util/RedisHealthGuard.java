package com.taskmanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class RedisHealthGuard {
    
    public <T> T executeWithFallback(Supplier<T> operation, T fallback, String operationName) {
        try {
            return operation.get();
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.warn("[Redis unavailable] '{}' degraded - using fallback. Cause: {}", operationName, e.getMessage());
            return fallback;
        } catch (Exception e) {
            log.error("[Redis error] '{}' failed with unexpected error. Cause: {}", operationName, e.getMessage());
            return fallback;
        }
    }

    public void executeVoid(Runnable operation, String operationName) {
        executeWithFallback(() -> { 
            operation.run(); 
            return null; 
        }, null, operationName);
    }
}
