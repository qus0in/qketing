package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kr.noco.qticket.app.booking.ConfirmBookingCommand;
import kr.noco.qticket.app.booking.ConfirmBookingService;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ReconnectSnapshotRecoveryTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "ws-member-2";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenSoldDuringDisconnect_whenReconnect_thenSnapshotRestoresFromSource() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
            assertReconnectFlow(nodes.nodeA(), nodes.nodeB());
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private void assertReconnectFlow(Node nodeA, Node nodeB) throws Exception {
        Fixture fixture = insertFixture(nodeA);
        try {
            admitAndHold(nodeA, fixture);
            WsClient first = WsClient.connect(nodeB.port(), fixture.performanceId());
            assertThat(first.awaitContaining("SNAPSHOT", BOUND)).isNotNull();
            first.close();
            assertSoldAfterReconnect(nodeA, nodeB, fixture);
        } finally {
            deleteFixture(nodeA, fixture);
        }
    }

    private void assertSoldAfterReconnect(Node nodeA, Node nodeB, Fixture fixture)
            throws Exception {
        nodeA.bean(ConfirmBookingService.class).confirm(new ConfirmBookingCommand(fixture.performanceId(), fixture.seatId(), "reconnect-key-1", HOLDER));
        WsClient second = WsClient.connect(nodeB.port(), fixture.performanceId());
        try {
            assertSoldSnapshot(second.awaitContaining("SNAPSHOT", BOUND), fixture);
        } finally {
            second.close();
        }
    }

    private void assertSoldSnapshot(String frame, Fixture fixture) {
        assertThat(frame).isNotNull();
        JsonNode snapshot = objectMapper.readTree(frame);
        assertThat(snapshot.get("type").asText()).isEqualTo("SNAPSHOT");
        assertThat(snapshot.get("performanceId").asLong()).isEqualTo(fixture.performanceId());
        assertThat(findSold(snapshot.get("seats"), fixture)).isTrue();
    }

    private boolean findSold(JsonNode seats, Fixture fixture) {
        for (JsonNode entry : seats) {
            if (entry.get("seatId").asLong() == fixture.seatId()
                    && "SOLD".equals(entry.get("saleStatus").asText())
                    && !entry.get("held").asBoolean()
                    && entry.get("holdTtlMillis").asLong() == 0L) {
                return true;
            }
        }
        return false;
    }

    private void admitAndHold(Node nodeA, Fixture fixture) {
        nodeA.bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
        boolean acquired = nodeA.bean(SeatHoldService.class).hold(fixture.performanceId(), fixture.seatId(), HOLDER);
        assertThat(acquired).isTrue();
    }
    private Fixture insertFixture(Node nodeA) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject("insert into performance (title, starts_at) values (?, now()) returning id", Long.class, "two-node reconnect");
        Long sid = jdbc.queryForObject("insert into seat (performance_id, section, row_label, seat_number, sale_status) values (?, 'R', '1', 1, 'AVAILABLE') returning id", Long.class, pid);
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
