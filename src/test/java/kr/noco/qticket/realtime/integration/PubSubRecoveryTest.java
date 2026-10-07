package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.jdbc.core.JdbcTemplate;

/** Valkey Pub/Sub 구독 절단·재구독 후 실제 수신 복구 검증 준비 (T47-43, worker2 소유). */
class PubSubRecoveryTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "recovery-member-1";

    @Test
    void givenResubscribed_whenHoldPublished_thenNodeBReceivesAgain() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
            assertRecoveryFlow(nodes);
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private void assertRecoveryFlow(Nodes nodes) throws Exception {
        Fixture fixture = insertFixture(nodes.nodeA());
        try {
            nodes.nodeA().bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
            WsClient client = WsClient.connect(nodes.nodeB().port(), fixture.performanceId());
            try {
                assertThat(client.awaitContaining("SNAPSHOT", BOUND)).isNotNull();
                RedisMessageListenerContainer container = seatContainer(nodes);
                container.stop();
                holdOnA(nodes.nodeA(), fixture, 1);
                assertThat(client.awaitContaining("SEAT_CHANGED", Duration.ofSeconds(2))).isNull();
                container.start();
                holdOnA(nodes.nodeA(), fixture, 2);
                String changed = client.awaitContaining("SEAT_CHANGED", BOUND);
                assertThat(changed).contains(fixture.secondSeatId().toString());
            } finally {
                client.close();
            }
        } finally {
            deleteFixture(nodes.nodeA(), fixture);
        }
    }

    private RedisMessageListenerContainer seatContainer(Nodes nodes) {
        return nodes.nodeB().context().getBean("seatRealtimeListenerContainer",
                RedisMessageListenerContainer.class);
    }

    private void holdOnA(Node nodeA, Fixture fixture, int seatNumber) {
        Long seatId = seatNumber == 1 ? fixture.firstSeatId() : fixture.secondSeatId();
        boolean acquired = nodeA.bean(SeatHoldService.class).hold(fixture.performanceId(),
                seatId, HOLDER);
        assertThat(acquired).isTrue();
    }

    private Fixture insertFixture(Node nodeA) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, "two-node recovery");
        Long first = insertSeat(jdbc, pid, 1);
        return new Fixture(pid, first, insertSeat(jdbc, pid, 2));
    }

    private Long insertSeat(JdbcTemplate jdbc, Long pid, int number) {
        return jdbc.queryForObject("insert into seat (performance_id, section, row_label,"
                + " seat_number, sale_status) values (?, 'R', '1', ?, 'AVAILABLE') returning id",
                Long.class, pid, number);
    }

    private void deleteFixture(Node nodeA, Fixture fixture) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        jdbc.update("delete from ticket where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from booking where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from seat where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from performance where id = ?", fixture.performanceId());
    }

    private record Fixture(Long performanceId, Long firstSeatId, Long secondSeatId) {
    }
}
