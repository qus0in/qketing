package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** message 실행기 포화 helper. assert는 항상 latch 해제 범위에서 호출한다 (T47-46 보완, worker2 소유). */
final class Saturation {
    static final int TASKS = 520;
    static final int EXPECTED_ACTIVE = 8;
    static final int EXPECTED_QUEUED = 512;

    private Saturation() {
    }

    static List<CountDownLatch> saturate(Node node) {
        ThreadPoolTaskExecutor executor = node.context().getBean("realtimeMessageExecutor", ThreadPoolTaskExecutor.class);
        List<CountDownLatch> latches = new ArrayList<>();
        for (int i = 0; i < TASKS; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            latches.add(latch);
            executor.execute(() -> awaitRelease(latch));
        }
        return latches;
    }

    static void awaitSaturated(Node node) throws InterruptedException {
        Thread.sleep(2000L);
        ThreadPoolTaskExecutor executor = node.context().getBean("realtimeMessageExecutor", ThreadPoolTaskExecutor.class);
        assertThat(executor.getActiveCount()).isEqualTo(EXPECTED_ACTIVE);
        assertThat(executor.getQueueSize()).isEqualTo(EXPECTED_QUEUED);
    }

    static void release(List<CountDownLatch> latches) {
        latches.forEach(CountDownLatch::countDown);
    }

    private static void awaitRelease(CountDownLatch latch) {
        try {
            latch.await(30L, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
