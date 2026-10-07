package kr.noco.qticket.ui.realtime.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
@Component
public class SeatWebSocketHandler extends TextWebSocketHandler {

    private static final Pattern PATH = Pattern.compile(
            "^/ws/performances/([1-9][0-9]*)/seats$");
    private final SeatWebSocketSessions sessions;
    private final ObjectMapper objectMapper;

    public SeatWebSocketHandler(SeatWebSocketSessions sessions, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long performanceId = performanceId(session.getUri());
        if (performanceId == null) {
            reject(session);
            return;
        }
        sessions.registerAndSnapshot(session, performanceId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            handleCommand(session, command(message.getPayload()));
        } catch (JacksonException | IllegalArgumentException exception) {
            reject(session);
        }
    }

    private void handleCommand(WebSocketSession session, String command) {
        switch (command) {
            case "PING" -> sessions.pong(session.getId());
            case "PONG" -> { }
            case "SNAPSHOT" -> sessions.snapshot(session.getId());
            default -> reject(session);
        }
    }

    private String command(String payload) {
        JsonNode frame = objectMapper.readTree(payload);
        if (frame == null || !frame.isObject() || frame.size() != 1
                || frame.get("type") == null || !frame.get("type").isString()) {
            throw new IllegalArgumentException("Invalid WebSocket frame");
        }
        return frame.get("type").asText();
    }

    private Long performanceId(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        Matcher matcher = PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.transportError(session.getId());
    }

    private void reject(WebSocketSession session) {
        sessions.remove(session.getId());
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.BAD_DATA);
            }
        } catch (IOException ignored) {
            sessions.remove(session.getId());
        }
    }
}
