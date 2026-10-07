package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/** snapshot 조립 중 도착한 live 이벤트가 snapshot 뒤로 밀리는지 검증한다. */
class SseConnectionManagerInterleavingTest {

    private static final Long PERFORMANCE_ID = 7L;

    @Test
    void givenLiveEventDuringSnapshotBuild_whenSnapshotReady_thenSnapshotSentFirst() throws Exception {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = mock(SseEmitter.class);
        List<Object> data = new CopyOnWriteArrayList<>();
        recordData(emitter, data);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");

        Thread registration = new Thread(
                () -> manager.registerWithSnapshot(key, emitter, snapshot(building, ready)));
        registration.start();
        assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(manager.emit(key, SseEvent.of("seat", Map.of("kind", "live")))).isTrue();
        assertThat(data).isEmpty();

        ready.countDown();
        registration.join(5000);
        assertThat(data).containsExactly(Map.of("kind", "snapshot"), Map.of("kind", "live"));
    }

    private static Supplier<SseEvent> snapshot(CountDownLatch building, CountDownLatch ready) {
        return () -> {
            building.countDown();
            await(ready);
            return SseEvent.of("queue", Map.of("kind", "snapshot"));
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
