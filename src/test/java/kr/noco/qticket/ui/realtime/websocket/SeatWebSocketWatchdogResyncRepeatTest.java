package kr.noco.qticket.ui.realtime.websocket;

import static kr.noco.qticket.ui.realtime.websocket.SeatWebSocketSessionsSupport.countingBlockingSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotEntry;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

/**
 * 자동 watchdog의 반복 resync 실측 (T47-44/T47-47).
 * 수동 sweep 없이 start()로 주기 감시를 띄우고, 이벤트가 없어도 resync가 2회 이상 반복되는지 검증한다.
 * in-flight 플래그가 완료마다 reset되지 않으면 1회 후 멈추므로, 이 테스트가 그 회귀를 잡는다.
 */
class SeatWebSocketWatchdogResyncRepeatTest {

    @Test
    void givenNoEvents_whenWatchdogRuns_thenSnapshotResyncedRepeatedly() throws Exception {
        List<String> frames = new ArrayList<>();
        WebSocketSession session = SeatWebSocketSessionsSupport.mockSession(frames);
        SeatWebSocketProperties properties = new SeatWebSocketProperties(
                java.time.Duration.ofSeconds(5), 262144, 256,
                java.time.Duration.ofSeconds(30), java.time.Duration.ofMillis(150));
        SeatSnapshotService snapshots = mock(SeatSnapshotService.class);
        given(snapshots.snapshot(7L)).willReturn(snapshot());
        SeatWebSocketSessions sessions = new SeatWebSocketSessions(snapshots,
                new SeatWebSocketFrameWriter(JsonMapper.builder().build()), properties);
        SeatWebSocketWatchdog watchdog = new SeatWebSocketWatchdog(sessions, properties);
        sessions.registerAndSnapshot(session, 7L);
        assertThat(frames).hasSize(1);

        watchdog.start();
        try {
            assertThat(waitForSnapshots(frames, 2, 5_000L)).isTrue();
        } finally {
            watchdog.stop();
        }
    }

    private boolean waitForSnapshots(List<String> frames, int expected, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (frames.size() >= expected + 1) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(25L);
        }
        return frames.size() >= expected + 1;
    }

    private SeatSnapshot snapshot() {
        return new SeatSnapshot(7L,
                List.of(new SeatSnapshotEntry(42L, "AVAILABLE", false, 0L)),
                Instant.parse("2026-10-06T10:00:00Z"));
    }
}
