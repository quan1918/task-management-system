package com.taskmanagement.repository;

import com.taskmanagement.entity.AuditEntityType;
import com.taskmanagement.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository 
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    
    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByEntityTypeAndEntityId(AuditEntityType entityType, Long entityId);

    Page<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
