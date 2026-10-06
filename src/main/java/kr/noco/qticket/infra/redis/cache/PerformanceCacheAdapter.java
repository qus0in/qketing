package kr.noco.qticket.infra.redis.cache;

import java.util.Optional;
import kr.noco.qticket.app.performance.PerformancePersistencePort;
import kr.noco.qticket.domain.performance.Performance;
import kr.noco.qticket.infra.persistence.performance.PerformancePersistenceAdapter;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 공연 메타데이터 read cache (#40). 좌석 판매 상태·booking은 캐시하지 않는다 (#8). */
@Primary
@Repository
public class PerformanceCacheAdapter implements PerformancePersistencePort {

    private static final String KEY_PREFIX = "qticket:cache:performance:";

    private final PerformancePersistenceAdapter performances;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    public PerformanceCacheAdapter(PerformancePersistenceAdapter performances,
            StringRedisTemplate redis, ObjectMapper objectMapper, CacheProperties properties) {
        this.performances = performances;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<Performance> findById(Long performanceId) {
        String key = key(performanceId);
        Performance cached = read(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Performance> found = performances.findById(performanceId);
        found.ifPresent(performance -> write(key, performance));
        return found;
    }

    private Performance read(String key) {
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Performance.class);
        } catch (JacksonException ignored) {
            redis.delete(key);
            return null;
        }
    }

    private void write(String key, Performance performance) {
        redis.opsForValue().set(key, objectMapper.writeValueAsString(performance),
                properties.performanceTtl());
    }

    private String key(Long performanceId) {
        return KEY_PREFIX + performanceId + ":detail:" + performanceId;
    }
}
