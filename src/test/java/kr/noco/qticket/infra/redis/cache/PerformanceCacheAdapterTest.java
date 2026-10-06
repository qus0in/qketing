package kr.noco.qticket.infra.redis.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.performance.PerformancePersistencePort;
import kr.noco.qticket.domain.performance.Performance;
import kr.noco.qticket.infra.persistence.performance.PerformanceEntity;
import kr.noco.qticket.infra.persistence.performance.PerformanceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/** performance read cache miss/hit/TTL 만료 검증 (#40). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PerformanceCacheAdapterTest {

    @Autowired
    private PerformancePersistencePort cache;

    @Autowired
    private PerformanceJpaRepository performances;

    @Autowired
    private StringRedisTemplate redis;

    private Long performanceId;

    @AfterEach
    void cleanUp() {
        if (performanceId != null) {
            performances.deleteById(performanceId);
            redis.delete(cachedKey());
        }
        performanceId = null;
    }

    @Test
    void givenCacheMiss_whenFound_thenLoadsFromDatabaseAndCaches() {
        performanceId = savePerformance();

        Optional<Performance> found = cache.findById(performanceId);

        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("캐시 검증 공연");
        assertThat(redis.hasKey(cachedKey())).isTrue();
    }

    @Test
    void givenDatabaseRowRemoved_whenFoundAgain_thenServedFromCache() {
        performanceId = savePerformance();
        cache.findById(performanceId);
        performances.deleteById(performanceId);

        Optional<Performance> second = cache.findById(performanceId);

        assertThat(second).isPresent();
        assertThat(second.get().title()).isEqualTo("캐시 검증 공연");
    }

    @Test
    void givenExpiredCache_whenFoundAgain_thenLoadsFromDatabaseAgain() throws Exception {
        performanceId = savePerformance();
        cache.findById(performanceId);
        redis.expire(cachedKey(), Duration.ofSeconds(1));
        Thread.sleep(1_200);

        assertThat(redis.hasKey(cachedKey())).isFalse();
        Optional<Performance> found = cache.findById(performanceId);

        assertThat(found).isPresent();
        assertThat(redis.hasKey(cachedKey())).isTrue();
    }

    private Long savePerformance() {
        return performances.saveAndFlush(
                PerformanceEntity.of("캐시 검증 공연", Instant.parse("2026-11-01T10:00:00Z")))
                .getId();
    }

    private String cachedKey() {
        return "qticket:cache:performance:" + performanceId + ":detail:" + performanceId;
    }
}
