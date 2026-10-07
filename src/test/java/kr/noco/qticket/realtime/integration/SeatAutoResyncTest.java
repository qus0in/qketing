package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 포화 유실 후 재연결 없이 주기 resync SNAPSHOT으로 복구되는지 검증 준비 (T47-46 보완, worker2 소유). */
class SeatAutoResyncTest {
    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "resync-member-1";
    private static final String RESYNC = "--qticket.realtime.seat-websocket.resync-interval=1s";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Test
    void givenDroppedEvent_whenResyncTicks_thenSameConnectionShowsHeld() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair(RESYNC);
        try {
            Fixture fixture = insertFixture(nodes.nodeA());
            try {
                assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
                assertResyncFlow(nodes, fixture);
            } finally {
                deleteFixture(nodes.nodeA(), fixture);
            }
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }
    private void assertResyncFlow(Nodes nodes, Fixture fixture) throws Exception {
        nodes.nodeA().bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
        WsClient client = WsClient.connect(nodes.nodeB().port(), fixture.performanceId());
        try {
            assertThat(client.awaitContaining("SNAPSHOT", BOUND)).contains("\"held\":false");
            dropWhileSaturated(nodes, fixture, client);
            awaitResynced(client, fixture);
        } finally {
            client.close();
        }
    }
    private void dropWhileSaturated(Nodes nodes, Fixture fixture, WsClient client) throws Exception {
        List<CountDownLatch> latches = Saturation.saturate(nodes.nodeB());
        try {
            Saturation.awaitSaturated(nodes.nodeB());
            boolean acquired = nodes.nodeA().bean(SeatHoldService.class).hold(fixture.performanceId(), fixture.seatId(), HOLDER);
            assertThat(acquired).isTrue();
            assertThat(client.awaitContaining("SEAT_CHANGED", Duration.ofSeconds(2))).isNull();
        } finally {
            Saturation.release(latches);
        }
    }
    private void awaitResynced(WsClient client, Fixture fixture) throws Exception {
        long deadline = System.currentTimeMillis() + BOUND.toMillis();
        boolean synced = false;
        while (!synced && System.currentTimeMillis() < deadline) {
            synced = heldTrue(client.awaitContaining("SNAPSHOT", Duration.ofSeconds(2)), fixture);
        }
        assertThat(synced).isTrue();
    }
    private boolean heldTrue(String frame, Fixture fixture) {
        if (frame == null) {
            return false;
        }
        JsonNode snapshot = objectMapper.readTree(frame);
        for (JsonNode entry : snapshot.get("seats")) {
            if (entry.get("seatId").asLong() == fixture.seatId()) {
                return entry.get("held").asBoolean();
            }
        }
        return false;
    }
    private Fixture insertFixture(Node nodeA) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject("insert into performance (title, starts_at) values (?, now()) returning id", Long.class, "two-node resync");
        return new Fixture(pid, jdbc.queryForObject("insert into seat (performance_id, section, row_label, seat_number, sale_status) values (?, 'R', '1', 1, 'AVAILABLE') returning id", Long.class, pid));
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
