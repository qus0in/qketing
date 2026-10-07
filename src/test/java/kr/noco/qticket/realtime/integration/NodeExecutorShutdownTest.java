package kr.noco.qticket.realtime.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import kr.noco.qticket.realtime.integration.TwoNodeFixture.Nodes;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** context 종료 후 양 노드 message/subscription 실행기 종료 검증 준비 (T47-46, worker2 소유). */
class NodeExecutorShutdownTest {
    @Test
    void givenContextsClosed_whenChecked_thenExecutorsAreShutdown() throws Exception {
        Nodes nodes = TwoNodeFixture.startPair();
        ThreadPoolTaskExecutor[] executors = {executor(nodes, true), executor(nodes, false),
                other(nodes, true), other(nodes, false)};
        TwoNodeFixture.stopPair(nodes);
        for (ThreadPoolTaskExecutor executor : executors) {
            assertThat(executor.getThreadPoolExecutor().isShutdown()).isTrue();
            assertThat(executor.getThreadPoolExecutor().awaitTermination(10L, TimeUnit.SECONDS)).isTrue();
        }
    }
    private ThreadPoolTaskExecutor executor(Nodes nodes, boolean message) {
        return bean(nodes, true, message);
    }
    private ThreadPoolTaskExecutor other(Nodes nodes, boolean message) {
        return bean(nodes, false, message);
    }
    private ThreadPoolTaskExecutor bean(Nodes nodes, boolean nodeA, boolean message) {
        String name = message ? "realtimeMessageExecutor" : "realtimeSubscriptionExecutor";
        ConfigurableApplicationContext context = null;
        if (nodeA) {
            context = nodes.nodeA().context();
        } else {
            context = nodes.nodeB().context();
        }
        return context.getBean(name, ThreadPoolTaskExecutor.class);
    }
}
