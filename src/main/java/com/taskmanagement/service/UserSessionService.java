package com.taskmanagement.service;

import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service chuyên xử lý session của user
 * (logout, revoke token, security events...)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserRepository userRepository;

    /**
     * Clear refresh token của user
     * 
     * REQUIRES_NEW → luôn tạo transaction mới
     * → commit ngay lập tức
     * → không bị rollback bởi AuthService
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearRefreshToken(User user) {
        user.setRefreshToken(null);
        userRepository.save(user);

        log.warn("Refresh token cleared (force logout) for user: {}", user.getUsername());
    }
}