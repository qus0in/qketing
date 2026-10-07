package kr.noco.qticket.ui.realtime.websocket;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

@Component
public class SeatWebSocketFrameWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatWebSocketFrameWriter.class);
    private final ObjectMapper objectMapper;

    public SeatWebSocketFrameWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean snapshot(WebSocketSession session, SeatSnapshot snapshot) {
        return send(session, new SnapshotFrame("SNAPSHOT", snapshot.performanceId(),
                snapshot.seats(), snapshot.capturedAt()));
    }

    public boolean changed(WebSocketSession session, SeatRealtimeEvent event) {
        return send(session, new SeatChangedFrame("SEAT_CHANGED", event.eventId(),
                event.performanceId(), event.seatId()));
    }

    public boolean pong(WebSocketSession session) {
        return send(session, new ControlFrame("PONG"));
    }

    private boolean send(WebSocketSession session, Object frame) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Seat WebSocket frame send failed", exception);
            close(session, CloseStatus.SERVER_ERROR);
            return false;
        }
    }

    public void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException exception) {
            LOGGER.debug("Seat WebSocket close failed", exception);
        }
    }

    private record SnapshotFrame(String type, Long performanceId, List<SeatSnapshotEntry> seats,
                                 Instant capturedAt) {}

    private record SeatChangedFrame(String type, UUID eventId, Long performanceId,
                                    Long seatId) {}

    private record ControlFrame(String type) {}
}
