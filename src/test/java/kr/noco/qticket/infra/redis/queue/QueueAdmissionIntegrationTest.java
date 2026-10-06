package kr.noco.qticket.infra.redis.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.domain.queue.AdmissionStatus;
import kr.noco.qticket.infra.redis.ValkeyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QueueAdmissionIntegrationTest {
    private static final int WORKERS = 8;
    private static final AtomicLong PERFORMANCE_IDS = new AtomicLong(100L);
    @Autowired private QueueAdmissionService admissions;
    @Autowired private ValkeyProperties properties;
    @Autowired private StringRedisTemplate redis;

    @DynamicPropertySource
    static void setTestSettings(DynamicPropertyRegistry registry) {
        registry.add("qticket.valkey.queue.capacity", () -> "3");
        registry.add("qticket.valkey.session.ttl", () -> "1s");
    }

    @Test
    void givenMoreRequestsThanCapacity_whenAdmittedConcurrently_thenActiveCountNeverExceedsCapacity() {
        Long performanceId = PERFORMANCE_IDS.incrementAndGet();
        assertConcurrentAdmissions(performanceId);
    }

    @Test
    void givenExpiredAdmission_whenMemberRetries_thenMemberCanReenter() throws InterruptedException {
        Long performanceId = PERFORMANCE_IDS.incrementAndGet();
        String memberId = UUID.randomUUID().toString();
        assertThat(admissions.admit(performanceId, memberId)).isEqualTo(AdmissionStatus.ACTIVE);
        assertThat(admissions.admit(performanceId, memberId)).isEqualTo(AdmissionStatus.ACTIVE);
        assertThat(redis.opsForZSet().zCard(activeKey(performanceId))).isEqualTo(1L);
        Thread.sleep(properties.session().ttl().toMillis() + 250L);
        assertThat(redis.hasKey(sessionKey(performanceId, memberId))).isFalse();
        assertThat(admissions.admit(performanceId, memberId)).isEqualTo(AdmissionStatus.ACTIVE);
    }

    private void assertConcurrentAdmissions(Long performanceId) {
        List<QueueAdmissionTestSupport.Attempt> attempts = QueueAdmissionTestSupport.run(
                admissions, performanceId, WORKERS);
        assertThat(attempts).hasSize(WORKERS)
                .allMatch(attempt -> attempt.failure() == null && attempt.status() != null);
        long admitted = attempts.stream().filter(
                attempt -> attempt.status() == AdmissionStatus.ACTIVE).count();
        assertThat(admitted).isEqualTo(properties.queue().capacity());
        assertThat(redis.opsForZSet().zCard(activeKey(performanceId)))
                .isLessThanOrEqualTo((long) properties.queue().capacity());
        assertThat(redis.opsForZSet().zCard(waitingKey(performanceId)))
                .isEqualTo(WORKERS - properties.queue().capacity());
    }

    private String activeKey(Long performanceId) {
        return "qticket:queue:{" + performanceId + "}:active";
    }

    private String waitingKey(Long performanceId) {
        return "qticket:queue:{" + performanceId + "}:waiting";
    }

    private String sessionKey(Long performanceId, String memberId) {
        return "qticket:queue:{" + performanceId + "}:active:" + memberId;
    }
}
