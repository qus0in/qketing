package kr.noco.qticket.app.booking;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

/** booking confirm 동시성 테스트 공용 barrier 실행 헬퍼 (#37 low 정리). */
abstract class BookingConcurrencySupport extends BookingIntegrationSupport {

    protected static final int WORKERS = 8;

    protected List<Attempt> executeConcurrently(Fixture fixture, Supplier<String> keys) {
        return executeConcurrently(fixture, keys, keys);
    }

    protected List<Attempt> executeConcurrently(Fixture fixture, Supplier<String> keys,
            Supplier<String> holders) {
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        try {
            return runWorkers(fixture, keys, holders, executor);
        } finally {
            stop(executor);
        }
    }

    private List<Attempt> runWorkers(Fixture fixture, Supplier<String> keys,
            Supplier<String> holders, ExecutorService executor) {
        CyclicBarrier barrier = new CyclicBarrier(WORKERS);
        List<CompletableFuture<Attempt>> futures = IntStream.range(0, WORKERS)
                .mapToObj(ignored -> CompletableFuture.supplyAsync(
                        () -> confirmAtBarrier(fixture, keys.get(), holders.get(), barrier),
                        executor)).toList();
        return futures.stream().map(future -> future.handle((result, failure) -> failure == null
                ? result : new Attempt(null, null, failure)).join()).toList();
    }

    private Attempt confirmAtBarrier(Fixture fixture, String key, String holder,
            CyclicBarrier barrier) {
        try {
            barrier.await(10, SECONDS);
            return new Attempt(confirm(fixture, fixture.firstSeatId(), key, holder), null, null);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (exception instanceof BusinessException business) {
                return new Attempt(null, business.getErrorCode(), null);
            }
            return new Attempt(null, null, exception);
        }
    }

    private void stop(ExecutorService executor) {
        executor.shutdownNow();
        try {
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Executor shutdown interrupted", exception);
        }
    }

    protected record Attempt(BookingResult result, ErrorCode error, Throwable failure) {}
}
