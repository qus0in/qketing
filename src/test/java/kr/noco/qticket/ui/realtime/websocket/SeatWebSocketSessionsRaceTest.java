package kr.noco.qticket.ui.realtime.websocket;

import static kr.noco.qticket.ui.realtime.websocket.SeatWebSocketSessionsSupport.mockSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotEntry;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

/** delta 전송과 close/remove 경합에서 예외 없이 정리되는지 검증한다 (T47-22). */
class SeatWebSocketSessionsRaceTest {

    @Test
    void givenConcurrentEventAndRemove_whenRacing_thenSnapshotSentAndNoException() throws Exception {
        List<String> frames = new ArrayList<>();
        WebSocketSession session = mockSession(frames);
        SeatWebSocketSessions sessions = new SeatWebSocketSessions(snapshots(), writer(),
                SeatWebSocketProperties.defaults());
        sessions.registerAndSnapshot(session, 7L);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            pool.submit(() -> race(start, () -> sessions.onSeatRealtimeEvent(event())));
            pool.submit(() -> race(start, () -> sessions.remove("ws-1")));
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(frames.get(0)).contains("SNAPSHOT");
    }

    private SeatWebSocketFrameWriter writer() {
        return new SeatWebSocketFrameWriter(JsonMapper.builder().build());
    }

    private SeatRealtimeEvent event() {
        return new SeatRealtimeEvent(UUID.randomUUID(), 7L, 42L, SeatRealtimeEvent.Type.HOLD,
                Instant.parse("2026-10-06T10:00:00Z"));
    }

    private SeatSnapshotService snapshots() {
        SeatSnapshotService snapshots = mock(SeatSnapshotService.class);
        given(snapshots.snapshot(7L)).willReturn(new SeatSnapshot(7L,
                List.of(new SeatSnapshotEntry(42L, "AVAILABLE", false, 0L)),
                Instant.parse("2026-10-06T10:00:00Z")));
        return snapshots;
    }

    private void race(CountDownLatch start, Runnable action) {
        try {
            start.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }
        action.run();
    }
}
