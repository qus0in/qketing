package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/** snapshot보다 오래된 pending/live queue 값은 적용하지 않는다. */
class SseConnectionManagerFreshnessTest {

    private static final Long PERFORMANCE_ID = 7L;

    @Test
    void givenOlderPendingDuringSnapshot_whenFlush_thenDropped() throws Exception {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = mock(SseEmitter.class);
        List<Object> data = new ArrayList<>();
        recordData(emitter, data);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");

        Thread registration = new Thread(
                () -> manager.registerWithSnapshot(key, emitter, snapshot(building, ready)));
        registration.start();
        assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();

        manager.emit(key, SseEvent.of("queue", Map.of("n", 1), 100L));
        manager.emit(key, SseEvent.of("queue", Map.of("n", 3), 300L));

        ready.countDown();
        registration.join(5000);

        assertThat(data).containsExactly(Map.of("n", 2), Map.of("n", 3));
    }

    @Test
    void givenEqualTimestamp_whenEmitted_thenApplied() {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = mock(SseEmitter.class);
        List<Object> data = new ArrayList<>();
        recordData(emitter, data);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");

        manager.registerWithSnapshot(key, emitter, SseEvent.of("queue", Map.of("n", 1), 200L));
        assertThat(manager.emit(key, SseEvent.of("queue", Map.of("n", 2), 200L))).isTrue();

        assertThat(data).containsExactly(Map.of("n", 1), Map.of("n", 2));
    }

    private static Supplier<SseEvent> snapshot(CountDownLatch building, CountDownLatch ready) {
        return () -> {
            building.countDown();
            await(ready);
            return SseEvent.of("queue", Map.of("n", 2), 200L);
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
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
}
