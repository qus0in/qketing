package kr.noco.qticket.ui.realtime.websocket;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SeatWebSocketSessions {

    private final ConcurrentMap<String, SeatWebSocketConnection> connections =
            new ConcurrentHashMap<>();
    private final SeatWebSocketFrameWriter writer;
    private final SeatWebSocketSnapshotDelivery snapshots;
    private final SeatWebSocketDeltaDelivery deltas;
    private final SeatWebSocketProperties properties;

    public SeatWebSocketSessions(SeatSnapshotService snapshots, SeatWebSocketFrameWriter writer,
                                 SeatWebSocketProperties properties) {
        this.writer = writer;
        this.properties = properties;
        this.snapshots = new SeatWebSocketSnapshotDelivery(snapshots, writer, this::terminate);
        this.deltas = new SeatWebSocketDeltaDelivery(writer, this::terminate);
    }

    public void registerAndSnapshot(WebSocketSession session, Long performanceId) {
        SeatWebSocketConnection connection = new SeatWebSocketConnection(session, performanceId,
                properties);
        if (connections.putIfAbsent(session.getId(), connection) != null) {
            writer.close(session, CloseStatus.BAD_DATA);
            return;
        }
        snapshots.send(connection);
    }

    public void snapshot(String sessionId) {
        withConnection(sessionId, snapshots::send);
    }

    public void pong(String sessionId) {
        withConnection(sessionId, deltas::pong);
    }

    public void remove(String sessionId) {
        SeatWebSocketConnection connection = connections.remove(sessionId);
        if (connection != null) { connection.close(); }
    }

    public void transportError(String sessionId) {
        withConnection(sessionId, connection -> forceClose(connection, CloseStatus.SERVER_ERROR));
    }

    @PreDestroy
    public void shutdown() {
        for (SeatWebSocketConnection connection : liveConnections()) {
            if (connections.remove(connection.session.getId(), connection)) {
                connection.forceCloseRaw(CloseStatus.GOING_AWAY);
            }
        }
    }
    @EventListener
    public void onSeatRealtimeEvent(SeatRealtimeEvent event) {
        for (SeatWebSocketConnection connection : liveConnections()) {
            if (connection.performanceId.equals(event.performanceId())) {
                deltas.changed(connection, event);
            }
        }
    }

    List<SeatWebSocketConnection> liveConnections() {
        return List.copyOf(connections.values());
    }

    void forceClose(SeatWebSocketConnection connection, CloseStatus status) {
        if (connections.remove(connection.session.getId(), connection)) {
            connection.forceCloseRaw(status);
        }
    }

    private void withConnection(String sessionId, Consumer<SeatWebSocketConnection> action) {
        SeatWebSocketConnection connection = connections.get(sessionId);
        if (connection != null) {
            action.accept(connection);
        }
    }

    private void terminate(SeatWebSocketConnection connection, CloseStatus status) {
        connections.remove(connection.session.getId(), connection);
        connection.close();
        writer.close(connection.session, status);
    }
}
