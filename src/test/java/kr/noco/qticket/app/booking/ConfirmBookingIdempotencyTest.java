package kr.noco.qticket.app.booking;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
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
class ConfirmBookingIdempotencyTest {
    private static final int WORKERS = 8;
    @Autowired private ConfirmBookingService confirmations;
    @Autowired private PerformanceJpaRepository performances;
    @Autowired private SeatJpaRepository seats;
    @Autowired private JdbcTemplate jdbc;
    @Test void givenSameKeyAndInput_whenRetriedSequentially_thenReturnsSameIds() {
        Fixture fixture = createFixture();
        String key = UUID.randomUUID().toString();
        BookingResult first = confirm(fixture, fixture.firstSeatId(), key);
        BookingResult second = confirm(fixture, fixture.firstSeatId(), key);
        assertThat(second).isEqualTo(first);
        assertCounts(fixture, 1L, 1L);
    }
    @Test void givenSameKeyAndInput_whenRetriedConcurrently_thenAllResultsMatch() throws Exception {
        Fixture fixture = createFixture();
        String key = UUID.randomUUID().toString();
        List<Attempt> attempts = executeConcurrently(fixture, key);
        assertThat(attempts).hasSize(WORKERS)
                .allMatch(attempt -> attempt.failure() == null && attempt.error() == null);
        assertThat(attempts).extracting(Attempt::result).containsOnly(attempts.get(0).result());
        assertCounts(fixture, 1L, 1L);
        assertThat(seats.findById(fixture.firstSeatId()).orElseThrow().getSaleStatus())
                .isEqualTo(SeatEntity.SaleStatus.SOLD);
    }
    @Test void givenSameKeyWithDifferentSeat_whenRetried_thenRejectsWithoutChangingBooking() {
        Fixture fixture = createFixture();
        String key = UUID.randomUUID().toString();
        BookingResult original = confirm(fixture, fixture.firstSeatId(), key);
        assertThatThrownBy(() -> confirm(fixture, fixture.secondSeatId(), key))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(confirm(fixture, fixture.firstSeatId(), key)).isEqualTo(original);
        assertCounts(fixture, 1L, 1L);
        assertThat(seats.findById(fixture.secondSeatId()).orElseThrow().getSaleStatus())
                .isEqualTo(SeatEntity.SaleStatus.AVAILABLE);
    }
    private List<Attempt> executeConcurrently(Fixture fixture, String key) {
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        try { return runWorkers(fixture, key, executor); }
        finally { stop(executor); }
    }
    private List<Attempt> runWorkers(Fixture fixture, String key, ExecutorService executor) {
        CyclicBarrier barrier = new CyclicBarrier(WORKERS);
        List<CompletableFuture<Attempt>> futures = IntStream.range(0, WORKERS)
                .mapToObj(ignored -> CompletableFuture.supplyAsync(
                        () -> confirmAtBarrier(fixture, key, barrier), executor)).toList();
        return futures.stream().map(future -> future.handle((result, failure) -> failure == null
                ? result : new Attempt(null, null, failure)).join()).toList();
    }
    private Attempt confirmAtBarrier(Fixture fixture, String key, CyclicBarrier barrier) {
        try {
            barrier.await(10, SECONDS);
            return new Attempt(confirm(fixture, fixture.firstSeatId(), key), null, null);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            return exception instanceof BusinessException business ? new Attempt(null, business.getErrorCode(), null) : new Attempt(null, null, exception);
        }
    }
    private BookingResult confirm(Fixture fixture, Long seatId, String key) { return confirmations.confirm(new ConfirmBookingCommand(fixture.performanceId(), seatId, key)); }
    private Fixture createFixture() {
        PerformanceEntity performance = performances.saveAndFlush(PerformanceEntity.of("Idempotency show", Instant.now()));
        SeatEntity first = seats.saveAndFlush(SeatEntity.of(performance.getId(), "A", "A", 1));
        SeatEntity second = seats.saveAndFlush(SeatEntity.of(performance.getId(), "A", "A", 2));
        return new Fixture(performance.getId(), first.getId(), second.getId());
    }
    private void assertCounts(Fixture fixture, Long bookings, Long tickets) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking WHERE performance_id = ?", Long.class, fixture.performanceId())).isEqualTo(bookings);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket WHERE performance_id = ?", Long.class, fixture.performanceId())).isEqualTo(tickets);
    }
    private void stop(ExecutorService executor) {
        executor.shutdownNow();
        try { assertThat(executor.awaitTermination(10, SECONDS)).isTrue(); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Executor shutdown interrupted", exception);
        }
    }
    private record Fixture(Long performanceId, Long firstSeatId, Long secondSeatId) {}
    private record Attempt(BookingResult result, ErrorCode error, Throwable failure) {}
}
