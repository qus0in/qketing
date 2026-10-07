package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/** SseConnectionManager 연결 수명 주기와 교체 안전성. */
class SseConnectionManagerTest {

    private static final Long PERFORMANCE_ID = 7L;
    private final SseConnectionManager manager = new SseConnectionManager();

    @Test
    void givenRegisteredEmitters_whenLifecycleCallback_thenAllReleased() {
        SseEmitter completed = registerMock(key("member-1"));
        SseEmitter timedOut = registerMock(key("member-2"));
        SseEmitter failed = registerMock(key("member-3"));
        assertThat(manager.subscriberKeys()).hasSize(3);

        captureCompletion(completed).run();
        captureTimeout(timedOut).run();
        captureError(failed).accept(new IOException("reset"));

        assertThat(manager.subscriberKeys()).isEmpty();
    }

    @Test
    void givenSendFailure_whenEmit_thenEmitterRemoved() throws IOException {
        SseEmitter emitter = registerMock(key("member-1"));
        doThrow(new IOException("closed")).when(emitter).send(any(SseEventBuilder.class));

        assertThat(manager.emit(key("member-1"), SseEvent.of("queue", "position 1"))).isFalse();
        assertThat(manager.subscriberKeys()).isEmpty();
    }

    @Test
    void givenReplacedEmitter_whenOldCompletes_thenNewSurvives() {
        SseEmitter old = registerMock(key("member-1"));
        manager.register(key("member-1"), mock(SseEmitter.class));

        captureCompletion(old).run();

        assertThat(manager.subscriberKeys()).hasSize(1);
        assertThat(manager.emit(key("member-1"), SseEvent.of("queue", "live"))).isTrue();
    }

    private SseEmitter registerMock(SseConnectionManager.SubscriberKey key) {
        SseEmitter emitter = mock(SseEmitter.class);
        manager.register(key, emitter);
        return emitter;
    }

    private Runnable captureCompletion(SseEmitter emitter) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(captor.capture());
        return captor.getValue();
    }

    private Runnable captureTimeout(SseEmitter emitter) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onTimeout(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Consumer<Throwable> captureError(SseEmitter emitter) {
        ArgumentCaptor<Consumer<Throwable>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(emitter).onError(captor.capture());
        return captor.getValue();
    }

    private static SseConnectionManager.SubscriberKey key(String subscriberId) {
        return new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, subscriberId);
    }
}
