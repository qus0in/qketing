package kr.noco.qticket.infra.redis.hold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeatHoldIntegrationTest {
    private static final int WORKERS = 8;
    private static final AtomicLong IDS = new AtomicLong(5000L);
    @Autowired private SeatHoldService holds;
    @Autowired private QueueAdmissionService admissions;
    @Autowired private StringRedisTemplate redis;
    @DynamicPropertySource
    static void holdTtl(DynamicPropertyRegistry registry) {
        registry.add("qticket.valkey.hold.ttl", () -> "1s");
    }
    @Test
    void givenSameSeat_whenHeldConcurrently_thenOnlyOneSucceeds() throws Exception {
        Long pid = IDS.incrementAndGet();
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        List<Future<?>> futures = submit(pool, pid, start, wins);
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdownNow();
        assertThat(wins.get()).isEqualTo(1);
    }
    @Test
    void givenExpiredHold_whenHeldAgain_thenReacquired() throws Exception {
        Long pid = IDS.incrementAndGet();
        admissions.admit(pid, "owner-a");
        assertThat(holds.hold(pid, 7L, "owner-a")).isTrue();
        Thread.sleep(1300L);
        admissions.admit(pid, "owner-b");
        assertThat(holds.hold(pid, 7L, "owner-b")).isTrue();
        assertThat(redis.opsForValue().get(key(pid, 7L))).isEqualTo("owner-b");
    }
    @Test
    void givenHoldByOther_whenReleasedByNonOwner_thenKept() {
        Long pid = IDS.incrementAndGet();
        admissions.admit(pid, "owner");
        assertThat(holds.hold(pid, 9L, "owner")).isTrue();
        assertThat(holds.release(pid, 9L, "intruder-" + UUID.randomUUID())).isFalse();
        assertThat(redis.opsForValue().get(key(pid, 9L))).isEqualTo("owner");
        assertThat(holds.release(pid, 9L, "owner")).isTrue();
    }
    private List<Future<?>> submit(ExecutorService pool, Long pid, CountDownLatch s, AtomicInteger w) {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < WORKERS; i++) {
            futures.add(pool.submit(() -> holdAfterStart(pid, s, w)));
        }
        return futures;
    }
    private void holdAfterStart(Long pid, CountDownLatch start, AtomicInteger wins) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        String holder = UUID.randomUUID().toString();
        admissions.admit(pid, holder);
        if (holds.hold(pid, 1L, holder)) {
            wins.incrementAndGet();
        }
    }
    private String key(Long pid, Long seatId) {
        return "qticket:hold:{" + pid + "}:" + seatId;
    }
}
