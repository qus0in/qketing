package kr.noco.qticket.ui.realtime.websocket;

import java.util.Map;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;

/**
 * Tomcat blocking send 상한 설정 (T47-53 추가 리뷰).
 * WsRemoteEndpointImplBase.getBlockingSendTimeout은 user property
 * org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT(Long, 기본 20000ms)를 읽는다.
 * 그래서 StandardWebSocketSession의 native jakarta Session user property에 sendHoldLimit을 넣어
 * Tomcat BasicRemote 자체에도 상한을 적용한다. mock/non-standard session은 조용히 건너뛴다.
 * (값 정책: decorator sendTimeLimit은 다음 send에서 검사되는 buffer 한도, 이 native 값은 실제
 *  blocking socket write 상한이며 watchdog은 7s+ best-effort close를 보완한다.)
 */
final class SeatWebSocketNativeTuning {

    static final String BLOCKING_SEND_TIMEOUT_PROPERTY =
            "org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT";

    private SeatWebSocketNativeTuning() {
    }

    /** Tomcat StandardWebSocketSession이면 native Session user property로 blocking send 상한을 설정한다. */
    static void applyBlockingSendTimeout(WebSocketSession session, long timeoutMillis) {
        if (!(session instanceof NativeWebSocketSession nativeSession)) {
            return;
        }
        if (nativeSession.getNativeSession() instanceof jakarta.websocket.Session jakartaSession) {
            Map<String, Object> properties = jakartaSession.getUserProperties();
            if (properties != null) {
                properties.put(BLOCKING_SEND_TIMEOUT_PROPERTY, timeoutMillis);
            }
        }
    }
}
