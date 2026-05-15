import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketManager {
    constructor() {
        this._client = null;
        this._connected = false;
        // Map<topic, { sub: StompSubscription, callback: Function }>
        this._subscriptions = new Map();
        // Map<topic, callback> — queued while connecting
        this._pending = new Map();
        this._unsubscribeTimers = new Map();
    }

    connect(token) {
        if (this._client?.active) {
            console.debug('WebSocketManager: already connected');
            return;
        }

        const client = new Client({                    // ← local var, NOT this._client yet
            webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_BASE_URL}/ws`),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            onConnect: () => {
                if (this._client !== client) return;   // ← stale guard: ignore old client
                this._connected = true;
                console.log('WebSocketManager: connected');

                const toResubscribe = new Map(this._subscriptions);
                this._subscriptions.clear();
                toResubscribe.forEach((entry, topic) => this._doSubscribe(topic, entry.callback));

                this._pending.forEach((cb, topic) => this._doSubscribe(topic, cb));
                this._pending.clear();
            },
            onDisconnect: () => {
                if (this._client !== client) return;   // ← stale guard: old client fires this → ignored
                this._connected = false;
                console.log('WebSocketManager: disconnected');
            },
            onStompError: (frame) =>
                console.error('WebSocketManager: STOMP error', frame.headers['message'], frame.body),
            onWebSocketError: (e) =>
                console.error('WebSocketManager: WebSocket error', e),
        });

        this._client = client;                         // ← assign AFTER closure captures `client`
        client.activate();
    }

    disconnect() {
        this._unsubscribeTimers.forEach(timer => clearTimeout(timer));
        this._unsubscribeTimers.clear();

        this._subscriptions.clear();
        this._pending.clear();
        if (this._client) {
            this._client.deactivate();
            this._client = null;
        }
        this._connected = false;
        console.log('WebSocketManager: disconnected');
    }

    _doSubscribe(topic, callback) {
        if (this._subscriptions.has(topic)) return;
        const sub = this._client.subscribe(topic, (frame) => {
            try {
                const entry = this._subscriptions.get(topic);
                entry?.callback(JSON.parse(frame.body)); // luôn lấy callback mới
            } catch (e) {
                console.error('parse error', e);
            }
        });
        this._subscriptions.set(topic, { sub, callback });
        console.log(`WebSocketManager: subscribed to ${topic}`);
    }

    subscribe(topic, callback) {
        const timer = this._unsubscribeTimers.get(topic);
        if (timer !== undefined) {
            clearTimeout(timer);
            this._unsubscribeTimers.delete(topic);
            // subscription still alive in _subscriptions → update callback only
            const existing = this._subscriptions.get(topic);
            if (existing) {
                existing.callback = callback;
            }
            return;
        }

        const existing = this._subscriptions.get(topic);
        if (existing) {
            // ✅ update callback thay vì ignore
            existing.callback = callback;
            console.log(`WebSocketManager: updated callback for ${topic}`);
            return;
        }
        if (this._connected) {
            this._doSubscribe(topic, callback);
        } else {
            this._pending.set(topic, callback);
            console.log(`WebSocketManager: queued subscription to ${topic} (not connected)`);
        }
    }

    unsubscribe(topic) {
        this._pending.delete(topic);

        if (!this._subscriptions.has(topic)) return;

        const timer = setTimeout(() => {
            this._unsubscribeTimers.delete(topic);
            const entry = this._subscriptions.get(topic);
            if (entry) {
                try { entry.sub.unsubscribe(); } catch (_) {}
                this._subscriptions.delete(topic);
                console.log(`WebSocketManager: unsubscribed from ${topic}`);
            }
        }, 0);   
        this._unsubscribeTimers.set(topic, timer);
    }

    reconnect(newToken) {
        this._unsubscribeTimers.forEach(timer => clearTimeout(timer));
        this._unsubscribeTimers.clear();

        // Preserve all callbacks before teardown
        const toRestore = new Map();
        this._subscriptions.forEach(({ callback}, topic) => toRestore.set(topic, callback));
        this._pending.forEach((cb, topic) => toRestore.set(topic, cb));

        this._subscriptions.clear();
        this._pending.clear();
        if (this._client) {
            this._client.deactivate();
            this._client = null;
        }
        this._connected = false;
    
        // Queue all previous topics to re-subscribe on next connection
        toRestore.forEach((cb, topic) => this._pending.set(topic, cb));
        this.connect(newToken);
        console.log('WebSocketManager: reconnecting with new token');
    }

    updateToken(newToken) {
        if (this._client?.active) {
            this.reconnect(newToken);
        }
    }
}
export const wsManager = new WebSocketManager();