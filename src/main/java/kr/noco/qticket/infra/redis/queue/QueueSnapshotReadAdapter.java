package kr.noco.qticket.infra.redis.queue;

import java.time.Instant;
import java.util.List;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotReadPort;
import kr.noco.qticket.infra.redis.ValkeyProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/** queue 상태를 read Lua 한 번으로 원자 조회한다 (기존 queue-admission.lua는 수정하지 않는다). */
@Repository
public class QueueSnapshotReadAdapter implements QueueSnapshotReadPort {

    private static final String PREFIX = "qticket:queue:";
    private static final long ACTIVE = 1L;
    private static final long WAITING = 2L;

    private final StringRedisTemplate redis;
    private final ValkeyProperties properties;
    private final DefaultRedisScript<List> snapshotScript;

    public QueueSnapshotReadAdapter(StringRedisTemplate redis, ValkeyProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.snapshotScript = loadScript();
    }

    @Override
    public QueueSnapshot read(Long performanceId, String memberId) {
        String activeKey = PREFIX + "{" + performanceId + "}:active";
        String waitingKey = PREFIX + "{" + performanceId + "}:waiting";
        List<?> result = redis.execute(snapshotScript,
                List.of(activeKey, waitingKey, activeKey + ":" + memberId), memberId,
                String.valueOf(properties.session().ttl().toMillis()));
        return toSnapshot(performanceId, result);
    }

    private QueueSnapshot toSnapshot(Long performanceId, List<?> result) {
        long code = number(result, 0);
        Instant capturedAt = Instant.now();
        if (code == ACTIVE) {
            return new QueueSnapshot(performanceId, QueueSnapshot.Status.ACTIVE, 0,
                    number(result, 1), capturedAt);
        }
        if (code == WAITING) {
            return new QueueSnapshot(performanceId, QueueSnapshot.Status.WAITING,
                    (int) number(result, 1), 0L, capturedAt);
        }
        return new QueueSnapshot(performanceId, QueueSnapshot.Status.ABSENT, 0, 0L, capturedAt);
    }

    private long number(List<?> result, int index) {
        if (result == null || result.size() <= index || !(result.get(index) instanceof Number value)) {
            return 0L;
        }
        return value.longValue();
    }

    private static DefaultRedisScript<List> loadScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/queue-snapshot.lua"));
        script.setResultType(List.class);
        return script;
    }
}
