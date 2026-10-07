package kr.noco.qticket.ui.realtime.websocket;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.springframework.web.socket.CloseStatus;

/**
 * seat WebSocket delta/pong 전송 (T47-30).
 * decorator가 내부 flushLock으로 write를 직렬화하고 tryLock 실패 시 sendTimeLimit을 검사하므로,
 * blocking send를 outer writeLock으로 감싸면 그 검사 경로가 막힌다. 그래서 상태 변경만 writeLock으로
 * 보호하고 실제 send는 잠금 밖에서 호출한다(순서는 decorator 내부 buffer가 보장).
 */
final class SeatWebSocketDeltaDelivery {

    private final SeatWebSocketFrameWriter writer;
    private final BiConsumer<SeatWebSocketConnection, CloseStatus> terminate;

    SeatWebSocketDeltaDelivery(SeatWebSocketFrameWriter writer,
            BiConsumer<SeatWebSocketConnection, CloseStatus> terminate) {
        this.writer = writer;
        this.terminate = terminate;
    }

    void pong(SeatWebSocketConnection connection) {
        if (!canSend(connection)) {
            return;
        }
        if (!writer.pong(connection.session)) {
            terminate.accept(connection, CloseStatus.SERVER_ERROR);
        }
    }

    void changed(SeatWebSocketConnection connection, SeatRealtimeEvent event) {
        synchronized (connection.writeLock) {
            if (connection.closed) {
                return;
            }
            if (connection.buffering) {
                if (!connection.buffer(event)) {
                    terminate.accept(connection, CloseStatus.SERVICE_OVERLOAD);
                }
                return;
            }
        }
        if (!writer.changed(connection.session, event)) {
            terminate.accept(connection, CloseStatus.SERVER_ERROR);
        }
    }

    private boolean canSend(SeatWebSocketConnection connection) {
        synchronized (connection.writeLock) {
            return !connection.closed;
        }
    }
}
