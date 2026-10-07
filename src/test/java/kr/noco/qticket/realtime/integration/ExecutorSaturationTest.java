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

/** message 실행기 포화 시 delta 유실 + snapshot 재연결 복구 검증 준비 (T47-43, worker2 소유). */
class ExecutorSaturationTest {
    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "saturation-member-1";
    @Test
    void givenSaturatedExecutor_whenHoldPublished_thenDroppedAndSnapshotRecovers() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            Fixture fixture = insertFixture(nodes.nodeA());
            try {
                assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
                admitAndConnect(nodes, fixture);
            } finally {
                deleteFixture(nodes.nodeA(), fixture);
            }
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }
    private void admitAndConnect(Nodes nodes, Fixture fixture) throws Exception {
        nodes.nodeA().bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
        WsClient client = WsClient.connect(nodes.nodeB().port(), fixture.performanceId());
        try {
            assertThat(client.awaitContaining("SNAPSHOT", BOUND)).isNotNull();
            publishWhileSaturated(nodes, fixture, client);
            assertSnapshotShowsHeld(nodes, fixture);
        } finally {
            client.close();
        }
    }
    private void publishWhileSaturated(Nodes nodes, Fixture fixture, WsClient client) throws Exception {
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
    private void assertSnapshotShowsHeld(Nodes nodes, Fixture fixture) throws Exception {
        // 재연결 snapshot 기준 복구. worker3 자동 push 복구 계약 후 별도 테스트 (재연결만으로 완료 처리 금지).
        WsClient reconnected = WsClient.connect(nodes.nodeB().port(), fixture.performanceId());
        String snapshot = reconnected.awaitContaining("SNAPSHOT", BOUND);
        reconnected.close();
        assertThat(snapshot).contains(fixture.seatId().toString());
        assertThat(snapshot).contains("\"held\":true");
    }
    private Fixture insertFixture(Node nodeA) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject("insert into performance (title, starts_at) values (?, now()) returning id", Long.class, "two-node saturation");
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
