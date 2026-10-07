package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/** SseConnectionManager snapshot 순서와 subscriber별 전달. */
class SseConnectionManagerDeliveryTest {

    private static final Long PERFORMANCE_ID = 7L;
    private final SseConnectionManager manager = new SseConnectionManager();

    @Test
    void givenSnapshotRegistration_whenLiveEmitted_thenSnapshotSentFirst() {
        SseEmitter emitter = mock(SseEmitter.class);
        List<Object> data = new ArrayList<>();
        recordData(emitter, data);
        SseConnectionManager.SubscriberKey key = key("member-1");

        boolean registered = manager.registerWithSnapshot(key, emitter,
                SseEvent.of("queue", Map.of("kind", "snapshot")));

        assertThat(registered).isTrue();
        manager.emit(key, SseEvent.of("seat", Map.of("kind", "live")));
        assertThat(data).containsExactly(Map.of("kind", "snapshot"), Map.of("kind", "live"));
    }

    @Test
    void givenUnsendableSnapshot_whenRegister_thenNotRegistered() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("closed")).when(emitter).send(any(SseEventBuilder.class));

        boolean registered = manager.registerWithSnapshot(key("member-1"), emitter,
                SseEvent.of("snapshot", "seats"));

        assertThat(registered).isFalse();
        assertThat(manager.subscriberKeys()).isEmpty();
    }

    @Test
    void givenTwoSubscribers_whenEmitToOne_thenOtherUntouched() throws IOException {
        SseEmitter memberA = registerMock(key("member-a"));
        SseEmitter memberB = registerMock(key("member-b"));

        manager.emit(key("member-a"), SseEvent.of("queue", "position 1"));

        verify(memberA).send(any(SseEventBuilder.class));
        verify(memberB, never()).send(any(SseEventBuilder.class));
    }

    @Test
    void givenOlderQueueTimestamp_whenEmitted_thenIgnored() throws IOException {
        SseEmitter emitter = registerMock(key("member-a"));

        assertThat(manager.emit(key("member-a"), SseEvent.of("queue", "v1", 200L))).isTrue();
        assertThat(manager.emit(key("member-a"), SseEvent.of("queue", "v0", 100L))).isTrue();

        verify(emitter).send(any(SseEventBuilder.class));
    }

    private SseEmitter registerMock(SseConnectionManager.SubscriberKey key) {
        SseEmitter emitter = mock(SseEmitter.class);
        manager.register(key, emitter);
        return emitter;
    }

    private static void recordData(SseEmitter emitter, List<Object> data) {
        try {
            doAnswer(invocation -> {
                SseEventBuilder builder = invocation.getArgument(0);
                builder.build().stream().map(DataWithMediaType::getData)
                        .filter(value -> !(value instanceof String)).forEach(data::add);
                return null;
            }).when(emitter).send(any(SseEventBuilder.class));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static SseConnectionManager.SubscriberKey key(String subscriberId) {
        return key(subscriberId, PERFORMANCE_ID);
    }
    private static SseConnectionManager.SubscriberKey key(String subscriberId, Long performanceId) {
        return new SseConnectionManager.SubscriberKey(performanceId, subscriberId);
    }
}
