package com.taskmanagement.service;

import com.taskmanagement.entity.AuditAction;
import com.taskmanagement.entity.AuditEntityType;
import com.taskmanagement.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final ApplicationEventPublisher eventPublisher;

    public void logAction(Long userId, AuditAction action, AuditEntityType entityType, Long entityId, String description) {
        try {
            eventPublisher.publishEvent(
                new AuditEvent(this, userId, action, entityType, entityId, description)
            );
        } catch (Exception e) {
            log.error("Failed to publish audit event: action={}, entityType={}, entityId={} — {}",
                    action, entityType, entityId, e.getMessage());
        }
    }
}
