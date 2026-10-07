package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 실제 Valkey TCP 절단(CLIENT KILL pubsub) 후 자동재구독 수신 검증 준비. 절단 후 publish 유실 윈도우는 expected이며 subscription executor 거부 회복과는 별도 범위다 (T47-46, worker2 소유). */
class PubSubTcpRecoveryTest {
    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "tcp-recovery-member-1";
    private static final int SEATS = 3;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Test
    void givenTcpKilled_whenResubscribed_thenNewEventReceivedWithoutRestart() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertTcpRecoveryFlow(nodes);
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }
    private void assertTcpRecoveryFlow(Nodes nodes) throws Exception {
        Fixture fixture = insertFixture(nodes.nodeA());
        try {
            nodes.nodeA().bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
            assertThat(pubsubCount(nodes)).isGreaterThan(0);
            WsClient client = WsClient.connect(nodes.nodeB().port(), fixture.performanceId());
            try {
                assertThat(client.awaitContaining("SNAPSHOT", BOUND)).isNotNull();
                killPubsub(nodes);
                assertPostRecoveryEvent(nodes, fixture, client);
            } finally {
                client.close();
            }
        } finally {
            deleteFixture(nodes.nodeA(), fixture);
        }
    }
    private void assertPostRecoveryEvent(Nodes nodes, Fixture fixture, WsClient client) throws Exception {
        String received = null;
        for (int seat = 0; seat < SEATS && received == null; seat++) {
            boolean acquired = nodes.nodeA().bean(SeatHoldService.class).hold(fixture.performanceId(), fixture.seatIds().get(seat), HOLDER);
            assertThat(acquired).isTrue();
            received = client.awaitContaining("SEAT_CHANGED", Duration.ofSeconds(5));
        }
        assertChangedFrame(received, fixture);
    }
    private void assertChangedFrame(String frame, Fixture fixture) {
        assertThat(frame).isNotNull();
        JsonNode event = objectMapper.readTree(frame);
        assertThat(event.get("type").asText()).isEqualTo("SEAT_CHANGED");
        assertThat(event.get("performanceId").asLong()).isEqualTo(fixture.performanceId());
        assertThat(fixture.seatIds()).contains(event.get("seatId").asLong());
        assertThat(event.get("eventId").asText()).isNotBlank();
    }
    private void killPubsub(Nodes nodes) {
        long killed = PubSubCommands.killPubsub(nodes.nodeA().bean(StringRedisTemplate.class));
        assertThat(killed).isGreaterThan(0L);
        System.out.println("valkey pubsub killed=" + killed);
    }
    private long pubsubCount(Nodes nodes) {
        return PubSubCommands.pubsubCount(nodes.nodeA().bean(StringRedisTemplate.class));
    }
    private Fixture insertFixture(Node nodeA) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject("insert into performance (title, starts_at) values (?, now()) returning id", Long.class, "two-node tcp");
        List<Long> seats = new ArrayList<>();
        for (int number = 1; number <= SEATS; number++) {
            seats.add(jdbc.queryForObject("insert into seat (performance_id, section, row_label, seat_number, sale_status) values (?, 'R', '1', ?, 'AVAILABLE') returning id", Long.class, pid, number));
        }
        return new Fixture(pid, seats);
    }
    private void deleteFixture(Node nodeA, Fixture fixture) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        jdbc.update("delete from ticket where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from booking where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from seat where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from performance where id = ?", fixture.performanceId());
    }
    private record Fixture(Long performanceId, List<Long> seatIds) {}
}
