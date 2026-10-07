package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import kr.noco.qticket.app.booking.ConfirmBookingCommand;
import kr.noco.qticket.app.booking.ConfirmBookingService;
import kr.noco.qticket.app.hold.SeatHoldService;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** REST snapshot 실제 HTTP JSON 검증 (T47-29, worker2 소유). */
class SeatSnapshotRestTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String HOLDER = "rest-member-1";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void givenHoldAndSold_whenSnapshotFetched_thenHttpJsonReflectsSource() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            Fixture fixture = insertFixture(nodes);
            try {
                admitAndHold(nodes, fixture);
                assertEntry(getSnapshot(nodes, fixture), fixture, "AVAILABLE", true);
                sellOnA(nodes, fixture);
                assertEntry(getSnapshot(nodes, fixture), fixture, "SOLD", false);
            } finally {
                deleteFixture(nodes, fixture);
            }
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private String getSnapshot(Nodes nodes, Fixture fixture) throws Exception {
        String url = "http://localhost:" + nodes.nodeB().port() + "/api/performances/" + fixture.performanceId() + "/seats/snapshot";
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(BOUND).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private void assertEntry(String body, Fixture fixture, String status, boolean held) {
        JsonNode snapshot = objectMapper.readTree(body);
        assertThat(snapshot.get("performanceId").asLong()).isEqualTo(fixture.performanceId());
        assertThat(findSold(snapshot.get("seats"), fixture, status, held)).isTrue();
    }

    private boolean findSold(JsonNode seats, Fixture fixture, String status, boolean held) {
        for (JsonNode entry : seats) {
            if (entry.get("seatId").asLong() == fixture.seatId() && status.equals(entry.get("saleStatus").asText()) && entry.get("held").asBoolean() == held && (!held || entry.get("holdTtlMillis").asLong() > 0L) && (held || entry.get("holdTtlMillis").asLong() == 0L)) {
                return true;
            }
        }
        return false;
    }

    private void admitAndHold(Nodes nodes, Fixture fixture) {
        nodes.nodeA().bean(QueueAdmissionService.class).admit(fixture.performanceId(), HOLDER);
        boolean acquired = nodes.nodeA().bean(SeatHoldService.class).hold(fixture.performanceId(), fixture.seatId(), HOLDER);
        assertThat(acquired).isTrue();
    }

    private void sellOnA(Nodes nodes, Fixture fixture) {
        nodes.nodeA().bean(ConfirmBookingService.class).confirm(new ConfirmBookingCommand(fixture.performanceId(), fixture.seatId(), "rest-key-1", HOLDER));
    }

    private Fixture insertFixture(Nodes nodes) {
        JdbcTemplate jdbc = nodes.nodeA().bean(JdbcTemplate.class);
        Long pid = jdbc.queryForObject("insert into performance (title, starts_at) values (?, now()) returning id", Long.class, "two-node rest");
        Long sid = jdbc.queryForObject("insert into seat (performance_id, section, row_label, seat_number, sale_status) values (?, 'R', '1', 1, 'AVAILABLE') returning id", Long.class, pid);
        return new Fixture(pid, sid);
    }

    private void deleteFixture(Nodes nodes, Fixture fixture) {
        JdbcTemplate jdbc = nodes.nodeA().bean(JdbcTemplate.class);
        jdbc.update("delete from ticket where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from booking where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from seat where performance_id = ?", fixture.performanceId());
        jdbc.update("delete from performance where id = ?", fixture.performanceId());
    }

    private record Fixture(Long performanceId, Long seatId) {
    }
}
