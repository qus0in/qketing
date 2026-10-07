package kr.noco.qticket.ui.realtime.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;

/**
 * snapshot 전송과 버퍼 flush (T47-30, resync T47-44).
 * send는 outer writeLock 밖에서 호출하고 상태(buffering·pending)만 writeLock으로 보호한다.
 * pending은 snapshot→delta 순서로 flush하고 완료 시 resync 주기를 다시 센다.
 */
final class SeatWebSocketSnapshotDelivery {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SeatWebSocketSnapshotDelivery.class);
    private final SeatSnapshotService snapshots;
    private final SeatWebSocketFrameWriter writer;
    private final BiConsumer<SeatWebSocketConnection, CloseStatus> terminate;

    SeatWebSocketSnapshotDelivery(SeatSnapshotService snapshots, SeatWebSocketFrameWriter writer,
            BiConsumer<SeatWebSocketConnection, CloseStatus> terminate) {
        this.snapshots = snapshots;
        this.writer = writer;
        this.terminate = terminate;
    }

    void send(SeatWebSocketConnection connection) {
        synchronized (connection.snapshotLock) {
            SeatSnapshot snapshot = beginAndLoad(connection);
            if (snapshot != null) { deliver(connection, snapshot); }
        }
    }

    /** buffering을 시작하고 snapshot을 조회한다. closed면 null(종료 없음), 실패면 종료 후 null. */
    private SeatSnapshot beginAndLoad(SeatWebSocketConnection connection) {
        synchronized (connection.writeLock) {
            if (connection.closed) {
                return null;
            }
            connection.buffering = true;
        }
        try {
            return snapshots.snapshot(connection.performanceId);
        } catch (RuntimeException exception) {
            LOGGER.warn("Seat snapshot failed performanceId={}", connection.performanceId,
                    exception);
            terminate.accept(connection, CloseStatus.SERVER_ERROR);
            return null;
        }
    }

    private void deliver(SeatWebSocketConnection connection, SeatSnapshot snapshot) {
        if (!writer.snapshot(connection.session, snapshot)) {
            terminate.accept(connection, CloseStatus.SERVER_ERROR);
            return;
        }
        while (true) {
            List<SeatRealtimeEvent> batch = drain(connection);
            if (batch == null) {
                connection.markSnapshotSent();
                return;
            }
            if (!sendAll(connection, batch)) {
                return;
            }
        }
    }

    private boolean sendAll(SeatWebSocketConnection connection, List<SeatRealtimeEvent> batch) {
        for (SeatRealtimeEvent event : batch) {
            if (!writer.changed(connection.session, event)) {
                terminate.accept(connection, CloseStatus.SERVER_ERROR);
                return false;
            }
        }
        return true;
    }

    /** pending을 비워 반환한다. 비어 있으면 buffering을 해제하고 null을 반환한다. */
    private List<SeatRealtimeEvent> drain(SeatWebSocketConnection connection) {
        synchronized (connection.writeLock) {
            if (connection.closed) {
                return null;
            }
            if (connection.pending.isEmpty()) {
                connection.buffering = false;
                return null;
            }
            List<SeatRealtimeEvent> batch = new ArrayList<>(connection.pending);
            connection.pending.clear();
            return batch;
        }
    }
}
