package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class NodeAHoldToNodeBWebSocketTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "ws-member-1";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenHoldOnNodeA_whenPublished_thenNodeBWsReceivesSeatChanged() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
            assertSeatChangedFlow(nodes.nodeA(), nodes.nodeB());
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private void assertSeatChangedFlow(Node nodeA, Node nodeB) throws Exception {
        Fixture fixture = insertFixture(nodeA);
        try {
            nodeA.bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
            WsClient client = WsClient.connect(nodeB.port(), fixture.performanceId());
            try {
                assertThat(client.awaitContaining("SNAPSHOT", BOUND)).isNotNull();
                holdOnNodeA(nodeA, fixture);
                assertChangedFrame(client.awaitContaining("SEAT_CHANGED", BOUND), fixture);
            } finally {
                client.close();
            }
        } finally {
            deleteFixture(nodeA, fixture);
        }
    }

    private void assertChangedFrame(String frame, Fixture fixture) {
        assertThat(frame).isNotNull();
        JsonNode changed = objectMapper.readTree(frame);
        assertThat(changed.get("type").asText()).isEqualTo("SEAT_CHANGED");
        assertThat(changed.get("performanceId").asLong())
                .isEqualTo(fixture.performanceId());
        assertThat(changed.get("seatId").asLong()).isEqualTo(fixture.seatId());
    }

    private void holdOnNodeA(Node nodeA, Fixture fixture) {
        boolean acquired = nodeA.bean(SeatHoldService.class).hold(fixture.performanceId(),
                fixture.seatId(), HOLDER);
        assertThat(acquired).isTrue();
    }

    private Fixture insertFixture(Node nodeA) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, "two-node ws");
        Long sid = jdbc.queryForObject("insert into seat (performance_id, section, row_label,"
                + " seat_number, sale_status) values (?, 'R', '1', 1, 'AVAILABLE') returning id",
                Long.class, pid);
        return new Fixture(pid, sid);
    }

    private void deleteFixture(Node nodeA, Fixture fixture) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        jdbc.update("delete from ticket where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from booking where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from seat where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from performance where id = ?", fixture.performanceId());
    }

    private record Fixture(Long performanceId, Long seatId) {
    }
}
