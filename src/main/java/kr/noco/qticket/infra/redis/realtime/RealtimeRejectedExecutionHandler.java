package kr.noco.qticket.infra.redis.realtime;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * realtime dispatch 포화 처리 (T47-30).
 * 큐·스레드 상한을 넘긴 이벤트는 연결 thread를 막지 않도록 버리고 경고만 남긴다.
 * seat/queue 이벤트는 invalidation hint이므로 클라이언트는 snapshot 재조회로 복구한다.
 */
final class RealtimeRejectedExecutionHandler implements RejectedExecutionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RealtimeRejectedExecutionHandler.class);

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        LOGGER.warn("Realtime Pub/Sub dispatch saturated; dropped task queueSize={}",
                executor.getQueue().size());
    }
}
