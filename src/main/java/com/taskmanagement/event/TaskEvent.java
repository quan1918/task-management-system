package com.taskmanagement.event;

import com.taskmanagement.dto.response.TaskResponse;
import lombok.Getter;

/**
 * Spring Application Event cho task mutations (create, update, delete)
 * Extends ApplicationEvent - required bởi ApplicationEventPublisher
 */

@Getter
public class TaskEvent extends org.springframework.context.ApplicationEvent {
    
    private final TaskEventType type;
    private final Long projectId;
    private final TaskResponse payload;
    private final Long deletedTaskId;

    // Constructor cho CREATED và UPDATED events
    public TaskEvent(Object source, TaskEventType type, Long projectId, TaskResponse payload) {
        super(source);
        this.type = type;
        this.projectId = projectId;
        this.payload = payload;
        this.deletedTaskId = null;
    }

    // Constructor cho DELETED event
    public TaskEvent(Object source, Long projectId, Long deletedTaskId) {
        super(source);
        this.type = TaskEventType.DELETED;
        this.projectId = projectId;
        this.payload = null;
        this.deletedTaskId = deletedTaskId;
    }
}
