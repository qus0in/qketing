package kr.noco.qticket.ui.realtime.websocket;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** watchdog 보조 실행기 생성·종료 (T47-44/T47-50). 감시 스레드와 분리된 bounded executor를 만든다. */
final class SeatWebSocketExecutors {

    private SeatWebSocketExecutors() {
    }

    static Executor bounded(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    /** 큐 소진 후 실행 중 작업이 끝날 때까지 최대 timeoutMillis 동안 기다린다. */
    static boolean awaitIdle(Executor executor, long timeoutMillis) {
        if (!(executor instanceof ThreadPoolTaskExecutor taskExecutor)) {
            return true;
        }
        ThreadPoolExecutor pool = taskExecutor.getThreadPoolExecutor();
        pool.shutdown();
        try {
            return pool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static void destroy(Executor executor) {
        if (executor instanceof ThreadPoolTaskExecutor taskExecutor) {
            taskExecutor.destroy();
        }
    }

    /** 두 실행기의 대기·종료를 한 번에 수행한다(종료 순서 계약 유지). */
    static void shutdownBounded(Executor first, Executor second, long waitMillis) {
        awaitIdle(first, waitMillis);
        awaitIdle(second, waitMillis);
        destroy(first);
        destroy(second);
    }

    static boolean isTerminated(Executor executor) {
        return executor instanceof ThreadPoolTaskExecutor taskExecutor
                && taskExecutor.getThreadPoolExecutor().isTerminated();
    }
}
