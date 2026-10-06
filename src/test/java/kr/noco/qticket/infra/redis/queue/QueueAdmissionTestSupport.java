package kr.noco.qticket.infra.redis.queue;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import kr.noco.qticket.app.queue.QueueAdmissionService;
import kr.noco.qticket.domain.queue.AdmissionStatus;

final class QueueAdmissionTestSupport {
    private QueueAdmissionTestSupport() {}

    static List<Attempt> run(QueueAdmissionService admissions, Long performanceId, int workers) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            return await(admissions, performanceId, workers, executor);
        } finally {
            stop(executor);
        }
    }

    private static List<Attempt> await(QueueAdmissionService admissions, Long performanceId,
                                       int workers, ExecutorService executor) {
        CyclicBarrier barrier = new CyclicBarrier(workers);
        List<CompletableFuture<Attempt>> futures = IntStream.range(0, workers)
                .mapToObj(index -> CompletableFuture.supplyAsync(
                        () -> admit(admissions, performanceId, barrier), executor)).toList();
        return futures.stream().map(future -> future.handle((attempt, failure) -> failure == null
                ? attempt : new Attempt(null, failure)).join()).toList();
    }

    private static Attempt admit(QueueAdmissionService admissions, Long performanceId,
                                 CyclicBarrier barrier) {
        try {
            barrier.await(10, SECONDS);
            return new Attempt(admissions.admit(performanceId, UUID.randomUUID().toString()), null);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Attempt(null, exception);
        }
    }

    private static void stop(ExecutorService executor) {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(10, SECONDS)) {
                throw new AssertionError("Queue admission workers did not stop");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Queue admission shutdown interrupted", exception);
        }
    }

    record Attempt(AdmissionStatus status, Throwable failure) {}
}
