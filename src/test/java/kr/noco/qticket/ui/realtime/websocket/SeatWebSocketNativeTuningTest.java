package kr.noco.qticket.ui.realtime.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;

/** Tomcat blocking send 상한 user property 설정 회귀 검증 (T47-53 추가 리뷰). */
class SeatWebSocketNativeTuningTest {

    @Test
    void givenNativeStandardSession_whenApplied_thenBlockingSendTimeoutPropertySet() {
        Map<String, Object> userProperties = new HashMap<>();
        jakarta.websocket.Session jakartaSession = mock(jakarta.websocket.Session.class);
        given(jakartaSession.getUserProperties()).willReturn(userProperties);
        WebSocketSession session = nativeSession(jakartaSession);

        SeatWebSocketNativeTuning.applyBlockingSendTimeout(session, 7000L);

        assertThat(userProperties)
                .containsEntry(SeatWebSocketNativeTuning.BLOCKING_SEND_TIMEOUT_PROPERTY, 7000L);
    }

    @Test
    void givenNonNativeSession_whenApplied_thenIgnoredWithoutError() {
        WebSocketSession session = mock(WebSocketSession.class);

        SeatWebSocketNativeTuning.applyBlockingSendTimeout(session, 7000L);

        verifyNoInteractions(session);
    }

    @Test
    void givenNullUserProperties_whenApplied_thenIgnoredWithoutError() {
        jakarta.websocket.Session jakartaSession = mock(jakarta.websocket.Session.class);
        given(jakartaSession.getUserProperties()).willReturn(null);

        SeatWebSocketNativeTuning.applyBlockingSendTimeout(nativeSession(jakartaSession), 7000L);
    }

    private WebSocketSession nativeSession(jakarta.websocket.Session jakartaSession) {
        WebSocketSession session = mock(WebSocketSession.class,
                org.mockito.Mockito.withSettings().extraInterfaces(NativeWebSocketSession.class));
        given(((NativeWebSocketSession) session).getNativeSession()).willReturn(jakartaSession);
        return session;
    }
}
