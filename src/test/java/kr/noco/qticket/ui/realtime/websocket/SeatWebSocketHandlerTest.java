package kr.noco.qticket.ui.realtime.websocket;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

class SeatWebSocketHandlerTest {

    private final SeatWebSocketSessions sessions = mock(SeatWebSocketSessions.class);
    private final SeatWebSocketHandler handler = new SeatWebSocketHandler(sessions,
            JsonMapper.builder().build());

    @Test
    void givenPerformancePath_whenConnected_thenRegistersAndSnapshots() {
        WebSocketSession session = session("/ws/performances/7/seats");

        handler.afterConnectionEstablished(session);

        verify(sessions).registerAndSnapshot(session, 7L);
    }

    @Test
    void givenInvalidPerformancePath_whenConnected_thenRejectsInput() throws Exception {
        WebSocketSession session = session("/ws/performances/0/seats");

        handler.afterConnectionEstablished(session);

        verify(sessions).remove("ws-1");
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void givenControlFrames_whenReceived_thenPingSnapshotAndPongAreHandled() {
        WebSocketSession session = session("/ws/performances/7/seats");

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"PING\"}"));
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"PONG\"}"));
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"SNAPSHOT\"}"));

        verify(sessions).pong("ws-1");
        verify(sessions).snapshot("ws-1");
    }

    @Test
    void givenMutationOrActorPayload_whenReceived_thenRejected() throws Exception {
        WebSocketSession session = session("/ws/performances/7/seats");

        handler.handleTextMessage(session,
                new TextMessage("{\"type\":\"SNAPSHOT\",\"holderId\":\"secret\"}"));

        verify(sessions).remove("ws-1");
        verify(session).close(CloseStatus.BAD_DATA);
    }

    private WebSocketSession session(String path) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("ws-1");
        given(session.getUri()).willReturn(URI.create("ws://localhost" + path));
        given(session.isOpen()).willReturn(true);
        return session;
    }
}
