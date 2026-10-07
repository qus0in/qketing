package kr.noco.qticket.infra.redis.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotService;
import kr.noco.qticket.domain.queue.AdmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** queue admission Lua의 transition-only PUBLISH와 waiting TTL/active 정합성을 검증한다 (T47-22). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QueueAdmissionPubSubIntegrationTest {

    private static final AtomicLong IDS = new AtomicLong(700L);
    private static final long CAPACITY = 1L;

    @Autowired private QueueAdmissionService admissions;
    @Autowired private QueueSnapshotService snapshots;
    @Autowired private StringRedisTemplate redis;
    @Autowired
    @Qualifier("queueRealtimeListenerContainer")
    private RedisMessageListenerContainer container;

    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry registry) {
        registry.add("qticket.valkey.queue.capacity", () -> String.valueOf(CAPACITY));
        registry.add("qticket.valkey.session.ttl", () -> "1s");
    }

    @Test
    void givenActiveRetryAndDuplicateWaiting_whenAdmitted_thenOnlyTransitionsPublished()
            throws Exception {
        Long pid = IDS.incrementAndGet();
        List<String> events = new CopyOnWriteArrayList<>();
        subscribe(pid, events);

        String active = "active-" + UUID.randomUUID();
        String waiting = "waiting-" + UUID.randomUUID();
        assertThat(admissions.admit(pid, active)).isEqualTo(AdmissionStatus.ACTIVE);
        assertThat(admissions.admit(pid, waiting)).isEqualTo(AdmissionStatus.WAITING);
        assertThat(admissions.admit(pid, active)).isEqualTo(AdmissionStatus.ACTIVE);
        assertThat(admissions.admit(pid, waiting)).isEqualTo(AdmissionStatus.WAITING);
        Thread.sleep(500L);

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).contains("\"status\":\"ACTIVE\"", active);
        assertThat(events.get(1)).contains("\"status\":\"WAITING\"", waiting);
    }

    @Test
    void givenExpiredWaitingMember_whenSnapshot_thenWaitingCleanedAndAbsent() throws Exception {
        Long pid = IDS.incrementAndGet();
        assertThat(admissions.admit(pid, "active-" + UUID.randomUUID()))
                .isEqualTo(AdmissionStatus.ACTIVE);
        String waiting = "waiting-" + UUID.randomUUID();
        assertThat(admissions.admit(pid, waiting)).isEqualTo(AdmissionStatus.WAITING);
        assertThat(waitingScore(pid, waiting)).isNotNull();

        Thread.sleep(1300L);

        assertThat(snapshots.snapshot(pid, waiting).status()).isEqualTo(QueueSnapshot.Status.ABSENT);
        assertThat(waitingScore(pid, waiting)).isNull();
    }

    private void subscribe(Long performanceId, List<String> events) throws Exception {
        container.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                events.add(new String(message.getBody(), StandardCharsets.UTF_8));
            }
        }, new ChannelTopic("qticket:queue-events:{" + performanceId + "}"));
        Thread.sleep(300L);
    }

    private Double waitingScore(Long pid, String member) {
        return redis.opsForZSet().score("qticket:queue:{" + pid + "}:waiting", member);
    }
}
