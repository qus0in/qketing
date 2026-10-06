package kr.noco.qticket.infra.redis.queue;

import java.util.List;
import kr.noco.qticket.app.queue.QueueAdmissionPort;
import kr.noco.qticket.domain.queue.AdmissionStatus;
import kr.noco.qticket.infra.redis.ValkeyProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class ValkeyQueueAdapter implements QueueAdmissionPort {
    private static final String KEY_PREFIX = "qticket:queue:";
    private static final long ACTIVE_RESULT = 1L;
    private final StringRedisTemplate redis;
    private final ValkeyProperties properties;
    private final DefaultRedisScript<Long> admissionScript;

    public ValkeyQueueAdapter(StringRedisTemplate redis, ValkeyProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.admissionScript = loadAdmissionScript();
    }

    @Override
    public AdmissionStatus admit(Long performanceId, String memberId) {
        String activeKey = activeKey(performanceId);
        String waitingKey = waitingKey(performanceId);
        Long result = redis.execute(admissionScript, List.of(activeKey, waitingKey,
                activeSessionKey(performanceId, memberId)), memberId,
                String.valueOf(properties.queue().capacity()),
                String.valueOf(properties.session().ttl().toMillis()));
        if (result == null) {
            throw new IllegalStateException("Queue admission script returned no result");
        }
        return result == ACTIVE_RESULT ? AdmissionStatus.ACTIVE : AdmissionStatus.WAITING;
    }

    @Override
    public boolean isActive(Long performanceId, String memberId) {
        return Boolean.TRUE.equals(redis.hasKey(activeSessionKey(performanceId, memberId)));
    }

    private static DefaultRedisScript<Long> loadAdmissionScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/queue-admission.lua"));
        script.setResultType(Long.class);
        return script;
    }

    private String activeKey(Long performanceId) {
        return KEY_PREFIX + "{" + performanceId + "}:active";
    }

    private String waitingKey(Long performanceId) {
        return KEY_PREFIX + "{" + performanceId + "}:waiting";
    }

    private String activeSessionKey(Long performanceId, String memberId) {
        return KEY_PREFIX + "{" + performanceId + "}:active:" + memberId;
    }
}
