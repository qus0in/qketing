package kr.noco.qticket.ui.realtime.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

final class SeatWebSocketConnection {

    final WebSocketSession session;
    final Long performanceId;
    final Object writeLock = new Object();
    final Object snapshotLock = new Object();
    final List<SeatRealtimeEvent> pending = new ArrayList<>();
    private final WebSocketSession rawSession;
    private final ConcurrentWebSocketSessionDecorator decorator;
    private final long sendHoldLimitMillis;
    private final long resyncIntervalMillis;
    private final int pendingLimit;
    private final AtomicLong lastSnapshotAtMillis = new AtomicLong();
    private final AtomicLong resyncRequestedAtMillis = new AtomicLong();
    volatile boolean closed;
    boolean buffering = true;

    SeatWebSocketConnection(WebSocketSession session, Long performanceId,
                            SeatWebSocketProperties properties) {
        this.rawSession = session;
        this.decorator = new ConcurrentWebSocketSessionDecorator(session,
                (int) properties.sendTimeLimit().toMillis(), properties.bufferSizeLimit(),
                OverflowStrategy.TERMINATE);
        this.session = decorator;
        this.performanceId = performanceId;
        this.pendingLimit = properties.pendingLimit();
        this.sendHoldLimitMillis = properties.sendHoldLimit().toMillis();
        this.resyncIntervalMillis = properties.resyncInterval().toMillis();
        this.lastSnapshotAtMillis.set(System.currentTimeMillis());
        SeatWebSocketNativeTuning.applyBlockingSendTimeout(session, sendHoldLimitMillis);
    }

    /** snapshot 전 버퍼링. 상한을 넘으면 false를 반환해 호출자가 연결을 종료한다. */
    boolean buffer(SeatRealtimeEvent event) {
        if (pending.size() >= pendingLimit) {
            return false;
        }
        pending.add(event);
        return true;
    }

    /** snapshot 전송 완료를 기록해 resync 주기를 다시 센다. */
    void markSnapshotSent() {
        lastSnapshotAtMillis.set(System.currentTimeMillis());
    }

    /** 주기 resync 필요 여부. CAS로 in-flight를 원자적으로 선점해 중복 enqueue를 막는다. */
    boolean requestResyncIfDue(long nowMillis) {
        if (nowMillis - lastSnapshotAtMillis.get() < resyncIntervalMillis) {
            return false;
        }
        return resyncRequestedAtMillis.compareAndSet(0L, nowMillis);
    }

    /** 완료 시각을 먼저 갱신한 뒤 in-flight를 해제해, 다음 sweep이 오래된 시각으로 중복 요청하지 않게 한다. */
    void resyncCompleted() {
        markSnapshotSent();
        resyncRequestedAtMillis.set(0L);
    }

    /** 첫 blocking send가 sendHoldLimit을 넘겼는지 감시한다. */
    boolean sendHoldExceeded() {
        return !closed && decorator.getTimeSinceSendStarted() > sendHoldLimitMillis;
    }

    /** watchdog 전용: decorator flushLock을 피해 raw socket을 직접 닫아 blocking send를 해제한다. */
    void forceCloseRaw(CloseStatus status) {
        close();
        try {
            if (rawSession.isOpen()) {
                rawSession.close(status);
            }
        } catch (IOException ignored) {
            // 이미 닫힌 연결
        }
    }

    void close() {
        synchronized (writeLock) {
            closed = true;
            pending.clear();
        }
    }
}
