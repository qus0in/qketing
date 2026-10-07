package kr.noco.qticket.infra.persistence.seat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import kr.noco.qticket.app.realtime.snapshot.SeatSaleState;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotEntry;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
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

/** 공유 PG의 sale_status 실조회 + SOLD 우선 병합 검증(컨테이너 1세트 공유, bean 생성 없음). */
@SpringBootTest
class SeatSnapshotReadAdapterIntegrationTest {

    @Autowired private SeatSnapshotReadAdapter adapter;
    @Autowired private SeatSnapshotService snapshots;
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
    void givenAvailableSeat_whenRead_thenStateReadFromPostgres() {
        SeatFixture fixture = fixtures.seat();

        List<SeatSaleState> states = adapter.findByPerformanceId(fixture.performanceId());

        assertThat(states).containsExactly(
                new SeatSaleState(fixture.seatId(), SeatSnapshotEntry.AVAILABLE));
    }

    @Test
    void givenSoldSeat_whenRead_thenSoldReadFromPostgres() {
        SeatFixture fixture = fixtures.seat();
        fixtures.markSold(fixture.seatId());

        List<SeatSaleState> states = adapter.findByPerformanceId(fixture.performanceId());

        assertThat(states).containsExactly(
                new SeatSaleState(fixture.seatId(), SeatSnapshotEntry.SOLD));
    }

    @Test
    void givenSoldSeatWithHoldKey_whenSnapshot_thenSoldWinsWithZeroTtl() {
        SeatFixture fixture = fixtures.seat();
        String key = fixtures.holdKey(fixture.performanceId(), fixture.seatId());
        redis.opsForValue().set(key, "holder-1", Duration.ofSeconds(30));
        fixtures.markSold(fixture.seatId());

        SeatSnapshot snapshot = snapshots.snapshot(fixture.performanceId());
        redis.delete(key);

        assertThat(snapshot.seats()).containsExactly(
                new SeatSnapshotEntry(fixture.seatId(), SeatSnapshotEntry.SOLD, false, 0L));
    }
}
