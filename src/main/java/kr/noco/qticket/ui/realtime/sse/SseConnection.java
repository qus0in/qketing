package kr.noco.qticket.ui.realtime.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** subscriber 연결. snapshot 전 live를 버퍼링하고, snapshot보다 오래된 값은 버린다. */
final class SseConnection {

    private static final int MAX_PENDING = 256;

    private final SseEmitter emitter;
    private final List<SseEvent> pending = new ArrayList<>();
    private boolean snapshotSent;
    private long appliedMillis;
    boolean closed;

    SseConnection(SseEmitter emitter, boolean snapshotSent) {
        this.emitter = emitter;
        this.snapshotSent = snapshotSent;
    }

    void bind(Runnable onClose) {
        emitter.onCompletion(onClose);
        emitter.onTimeout(onClose);
        emitter.onError(error -> onClose.run());
    }

    void close() {
        closed = true;
        pending.clear();
    }

    boolean deliver(SseEvent event) {
        if (isStale(event.occurredAtMillis())) {
            return true;
        }
        if (!snapshotSent) {
            return buffer(event);
        }
        if (!send(event)) {
            return false;
        }
        track(event.occurredAtMillis());
        return true;
    }

    boolean flush(SseEvent snapshot) {
        if (!send(snapshot)) {
            return false;
        }
        track(snapshot.occurredAtMillis());
        for (SseEvent event : pending) {
            if (!isStale(event.occurredAtMillis()) && !send(event)) {
                return false;
            }
        }
        pending.clear();
        snapshotSent = true;
        return true;
    }

    void complete() {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // 이미 종료된 emitter
        }
    }

    private boolean buffer(SseEvent event) {
        if (pending.size() >= MAX_PENDING) {
            closed = true;
            return false;
        }
        pending.add(event);
        return true;
    }

    private void track(long millis) {
        if (millis > appliedMillis) {
            appliedMillis = millis;
        }
    }

    private boolean isStale(long millis) {
        return millis > 0 && millis < appliedMillis;
    }

    private boolean send(SseEvent event) {
        try {
            emitter.send(event.toBuilder());
            return true;
        } catch (IOException | IllegalStateException e) {
            closed = true;
            return false;
        }
    }
}
