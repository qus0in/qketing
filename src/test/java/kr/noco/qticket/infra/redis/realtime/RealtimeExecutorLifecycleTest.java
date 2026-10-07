package kr.noco.qticket.infra.redis.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** message 실행기(포화 드롭)와 subscription 실행기(명시 거부) 정책 검증 (T47-30). */
class RealtimeExecutorLifecycleTest {

    @Test
    void givenSaturatedQueue_whenMessageDispatched_thenDroppedWithoutThrowing() throws Exception {
        ThreadPoolTaskExecutor executor = messageExecutor();
        executor.initialize();
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger dropped = new AtomicInteger();
        try {
            executor.execute(() -> await(blocked, release));
            assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
            executor.execute(() -> { });
            for (int i = 0; i < 3; i++) {
                executor.execute(dropped::incrementAndGet);
            }
            assertThat(dropped.get()).isZero();
        } finally {
            release.countDown();
            executor.destroy();
        }
    }

    @Test
    void givenSaturatedSubscriptionExecutor_whenSubmitted_thenExplicitRejection() throws Exception {
        ThreadPoolTaskExecutor executor =
                ValkeyRealtimeConfiguration.boundedExecutor("test-sub-", 1, 1, 1,
                        new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            executor.execute(() -> await(blocked, release));
            assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
            executor.execute(() -> { });
            assertThatThrownBy(() -> executor.execute(() -> { }))
                    .isInstanceOf(RejectedExecutionException.class);
        } finally {
            release.countDown();
            executor.destroy();
        }
    }

    @Test
    void givenExecutorBean_whenDestroyed_thenTerminated() throws Exception {
        ThreadPoolTaskExecutor executor = messageExecutor();
        executor.initialize();
        executor.execute(() -> { });

        executor.destroy();

        assertThat(executor.getThreadPoolExecutor().isShutdown()).isTrue();
        assertThat(executor.getThreadPoolExecutor().isTerminated()).isTrue();
    }

    private ThreadPoolTaskExecutor messageExecutor() {
        return ValkeyRealtimeConfiguration.boundedExecutor("test-msg-", 1, 1, 1,
                new RealtimeRejectedExecutionHandler());
    }

    private void await(CountDownLatch blocked, CountDownLatch release) {
        blocked.countDown();
        try {
            release.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
