package kr.noco.qticket.infra.redis.hold;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.app.realtime.snapshot.HoldTtlSnapshotReadPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * hold 키의 TTL만 조회한다(PTTL 단독 호출, 값 조회 없음 → holderId 비노출).
 * 키 없음(-2)·무만료(-1)·TTL 0 이하는 held로 세지 않고 map에서 제외한다.
 */
@Repository
public class HoldSnapshotReadAdapter implements HoldTtlSnapshotReadPort {

    private final StringRedisTemplate redis;

    public HoldSnapshotReadAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Map<Long, Long> holdTtlMillis(Long performanceId, List<Long> seatIds) {
        Map<Long, Long> ttlBySeat = new LinkedHashMap<>();
        for (Long seatId : seatIds) {
            Long ttl = redis.getExpire(key(performanceId, seatId), TimeUnit.MILLISECONDS);
            if (ttl != null && ttl > 0) {
                ttlBySeat.put(seatId, ttl);
            }
        }
        return ttlBySeat;
    }

    private static String key(Long performanceId, Long seatId) {
        return "qticket:hold:{" + performanceId + "}:" + seatId;
    }
}
