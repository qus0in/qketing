package kr.noco.qticket.infra.persistence.seat;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** 같은 좌석 동시 판매 경합 실행 공용 harness (#40 T40-13). */
abstract class SeatSaleConcurrencyFixture {

    protected static final int THREADS = 8;
    private static final long TIMEOUT_SECONDS = 10L;

    @Autowired
    protected SeatPersistencePort seats;

    @Autowired
    protected TransactionTemplate transactions;

    protected List<Outcome> claimConcurrently(Long performanceId, Long seatId) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CyclicBarrier barrier = new CyclicBarrier(THREADS);
            return runWorkers(performanceId, seatId, barrier, executor);
        } finally {
            stop(executor);
        }
    }

    private List<Outcome> runWorkers(Long performanceId, Long seatId, CyclicBarrier barrier,
            ExecutorService executor) {
        List<CompletableFuture<Outcome>> futures = IntStream.range(0, THREADS)
                .mapToObj(ignored -> CompletableFuture.supplyAsync(
                        () -> claim(performanceId, seatId, barrier), executor))
                .toList();
        return futures.stream().map(future -> future.handle((outcome, failure) ->
                failure == null ? outcome : new Outcome(null, failure)).join()).toList();
    }

    private Outcome claim(Long performanceId, Long seatId, CyclicBarrier barrier) {
        try {
            barrier.await(TIMEOUT_SECONDS, SECONDS);
            return new Outcome(transactions.execute(
                    status -> seats.claimForSale(performanceId, seatId)), null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Outcome(null, exception);
        } catch (Exception exception) {
            return new Outcome(null, exception);
        }
    }

    private void stop(ExecutorService executor) {
        executor.shutdownNow();
        try {
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Executor shutdown interrupted", exception);
        }
    }

    protected record Outcome(Boolean claimed, Throwable failure) {
        boolean won() {
            return Boolean.TRUE.equals(claimed);
        }
        boolean lost() {
            return Boolean.FALSE.equals(claimed);
        }
    }
}