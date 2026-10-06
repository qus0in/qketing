package kr.noco.qticket.app.booking;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.*;
import kr.noco.qticket.infra.persistence.performance.*;
import kr.noco.qticket.infra.persistence.seat.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentBookingConfirmTest {
    private static final int WORKERS = 8;

    @Autowired private ConfirmBookingService confirmations;
    @Autowired private PerformanceJpaRepository performances;
    @Autowired private SeatJpaRepository seats;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Test
    void givenDifferentKeys_whenConfirmingSameSeatConcurrently_thenOnlyOneTicketIsSold() throws Exception {
        Fixture fixture = createFixture();
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        try {
            assertOutcomes(runWorkers(fixture, executor), fixture.seatId());
        } finally {
            stop(executor);
        }
        assertPersistedResult(fixture);
    }

    private List<Attempt> runWorkers(Fixture fixture, ExecutorService executor) {
        CyclicBarrier barrier = new CyclicBarrier(WORKERS);
        List<CompletableFuture<Attempt>> futures = IntStream.range(0, WORKERS)
                .mapToObj(ignored -> CompletableFuture.supplyAsync(
                        () -> confirmAtBarrier(fixture, barrier), executor)).toList();
        return futures.stream().map(future -> future.handle((result, failure) -> failure == null
                ? result : new Attempt(null, null, failure)).join()).toList();
    }

    private Fixture createFixture() {
        PerformanceEntity performance = performances.saveAndFlush(
                PerformanceEntity.of("Concurrent show", Instant.now()));
        SeatEntity seat = seats.saveAndFlush(SeatEntity.of(performance.getId(), "A", "A", 1));
        return new Fixture(performance.getId(), seat.getId());
    }

    private Attempt confirmAtBarrier(Fixture fixture, CyclicBarrier barrier) {
        try {
            barrier.await(10, SECONDS);
            ConfirmBookingCommand command = new ConfirmBookingCommand(fixture.performanceId(),
                    fixture.seatId(), UUID.randomUUID().toString());
            return new Attempt(confirmations.confirm(command), null, null);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return exception instanceof BusinessException business
                    ? new Attempt(null, business.getErrorCode(), null)
                    : new Attempt(null, null, exception);
        }
    }

    private void assertOutcomes(List<Attempt> attempts, Long seatId) {
        assertThat(attempts).hasSize(WORKERS).allSatisfy(attempt ->
                assertThat(attempt.failure()).isNull());
        assertThat(attempts).filteredOn(attempt -> attempt.error() == null).singleElement()
                .satisfies(winner -> assertThat(winner.result().seatId()).isEqualTo(seatId));
        assertThat(attempts).filteredOn(attempt -> attempt.error() == ErrorCode.CONFLICT)
                .hasSize(WORKERS - 1);
    }

    private void assertPersistedResult(Fixture fixture) {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ticket WHERE seat_id = ?",
                Long.class, fixture.seatId())).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM booking WHERE performance_id = ?",
                Long.class, fixture.performanceId())).isEqualTo(1L);
        assertThat(seats.findById(fixture.seatId()).orElseThrow().getSaleStatus())
                .isEqualTo(SeatEntity.SaleStatus.SOLD);
    }

    private void stop(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
    }
    private record Fixture(Long performanceId, Long seatId) {}

    private record Attempt(BookingResult result, ErrorCode error, Throwable failure) {}
}
