package kr.noco.qticket.ui.realtime.websocket;

import static kr.noco.qticket.ui.realtime.websocket.SeatWebSocketSessionsSupport.mockSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotEntry;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

/** snapshot 대기 중 pending overflow 시 연결을 닫는지 검증한다 (T47-22). */
class SeatWebSocketSessionsOverflowTest {

    private static final SeatWebSocketProperties PROPERTIES = new SeatWebSocketProperties(
            Duration.ofSeconds(2), 262144, 2);

    @Test
    void givenPendingOverflowDuringSnapshot_whenEventsArrive_thenConnectionClosed() throws Exception {
        List<String> frames = new ArrayList<>();
        WebSocketSession session = mockSession(frames);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SeatWebSocketSessions sessions = new SeatWebSocketSessions(
                blockingSnapshots(building, release), writer(), PROPERTIES);

        Thread connect = new Thread(() -> sessions.registerAndSnapshot(session, 7L));
        connect.start();
        assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < 5; i++) {
            sessions.onSeatRealtimeEvent(event());
        }
        release.countDown();
        connect.join(5000);

        verify(session).close(CloseStatus.SERVICE_OVERLOAD);
        assertThat(frames).isEmpty();
    }

    private SeatWebSocketFrameWriter writer() {
        return new SeatWebSocketFrameWriter(JsonMapper.builder().build());
    }

    private SeatRealtimeEvent event() {
        return new SeatRealtimeEvent(UUID.randomUUID(), 7L, 42L, SeatRealtimeEvent.Type.HOLD,
                Instant.parse("2026-10-06T10:00:00Z"));
    }

    private SeatSnapshotService blockingSnapshots(CountDownLatch building, CountDownLatch release) {
        SeatSnapshotService snapshots = mock(SeatSnapshotService.class);
        given(snapshots.snapshot(7L)).willAnswer(invocation -> {
            building.countDown();
            release.await(5, TimeUnit.SECONDS);
            return snapshot();
        });
        return snapshots;
    }

    private SeatSnapshot snapshot() {
        return new SeatSnapshot(7L,
                List.of(new SeatSnapshotEntry(42L, "AVAILABLE", false, 0L)),
                Instant.parse("2026-10-06T10:00:00Z"));
    }
}
