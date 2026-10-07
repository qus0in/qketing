package kr.noco.qticket.ui.realtime.websocket;

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
 * watchdog stop()가 blocked send를 raw close로 해제하는지 실측 (T47-50).
 * stop()은 sweep 정지 후 열린 연결을 close 전용 pool로 제출하고 resync/close 실행기 종료를 기다린다.
 * 여기서는 stop()이 raw close를 유발해 blocked sender가 풀리고 registry가 비는지 확인한다.
 */
class SeatWebSocketWatchdogStopReleaseTest {

    @Test
    void givenBlockedSend_whenWatchdogStops_thenRawCloseReleasesSendAndRegistryEmpty()
            throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch released = new CountDownLatch(1);
        WebSocketSession session =
                SeatWebSocketSessionsSupport.closeReleasingSession(blocked, released);
        SeatWebSocketProperties properties = new SeatWebSocketProperties(Duration.ofMillis(60),
                262144, 256, Duration.ofSeconds(30), Duration.ofSeconds(30));
        SeatWebSocketSessions sessions = new SeatWebSocketSessions(snapshots(), writer(),
                properties);
        SeatWebSocketWatchdog watchdog = new SeatWebSocketWatchdog(sessions, properties);
        sessions.registerAndSnapshot(session, 7L);
        Thread sender = new Thread(() -> sessions.onSeatRealtimeEvent(event()));
        sender.start();
        assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();

        watchdog.stop();
        sender.join(5_000L);

        verify(session).close(CloseStatus.GOING_AWAY);
        assertThat(sender.isAlive()).isFalse();
        assertThat(sessions.liveConnections()).isEmpty();
        assertThat(watchdogWorkersAlive()).isFalse();
    }

    /** stop()의 awaitIdle+destroy 후 남아 있는 watchdog 보조 스레드가 없는지 확인한다. */
    private boolean watchdogWorkersAlive() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            boolean alive = Thread.getAllStackTraces().keySet().stream()
                    .map(Thread::getName)
                    .anyMatch(name -> name.startsWith("seat-ws-resync-")
                            || name.startsWith("seat-ws-close-"));
            if (!alive) {
                return false;
            }
            TimeUnit.MILLISECONDS.sleep(50L);
        }
        return true;
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
