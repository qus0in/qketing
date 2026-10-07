package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 초기 snapshot 후 타 노드 admission delta가 polling 없이 도착하는지 검증 (T47-29). */
class QueueDeltaRealtimeTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String MEMBER = "delta-member-b";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenAbsentStream_whenAdmittedOnOtherNode_thenDeltaArrivesWithoutPolling()
            throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
            assertDeltaFlow(nodes);
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private void assertDeltaFlow(Nodes nodes) throws Exception {
        Long pid = insertPerformance(nodes.nodeA());
        try {
            SseStream stream = SseStream.open(nodes.nodeB().port(), pid, MEMBER);
            try {
                assertQueuePayload(stream.awaitData(BOUND), pid, "ABSENT");
                nodes.nodeA().bean(QueueAdmissionService.class).admit(pid, MEMBER);
                String delta = stream.awaitDataContaining("ACTIVE", BOUND);
                assertQueuePayload(delta, pid, "ACTIVE");
                assertThat(objectMapper.readTree(delta.substring(5))
                        .get("sessionTtlMillis").asLong()).isGreaterThan(0L);
            } finally {
                stream.close();
            }
        } finally {
            deletePerformance(nodes.nodeA(), pid);
        }
    }

    private void assertQueuePayload(String line, Long pid, String status) {
        assertThat(line).isNotNull();
        JsonNode payload = objectMapper.readTree(line.substring(5));
        assertThat(payload.get("performanceId").asLong()).isEqualTo(pid);
        assertThat(payload.get("status").asText()).isEqualTo(status);
        assertThat(payload.has("sessionTtlMillis")).isTrue();
        assertThat(payload.toString()).doesNotContain(MEMBER);
    }

    private Long insertPerformance(Node nodeA) {
        return nodeA.bean(JdbcTemplate.class).queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, "two-node delta");
    }

    private void deletePerformance(Node nodeA, Long pid) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        jdbc.update("delete from seat where performance_id = ?", pid);
        jdbc.update("delete from performance where id = ?", pid);
    }
}
