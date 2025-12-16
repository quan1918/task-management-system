# Event Layer

## 📋 Status: NOT IMPLEMENTED

**Location:** `src/main/java/com/taskmanagement/event/`

**Current State:** Empty folder - no event system implemented

---

## 🔲 Planned Event System

This folder is reserved for future domain-driven design (DDD) events:

### Phase 1: Domain Events
- `TaskCreatedEvent.java` - Published when task is created
- `TaskUpdatedEvent.java` - Published when task is updated
- `TaskDeletedEvent.java` - Published when task is deleted
- `TaskStatusChangedEvent.java` - Published on status transitions
- `TaskAssignedEvent.java` - Published when assignee changes

### Phase 2: Event Listeners
- `TaskEventListener.java` - Handle task events
- `NotificationEventListener.java` - Send notifications on events
- `AuditLogEventListener.java` - Log events for audit trail

### Phase 3: Event Sourcing
- `EventStore.java` - Store all events
- `EventPublisher.java` - Publish events to message queue
- `EventReplayer.java` - Replay events for debugging

---

## 📝 Why No Events Yet?

**Current state:** Simple CRUD operations with synchronous processing

**Events would add value when:**
- Sending email notifications (when task assigned)
- Generating audit logs (who did what when)
- Triggering workflows (auto-assign based on rules)
- External system integration (Slack, Jira, etc.)
- Event sourcing for complete history

**Decision:** Defer events until clear async requirements emerge

---

## 🔮 Future Implementation

When event system is implemented, this folder will contain:

```
event/
├── TaskCreatedEvent.java
├── TaskUpdatedEvent.java
├── TaskDeletedEvent.java
├── TaskStatusChangedEvent.java
├── TaskAssignedEvent.java
├── listener/
│   ├── TaskEventListener.java
│   ├── NotificationEventListener.java
│   └── AuditLogEventListener.java
└── README.md
```

### Example Usage (Future)

```java
// In TaskService.createTask()
Task task = taskRepository.save(task);
applicationEventPublisher.publishEvent(new TaskCreatedEvent(task));
return TaskResponse.from(task);

// Listener handles notification
@EventListener
public void onTaskCreated(TaskCreatedEvent event) {
    emailService.sendTaskCreatedEmail(event.getTask());
    auditLogService.log("TASK_CREATED", event.getTask().getId());
}
```

---

**Last Updated:** December 15, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Placeholder - awaiting event system implementation
