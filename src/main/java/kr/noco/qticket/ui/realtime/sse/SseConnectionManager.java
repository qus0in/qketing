package kr.noco.qticket.ui.realtime.sse;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseConnectionManager {

    private final Map<SubscriberKey, SseConnection> connections = new ConcurrentHashMap<>();

    public record SubscriberKey(Long performanceId, String subscriberId) {}

    public void register(SubscriberKey key, SseEmitter emitter) {
        add(key, new SseConnection(emitter, true));
    }

    public boolean registerWithSnapshot(SubscriberKey key, SseEmitter emitter, SseEvent snapshot) {
        return registerWithSnapshot(key, emitter, () -> snapshot);
    }

    public boolean registerWithSnapshot(SubscriberKey key, SseEmitter emitter, Supplier<SseEvent> snapshot) {
        SseConnection connection = new SseConnection(emitter, false);
        add(key, connection);
        SseEvent event = load(key, connection, snapshot);
        synchronized (connection) {
            if (!connection.closed && connection.flush(event)) {
                return true;
            }
        }
        return discard(key, connection);
    }

    public boolean emit(SubscriberKey key, SseEvent event) {
        SseConnection connection = connections.get(key);
        boolean delivered = false;
        if (connection != null) {
            synchronized (connection) {
                delivered = !connection.closed && connection.deliver(event);
            }
        }
        return delivered || discard(key, connection);
    }

    public void remove(SubscriberKey key) {
        discard(key, connections.get(key));
    }

    public List<SubscriberKey> subscriberKeys() {
        return List.copyOf(connections.keySet());
    }

    @PreDestroy
    public void shutdown() {
        for (SubscriberKey key : subscriberKeys()) {
            discard(key, connections.get(key));
        }
    }

    private SseEvent load(SubscriberKey key, SseConnection connection, Supplier<SseEvent> snapshot) {
        try {
            SseEvent event = snapshot.get();
            if (event != null) {
                return event;
            }
        } catch (RuntimeException exception) {
            discard(key, connection);
            throw exception;
        }
        discard(key, connection);
        throw new IllegalStateException("Snapshot supplier returned null");
    }

    private void add(SubscriberKey key, SseConnection connection) {
        connection.bind(() -> drop(key, connection));
        SseConnection previous = connections.put(key, connection);
        if (previous != null && previous != connection) {
            discard(key, previous);
        }
    }

    private boolean discard(SubscriberKey key, SseConnection connection) {
        if (connection != null) {
            drop(key, connection);
            connection.complete();
        }
        return false;
    }

    private void drop(SubscriberKey key, SseConnection connection) {
        synchronized (connection) {
            connection.close();
        }
        connections.remove(key, connection);
    }
}
