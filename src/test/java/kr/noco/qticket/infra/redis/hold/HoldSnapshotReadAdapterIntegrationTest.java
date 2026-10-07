package kr.noco.qticket.infra.redis.hold;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import kr.noco.qticket.realtime.support.RealtimeFixtureSql;
import kr.noco.qticket.realtime.support.RealtimeFixtureSql.SeatFixture;
import kr.noco.qticket.realtime.support.SharedRealtimeInfrastructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** hold 키 TTL 실조회 검증: 실 PTTL·없는 키·만료 키(값 조회 없음 → holder 미노출). */
@SpringBootTest
class HoldSnapshotReadAdapterIntegrationTest {

    @Autowired private HoldSnapshotReadAdapter adapter;
    @Autowired private StringRedisTemplate redis;
    @Autowired private JdbcTemplate jdbc;

    private RealtimeFixtureSql fixtures;

    @DynamicPropertySource
    static void sharedInfrastructure(DynamicPropertyRegistry registry) {
        SharedRealtimeInfrastructure infra = SharedRealtimeInfrastructure.start();
        infra.properties().forEach((key, value) -> registry.add(key, () -> value));
    }

    @BeforeEach
    void setUpFixtures() {
        fixtures = new RealtimeFixtureSql(jdbc);
    }

    @Test
    void givenHoldKeyWithTtl_whenRead_thenPositiveTtlWithinRange() {
        SeatFixture fixture = fixtures.seat();
        String key = fixtures.holdKey(fixture.performanceId(), fixture.seatId());
        redis.opsForValue().set(key, "holder-1", Duration.ofMillis(5000));

        Map<Long, Long> ttls =
                adapter.holdTtlMillis(fixture.performanceId(), List.of(fixture.seatId()));
        redis.delete(key);

        Long ttl = ttls.get(fixture.seatId());
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(5000L);
        assertThat(ttls).containsOnlyKeys(fixture.seatId());
    }

    @Test
    void givenMissingKey_whenRead_thenEmptyMap() {
        SeatFixture fixture = fixtures.seat();

        Map<Long, Long> ttls =
                adapter.holdTtlMillis(fixture.performanceId(), List.of(fixture.seatId()));

        assertThat(ttls).isEmpty();
    }

    @Test
    void givenExpiredKey_whenRead_thenAbsent() throws InterruptedException {
        SeatFixture fixture = fixtures.seat();
        String key = fixtures.holdKey(fixture.performanceId(), fixture.seatId());
        redis.opsForValue().set(key, "holder-1", Duration.ofMillis(50));
        Thread.sleep(200L);

        Map<Long, Long> ttls =
                adapter.holdTtlMillis(fixture.performanceId(), List.of(fixture.seatId()));

        assertThat(ttls).isEmpty();
    }
}
