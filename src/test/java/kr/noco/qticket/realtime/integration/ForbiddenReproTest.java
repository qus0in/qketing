package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** 403 재현 수집: 동일 localhost·ACTIVE/ABSENT·동시stream/재연결 반복, 증거 보존 (T47-43, worker2 소유). */
class ForbiddenReproTest {

    private static final Duration BOUND = Duration.ofSeconds(10);
    private static final int ROUNDS = 10;
    private static final String ACTIVE_MEMBER = "repro-member-active";
    private static final String ABSENT_MEMBER = "repro-member-absent";

    @Test
    void givenRepeatedStreams_whenOpened_thenCollectNon200Evidence() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        try {
            assertThat(nodes.nodeA().port()).isNotEqualTo(nodes.nodeB().port());
            List<String> failures = collectRounds(nodes);
            assertThat(failures).isEmpty();
        } finally {
            TwoNodeFixture.stopPair(nodes);
        }
    }

    private List<String> collectRounds(Nodes nodes) throws Exception {
        Long pid = insertPerformance(nodes);
        List<String> failures = new ArrayList<>();
        try {
            nodes.nodeA().bean(QueueAdmissionService.class).admit(pid, ACTIVE_MEMBER);
            for (int round = 0; round < ROUNDS && failures.size() < 3; round++) {
                collectRound(nodes, pid, failures);
            }
        } finally {
            deletePerformance(nodes, pid);
        }
        return failures;
    }

    private void collectRound(Nodes nodes, Long pid, List<String> failures) throws Exception {
        SseStream active = openQuietly(nodes, pid, ACTIVE_MEMBER, failures);
        SseStream absent = openQuietly(nodes, pid, ABSENT_MEMBER, failures);
        awaitQuietly(active, failures);
        awaitQuietly(absent, failures);
        closeQuietly(active);
        closeQuietly(absent);
        SseStream reopened = openQuietly(nodes, pid, ACTIVE_MEMBER, failures);
        awaitQuietly(reopened, failures);
        closeQuietly(reopened);
    }

    private SseStream openQuietly(Nodes nodes, Long pid, String member, List<String> errors) {
        try {
            return SseStream.open(nodes.nodeB().port(), pid, member);
        } catch (Exception exception) {
            errors.add(exception.getMessage());
            return null;
        }
    }

    private void awaitQuietly(SseStream stream, List<String> errors) {
        try {
            if (stream == null || stream.awaitData(BOUND) == null) {
                errors.add("no initial data");
            }
        } catch (Exception exception) {
            errors.add(exception.getMessage());
        }
    }

    private void closeQuietly(SseStream stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (Exception ignored) {
            // evidence already collected
        }
    }

    private Long insertPerformance(Nodes nodes) {
        return nodes.nodeA().bean(JdbcTemplate.class).queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, "two-node repro");
    }

    private void deletePerformance(Nodes nodes, Long pid) {
        JdbcTemplate jdbc = nodes.nodeA().bean(JdbcTemplate.class);
        jdbc.update("delete from seat where performance_id = ?", pid);
        jdbc.update("delete from performance where id = ?", pid);
    }
}
