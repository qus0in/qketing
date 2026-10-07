package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** remove/shutdown이 모든 연결을 complete로 정리하고 registry를 비우는지 검증한다. */
class SseConnectionManagerLifecycleTest {

    private static final Long PERFORMANCE_ID = 7L;
    private final SseConnectionManager manager = new SseConnectionManager();

    @Test
    void givenConnections_whenShutdown_thenAllCompletedAndCleared() {
        SseEmitter first = registerMock(key("member-1"));
        SseEmitter second = registerMock(key("member-2"));

        manager.shutdown();

        assertThat(manager.subscriberKeys()).isEmpty();
        verify(first).complete();
        verify(second).complete();
    }

    @Test
    void givenConnection_whenRemove_thenCompletedAndEmitFails() {
        SseEmitter emitter = registerMock(key("member-1"));

        manager.remove(key("member-1"));

        assertThat(manager.subscriberKeys()).isEmpty();
        verify(emitter).complete();
        assertThat(manager.emit(key("member-1"), SseEvent.of("queue", "v"))).isFalse();
    }

    @Test
    void givenUnknownKey_whenRemove_thenNoop() {
        manager.remove(key("ghost"));

        assertThat(manager.subscriberKeys()).isEmpty();
    }

    private SseEmitter registerMock(SseConnectionManager.SubscriberKey key) {
        SseEmitter emitter = mock(SseEmitter.class);
        manager.register(key, emitter);
        return emitter;
    }

    private static SseConnectionManager.SubscriberKey key(String subscriberId) {
        return new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, subscriberId);
    }
}
