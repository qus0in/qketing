package kr.noco.qticket.infra.redis.hold;

import java.util.List;
import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.infra.redis.ValkeyProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class ValkeySeatHoldAdapter implements SeatHoldPort {

    private final StringRedisTemplate redis;
    private final ValkeyProperties properties;
    private final DefaultRedisScript<Long> releaseScript;

    public ValkeySeatHoldAdapter(StringRedisTemplate redis, ValkeyProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.releaseScript = loadReleaseScript();
    }

    @Override
    public boolean hold(Long performanceId, Long seatId, String holderId) {
        String key = key(performanceId, seatId);
        Boolean acquired = redis.opsForValue().setIfAbsent(key, holderId,
                properties.hold().ttl());
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public boolean isHeldBy(Long performanceId, Long seatId, String holderId) {
        String current = redis.opsForValue().get(key(performanceId, seatId));
        return holderId.equals(current);
    }

    @Override
    public boolean release(Long performanceId, Long seatId, String holderId) {
        String key = key(performanceId, seatId);
        Long deleted = redis.execute(releaseScript, List.of(key), holderId);
        return Long.valueOf(1L).equals(deleted);
    }

    private static DefaultRedisScript<Long> loadReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/hold-release.lua"));
        script.setResultType(Long.class);
        return script;
    }

    private String key(Long performanceId, Long seatId) {
        return "qticket:hold:{" + performanceId + "}:" + seatId;
    }
}
