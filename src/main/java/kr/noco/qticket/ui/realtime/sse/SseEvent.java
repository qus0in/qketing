package kr.noco.qticket.ui.realtime.sse;

import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/**
 * SSE 전송 단위. id는 클라이언트 Last-Event-ID 복구용.
 * occurredAtMillis는 서버 내부 신선도 판정용이며 wire payload에는 넣지 않는다(0이면 무시).
 */
public record SseEvent(String id, String name, Object data, long occurredAtMillis) {

    public static SseEvent of(String name, Object data) {
        return of(name, data, 0L);
    }

    public static SseEvent of(String name, Object data, long occurredAtMillis) {
        return new SseEvent(UUID.randomUUID().toString(), name, data, occurredAtMillis);
    }

    public SseEventBuilder toBuilder() {
        return SseEmitter.event().id(id).name(name).data(data);
    }
}
