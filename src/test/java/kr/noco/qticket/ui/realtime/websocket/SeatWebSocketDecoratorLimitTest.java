package kr.noco.qticket.ui.realtime.websocket;

import static kr.noco.qticket.ui.realtime.websocket.SeatWebSocketSessionsSupport.blockingSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;
import org.springframework.web.socket.handler.SessionLimitExceededException;

/**
 * ConcurrentWebSocketSessionDecorator의 첫 blocking send 한도 실측 (T47-30).
 * 바이트코드 + JDK 직접 실행 실측: 첫 send가 delegate에서 멈추면 flushLock을 보유해
 * checkSessionLimits()는 다음 send의 tryLock 실패 시에만 실행된다. 그래서 초과는 "다음 send"에서
 * SessionLimitExceededException(4500)으로 드러나고, 추가 send가 없으면 스스로 닫지 않는다.
 */
class SeatWebSocketDecoratorLimitTest {

    private static final int SEND_TIME_LIMIT_MS = 100;

    @Test
    void givenFirstSendBlocked_whenSecondSendArrives_thenLimitExceededException() throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ConcurrentWebSocketSessionDecorator decorator = decorator(
                blockingSession(blocked, release));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = pool.submit(() -> send(decorator));
            assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(SEND_TIME_LIMIT_MS + 120L);

            assertThatThrownBy(() -> decorator.sendMessage(new TextMessage("second")))
                    .isInstanceOf(SessionLimitExceededException.class)
                    .extracting(exception -> ((SessionLimitExceededException) exception)
                            .getStatus().getCode())
                    .isEqualTo(4500);

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void givenFirstSendBlocked_whenNoFurtherSend_thenNotSelfClosed() throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ConcurrentWebSocketSessionDecorator decorator = decorator(
                blockingSession(blocked, release));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = pool.submit(() -> send(decorator));
            assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(SEND_TIME_LIMIT_MS + 200L);

            assertThat(decorator.isOpen()).isTrue();
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private void send(ConcurrentWebSocketSessionDecorator decorator) {
        try {
            decorator.sendMessage(new TextMessage("first"));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ConcurrentWebSocketSessionDecorator decorator(WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, 262144,
                OverflowStrategy.TERMINATE);
    }
}
