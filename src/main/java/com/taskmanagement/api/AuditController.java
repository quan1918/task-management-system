package com.taskmanagement.api;

import com.taskmanagement.dto.response.AuditLogResponse;
import com.taskmanagement.dto.response.PagedResponse;
import com.taskmanagement.entity.AuditEntityType;
import com.taskmanagement.entity.AuditLog;
import com.taskmanagement.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {
    
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Fetching audit logs with filters: userId={}, entityType={}, entityId={}, startDate={}, endDate={}",
                userId, entityType, entityId, startDate, endDate);
        
        Specification<AuditLog> spec = buildSpecification(userId, entityType != null ? entityType.name() : null,
                entityId, startDate, endDate);

        Page<AuditLogResponse> page = auditLogRepository.findAll(spec, pageable)
                .map(AuditLogResponse::from);
        
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    private Specification<AuditLog> buildSpecification(Long userId, String entityType,
                                                        Long entityId, LocalDateTime startDate,
                                                        LocalDateTime endDate) {

        Specification<AuditLog> spec = Specification.where(null);
        
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }

        if (entityType != null) {
            try {
                AuditEntityType type = AuditEntityType.valueOf(entityType.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), type));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid entityType filter: {}", entityType);
            }
        }

        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }
        return spec;
    }
}
