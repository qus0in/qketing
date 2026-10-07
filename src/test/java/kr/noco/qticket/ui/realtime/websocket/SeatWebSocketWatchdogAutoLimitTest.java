package kr.noco.qticket.ui.realtime.websocket;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static kr.noco.qticket.ui.realtime.websocket.SeatWebSocketSessionsSupport.countingBlockingSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
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

/**
 * watchdog 자동 감시 실측 (T47-44).
 * 수동 sweep 없이 start()로 실제 주기 감시를 띄우고, 첫 blocking send가 sendHoldLimit을 넘기면
 * watchdog이 별도 실행기에서 raw close로 강제 종료하고 registry에서 제거하는지 검증한다.
 * (watchdog이 없으면 raw close가 없다는 대조는 SeatWebSocketFirstSendBlockTest가 다룬다.)
 */
class SeatWebSocketWatchdogAutoLimitTest {

    @Test
    void givenBlockedFirstSend_whenWatchdogRuns_thenRawCloseAndRegistryCleanup() throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        WebSocketSession session = countingBlockingSession(blocked, release, 2);
        SeatWebSocketProperties properties = new SeatWebSocketProperties(Duration.ofMillis(60),
                262144, 256, Duration.ofMillis(120), Duration.ofSeconds(30));
        SeatWebSocketSessions sessions = new SeatWebSocketSessions(snapshots(), writer(),
                properties);
        SeatWebSocketWatchdog watchdog = new SeatWebSocketWatchdog(sessions, properties);
        sessions.registerAndSnapshot(session, 7L);
        Thread sender = new Thread(() -> sessions.onSeatRealtimeEvent(event()));
        sender.start();
        assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();

        watchdog.start();
        try {
            assertThat(waitForEmpty(sessions, 5_000L)).isTrue();
            verify(session).close(CloseStatus.SESSION_NOT_RELIABLE);
        } finally {
            watchdog.stop();
            release.countDown();
            sender.join(5_000L);
        }
    }

    private boolean waitForEmpty(SeatWebSocketSessions sessions, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline && !sessions.liveConnections().isEmpty()) {
            MILLISECONDS.sleep(25L);
        }
        return sessions.liveConnections().isEmpty();
    }

    private SeatWebSocketFrameWriter writer() {
        return new SeatWebSocketFrameWriter(JsonMapper.builder().build());
    }

    private SeatSnapshotService snapshots() {
        SeatSnapshotService snapshots = mock(SeatSnapshotService.class);
        given(snapshots.snapshot(7L)).willReturn(new SeatSnapshot(7L,
                List.of(new SeatSnapshotEntry(42L, "AVAILABLE", false, 0L)),
                Instant.parse("2026-10-06T10:00:00Z")));
        return snapshots;
    }

    private SeatRealtimeEvent event() {
        return new SeatRealtimeEvent(UUID.randomUUID(), 7L, 42L, SeatRealtimeEvent.Type.HOLD,
                Instant.parse("2026-10-06T10:00:00Z"));
    }
}
