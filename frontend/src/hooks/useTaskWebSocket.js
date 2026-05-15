import { useEffect, useRef, useCallback } from "react";
import { wsManager } from "../services/WebSocketManager";

/**
 * Hook kết nối WebSocket STOMP và subscribe topic của 1 project
 */
export function useTaskWebSocket(projectId, onMessage) {
    const onMessageRef = useRef(onMessage);

    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    // ✅ stable callback (chỉ tạo 1 lần)
    const stableCallback = useCallback((event) => {
        onMessageRef.current(event);
    }, []);

    useEffect(() => {
        if (!projectId) return;

        const topic = `/topic/projects/${projectId}/tasks`;

        wsManager.subscribe(topic, stableCallback);

        return () => {
            wsManager.unsubscribe(topic);
        };
    }, [projectId, stableCallback]);
}
