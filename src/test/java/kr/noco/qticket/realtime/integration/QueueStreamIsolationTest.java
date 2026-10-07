package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Node;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class QueueStreamIsolationTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final String MEMBER_A = "iso-member-a";
    private static final String MEMBER_B = "iso-member-b";

    @Test
    void givenTwoMembers_whenStreamed_thenIsolatedPerMember() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
            assertIsolationFlow(nodes.nodeA(), nodes.nodeB());
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private void assertIsolationFlow(Node nodeA, Node nodeB) throws Exception {
        Long pid = insertPerformance(nodeA);
        try {
            nodeA.bean(QueueAdmissionService.class).admit(pid, MEMBER_A);
            SseStream streamA = SseStream.open(nodeB.port(), pid, MEMBER_A);
            SseStream streamB = SseStream.open(nodeB.port(), pid, MEMBER_B);
            try {
                assertSnapshots(streamA, streamB);
                assertReconnectSnapshot(nodeB, pid);
            } finally {
                streamA.close();
                streamB.close();
            }
        } finally {
            deletePerformance(nodeA, pid);
        }
    }

    private void assertSnapshots(SseStream streamA, SseStream streamB) throws Exception {
        String dataA = streamA.awaitData(BOUND);
        String dataB = streamB.awaitData(BOUND);
        assertThat(dataA).contains("ACTIVE");
        assertThat(dataB).contains("ABSENT");
        assertThat(dataA).doesNotContain(MEMBER_B);
        assertThat(dataB).doesNotContain(MEMBER_A);
    }

    private void assertReconnectSnapshot(Node nodeB, Long pid) throws Exception {
        SseStream reopened = SseStream.open(nodeB.port(), pid, MEMBER_A);
        try {
            assertThat(reopened.awaitData(BOUND)).contains("ACTIVE");
        } finally {
            reopened.close();
        }
    }

    private Long insertPerformance(Node nodeA) {
        return nodeA.bean(JdbcTemplate.class).queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, "two-node sse");
    }

    private void deletePerformance(Node nodeA, Long pid) {
        JdbcTemplate jdbc = nodeA.bean(JdbcTemplate.class);
        jdbc.update("delete from seat where performance_id = ?", pid);
        jdbc.update("delete from performance where id = ?", pid);
    }
}
