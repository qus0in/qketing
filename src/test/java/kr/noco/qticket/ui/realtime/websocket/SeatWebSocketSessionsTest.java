package kr.noco.qticket.ui.realtime.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotEntry;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SeatWebSocketSessionsTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void givenEventDuringSnapshot_whenConnected_thenSnapshotPrecedesInvalidation() throws Exception {
        List<String> frames = connectWithEventBufferedDuringSnapshot();
        JsonNode snapshot = mapper.readTree(frames.get(0));
        JsonNode changed = mapper.readTree(frames.get(1));

        assertThat(snapshot.path("type").asText()).isEqualTo("SNAPSHOT");
        assertThat(changed.path("type").asText()).isEqualTo("SEAT_CHANGED");
        assertThat(changed.path("seatId").asLong()).isEqualTo(42L);
        assertThat(changed.has("holderId")).isFalse();
        assertThat(changed.has("memberId")).isFalse();
    }

    private List<String> connectWithEventBufferedDuringSnapshot() throws Exception {
        List<String> frames = new ArrayList<>();
        SeatRealtimeEvent event = new SeatRealtimeEvent(UUID.randomUUID(), 7L, 42L,
                SeatRealtimeEvent.Type.HOLD, Instant.parse("2026-10-06T10:00:00Z"));
        AtomicReference<SeatWebSocketSessions> current = new AtomicReference<>();
        SeatSnapshotService snapshots = snapshotService(event, current);
        SeatWebSocketSessions sessions = new SeatWebSocketSessions(snapshots,
                new SeatWebSocketFrameWriter(mapper), SeatWebSocketProperties.defaults());
        current.set(sessions);
        sessions.registerAndSnapshot(mockSession(frames), 7L);
        return frames;
    }

    private SeatSnapshotService snapshotService(SeatRealtimeEvent event,
            AtomicReference<SeatWebSocketSessions> sessions) {
        SeatSnapshotService snapshots = mock(SeatSnapshotService.class);
        given(snapshots.snapshot(7L)).willAnswer(invocation -> {
            sessions.get().onSeatRealtimeEvent(event);
            return new SeatSnapshot(7L,
                    List.of(new SeatSnapshotEntry(42L, "AVAILABLE", false, 0L)),
                    Instant.parse("2026-10-06T10:00:00Z"));
        });
        return snapshots;
    }

    private WebSocketSession mockSession(List<String> frames) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("ws-1");
        given(session.isOpen()).willReturn(true);
        doAnswer(invocation -> {
            frames.add(((TextMessage) invocation.getArgument(0)).getPayload());
            return null;
        }).when(session).sendMessage(any(TextMessage.class));
        return session;
    }
}
