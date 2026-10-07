package kr.noco.qticket.ui.realtime.websocket;

import static kr.noco.qticket.ui.realtime.websocket.SeatWebSocketSessionsSupport.blockingSession;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * delta 전송 경로가 decorator의 시간 한도를 실제 강제하는지 검증 (T47-30).
 * 첫 delta가 멈춘 뒤 한도가 지나 두 번째 delta가 오면 decorator가 SessionLimitExceededException을
 * 던지고, writer가 실패를 반환해 terminate로 연결이 닫힌다. 추가 delta가 없으면 미강제(운영 한계).
 */
class SeatWebSocketFirstSendBlockTest {

    private static final int SEND_TIME_LIMIT_MS = 100;

    @Test
    void givenFirstDeltaBlocked_whenSecondDeltaAfterLimit_thenConnectionTerminated()
            throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SeatWebSocketConnection connection = new SeatWebSocketConnection(
                blockingSession(blocked, release), 7L,
                new SeatWebSocketProperties(Duration.ofMillis(SEND_TIME_LIMIT_MS), 262144, 256));
        connection.buffering = false;
        SeatWebSocketDeltaDelivery deltas = new SeatWebSocketDeltaDelivery(
                new SeatWebSocketFrameWriter(JsonMapper.builder().build()),
                (target, status) -> terminate(target, status));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> deltas.changed(connection, event()));
            assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(SEND_TIME_LIMIT_MS + 120L);

            Future<?> second = pool.submit(() -> deltas.changed(connection, event()));
            second.get(2, TimeUnit.SECONDS);

            assertThat(connection.closed).isTrue();
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private void terminate(SeatWebSocketConnection connection, CloseStatus status) {
        try {
            connection.session.close(status);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        connection.close();
    }

    private SeatRealtimeEvent event() {
        return new SeatRealtimeEvent(UUID.randomUUID(), 7L, 42L, SeatRealtimeEvent.Type.HOLD,
                Instant.parse("2026-10-06T10:00:00Z"));
    }
}
