package com.taskmanagement.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventListener {
    
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async // Không block transaction của TaskService
    public void handleTaskEvent(TaskEvent event) {
        String destination = "/topic/projects/" + event.getProjectId() + "/tasks";

        Object message = switch (event.getType()) {
            case CREATED, UPDATED -> Map.of(
                "type", event.getType().name(),
                "payload", event.getPayload()
            );
            case DELETED -> Map.of(
                "type", event.getType().name(),
                "payload", Map.of("id", event.getDeletedTaskId())
            );
        };
        messagingTemplate.convertAndSend(destination, message);
        log.info("WebSocket broadcast: destination={}, type={}", destination, event.getType());
    }
}
