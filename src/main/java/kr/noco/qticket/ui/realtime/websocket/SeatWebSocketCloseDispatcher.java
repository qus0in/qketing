package kr.noco.qticket.ui.realtime.websocket;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;

/**
 * watchdog close dispatch (T47-50).
 * raw close는 blocking send의 decorator flushLock과 별개로 socket을 닫아 송신을 풀어낸다.
 * close 전용 pool로 제출해 감시 스레드와 다른 연결 감시를 막지 않게 하고, 상한을 넘으면 로그만 남긴다.
 */
final class SeatWebSocketCloseDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatWebSocketCloseDispatcher.class);
    private final SeatWebSocketSessions sessions;
    private final Executor closeExecutor;

    SeatWebSocketCloseDispatcher(SeatWebSocketSessions sessions, Executor closeExecutor) {
        this.sessions = sessions;
        this.closeExecutor = closeExecutor;
    }

    void submit(SeatWebSocketConnection connection, CloseStatus status) {
        try {
            closeExecutor.execute(() -> sessions.forceClose(connection, status));
        } catch (RejectedExecutionException exception) {
            LOGGER.warn("Seat WebSocket close dispatch saturated status={}", status);
        }
    }
}
