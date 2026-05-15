package com.taskmanagement.event;

import com.taskmanagement.entity.AuditAction;
import com.taskmanagement.entity.AuditEntityType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditEvent extends ApplicationEvent {
    private final Long userId;
    private final AuditAction action;
    private final AuditEntityType entityType;
    private final Long entityId;
    private final String description;

    public AuditEvent(Object source, Long userId, AuditAction action, AuditEntityType entityType, Long entityId, String description) {
        super(source);
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
    }
}
