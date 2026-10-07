package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** pending 상한 초과 시 연결을 닫아 클라이언트가 재연결 snapshot을 받게 한다. */
class SseConnectionManagerBufferTest {

    private static final Long PERFORMANCE_ID = 7L;

    @Test
    void givenPendingOverflow_whenBuffering_thenConnectionClosed() throws Exception {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = mock(SseEmitter.class);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");

        Thread registration = new Thread(
                () -> manager.registerWithSnapshot(key, emitter, snapshot(building, ready)));
        registration.start();
        assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();

        boolean delivered = true;
        for (int index = 0; index < 300; index++) {
            delivered = manager.emit(key, SseEvent.of("queue", "v" + index));
        }

        assertThat(delivered).isFalse();
        assertThat(manager.subscriberKeys()).isEmpty();

        ready.countDown();
        registration.join(5000);
    }

    @Test
    void givenPendingAtLimit_whenBuffered_thenOverflowClosesConnection() throws Exception {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = mock(SseEmitter.class);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");

        Thread registration = new Thread(
                () -> manager.registerWithSnapshot(key, emitter, snapshot(building, ready)));
        registration.start();
        assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();

        boolean withinLimit = true;
        for (int index = 0; index < 256; index++) {
            withinLimit &= manager.emit(key, SseEvent.of("queue", "v" + index));
        }
        assertThat(withinLimit).isTrue();
        assertThat(manager.subscriberKeys()).hasSize(1);

        assertThat(manager.emit(key, SseEvent.of("queue", "overflow"))).isFalse();
        assertThat(manager.subscriberKeys()).isEmpty();

        ready.countDown();
        registration.join(5000);
    }

    private static Supplier<SseEvent> snapshot(CountDownLatch building, CountDownLatch ready) {
        return () -> {
            building.countDown();
            await(ready);
            return SseEvent.of("queue", "snapshot");
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
