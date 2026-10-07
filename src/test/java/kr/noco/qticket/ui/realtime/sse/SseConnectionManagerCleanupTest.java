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

/** snapshot 로딩 중 remove되면 버퍼된 이벤트를 보내지 않고 정리되는지 검증한다. */
class SseConnectionManagerCleanupTest {

    private static final Long PERFORMANCE_ID = 7L;

    @Test
    void givenRemoveDuringSnapshot_whenSupplierReturns_thenNothingSent() throws Exception {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = mock(SseEmitter.class);
        List<Object> sent = new ArrayList<>();
        recordData(emitter, sent);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");

        Thread registration = new Thread(
                () -> manager.registerWithSnapshot(key, emitter, snapshot(building, ready)));
        registration.start();
        assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();
        manager.emit(key, SseEvent.of("queue", Map.of("n", 1)));

        manager.remove(key);
        ready.countDown();
        registration.join(5000);

        assertThat(sent).isEmpty();
        assertThat(manager.subscriberKeys()).isEmpty();
    }

    private static Supplier<SseEvent> snapshot(CountDownLatch building, CountDownLatch ready) {
        return () -> {
            building.countDown();
            await(ready);
            return SseEvent.of("queue", Map.of("n", 2));
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
