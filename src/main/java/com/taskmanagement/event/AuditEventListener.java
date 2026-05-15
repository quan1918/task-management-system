package com.taskmanagement.event;

import com.taskmanagement.entity.AuditLog;
import com.taskmanagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {
    
    private final AuditLogRepository auditLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(event.getUserId())
                    .action(event.getAction())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .description(event.getDescription())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: action={}, entityType={}, entityId={}, userId={}",
                    event.getAction(), event.getEntityType(),
                    event.getEntityId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to save audit log: action={}, entityType={}, entityId={} — {}",
                    event.getAction(), event.getEntityType(),
                    event.getEntityId(), e.getMessage());
        }
    }
}
