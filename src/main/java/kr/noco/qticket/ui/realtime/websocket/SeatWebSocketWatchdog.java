package kr.noco.qticket.ui.realtime.websocket;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;

/**
 * WS 연결 감시 (T47-44/T47-50).
 * decorator flushLock을 첫 blocking send가 잡으면 checkSessionLimits()가 다음 send에서만 실행되어
 * 추가 send가 없으면 시간 한도가 강제되지 않는다. 이 watchdog은 sweep 스레드에서 판정만 하고 실제
 * resync·raw close는 별도 bounded executor로 submit해 감시 스레드가 막히지 않게 한다. resync는
 * connection CAS in-flight로 중복을 막고 정상·거부 모든 경로에서 해제한다. stop()은 sweep 정지·신규
 * submit 차단 → 열린 연결 raw close → resync/close 종료 순서로 진행한다.
 */
public class SeatWebSocketWatchdog {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatWebSocketWatchdog.class);
    private static final long STOP_WAIT_MILLIS = 5_000L;

    private final SeatWebSocketSessions sessions;
    private final long sweepIntervalMillis;
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final Executor resyncExecutor = SeatWebSocketExecutors.bounded("seat-ws-resync-", 1, 2, 64);
    private final Executor closeExecutor = SeatWebSocketExecutors.bounded("seat-ws-close-", 1, 2, 64);
    private final SeatWebSocketCloseDispatcher closeDispatcher;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "seat-ws-watchdog");
                thread.setDaemon(true);
                return thread;
            });

    public SeatWebSocketWatchdog(SeatWebSocketSessions sessions, SeatWebSocketProperties properties) {
        this.sessions = sessions;
        this.closeDispatcher = new SeatWebSocketCloseDispatcher(sessions, closeExecutor);
        long resync = properties.resyncInterval().toMillis();
        this.sweepIntervalMillis = Math.max(50L, Math.min(resync / 3, 1_000L));
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::sweepQuietly, sweepIntervalMillis,
                sweepIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        stopping.set(true);
        scheduler.shutdownNow();
        for (SeatWebSocketConnection connection : sessions.liveConnections()) {
            closeDispatcher.submit(connection, CloseStatus.GOING_AWAY);
        }
        SeatWebSocketExecutors.shutdownBounded(resyncExecutor, closeExecutor, STOP_WAIT_MILLIS);
    }
    private void sweepQuietly() {
        try {
            sweep();
        } catch (RuntimeException exception) {
            LOGGER.warn("Seat WebSocket watchdog sweep failed", exception);
        }
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        for (SeatWebSocketConnection connection : sessions.liveConnections()) {
            if (connection.sendHoldExceeded()) {
                closeDispatcher.submit(connection, CloseStatus.SESSION_NOT_RELIABLE);
            } else if (connection.requestResyncIfDue(now)) {
                submitResync(connection);
            }
        }
    }

    private void submitResync(SeatWebSocketConnection connection) {
        if (stopping.get()) {
            connection.resyncCompleted();
            return;
        }
        try {
            resyncExecutor.execute(() -> resync(connection));
        } catch (RejectedExecutionException exception) {
            connection.resyncCompleted();
            LOGGER.warn("Seat WebSocket resync dispatch saturated; retry next sweep");
        }
    }

    private void resync(SeatWebSocketConnection connection) {
        try {
            sessions.snapshot(connection.session.getId());
        } finally {
            connection.resyncCompleted();
        }
    }
}
