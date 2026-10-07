package kr.noco.qticket.infra.redis.realtime;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * realtime Pub/Sub 실행기 (T47-30).
 * 기본 task executor는 SimpleAsyncTaskExecutor(unbounded)이고, 외부 executor를 주면 컨테이너가
 * destroy하지 않는다(바이트코드 확인). 그래서 @Bean으로 등록해 Spring이 종료를 보장하게 한다.
 * initialize()를 직접 부르지 않는다: afterPropertiesSet→initialize가 executor 필드를 재할당하므로
 * 수동 호출 시 스레드풀이 누수된다.
 * message 실행기는 포화 시 invalidation hint를 버리고 경고만 남긴다(snapshot fallback 대상).
 * subscription 실행기는 거부를 명시 오류(AbortPolicy)로 올린다 — 구독/recovery 작업 유실은
 * snapshot으로 복구되지 않기 때문이다.
 */
@Configuration(proxyBeanMethods = false)
public class ValkeyRealtimeConfiguration {

    static final int MESSAGE_CORE = 2;
    static final int MESSAGE_MAX = 8;
    static final int MESSAGE_QUEUE = 512;
    static final int SUBSCRIPTION_CORE = 1;
    static final int SUBSCRIPTION_MAX = 2;
    static final int SUBSCRIPTION_QUEUE = 16;
    private static final int SHUTDOWN_WAIT_SECONDS = 5;

    @Bean("realtimeMessageExecutor")
    ThreadPoolTaskExecutor realtimeMessageExecutor() {
        return boundedExecutor("realtime-msg-", MESSAGE_CORE, MESSAGE_MAX, MESSAGE_QUEUE,
                new RealtimeRejectedExecutionHandler());
    }

    @Bean("realtimeSubscriptionExecutor")
    ThreadPoolTaskExecutor realtimeSubscriptionExecutor() {
        return boundedExecutor("realtime-sub-", SUBSCRIPTION_CORE, SUBSCRIPTION_MAX,
                SUBSCRIPTION_QUEUE, new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    RedisMessageListenerContainer seatRealtimeListenerContainer(
            RedisConnectionFactory connectionFactory, ValkeySeatRealtimeListener listener,
            @Qualifier("realtimeMessageExecutor") Executor messageExecutor,
            @Qualifier("realtimeSubscriptionExecutor") Executor subscriptionExecutor) {
        return container(connectionFactory, listener, SeatRealtimeChannels.PATTERN,
                messageExecutor, subscriptionExecutor);
    }

    @Bean
    RedisMessageListenerContainer queueRealtimeListenerContainer(
            RedisConnectionFactory connectionFactory, QueueRealtimeListener listener,
            @Qualifier("realtimeMessageExecutor") Executor messageExecutor,
            @Qualifier("realtimeSubscriptionExecutor") Executor subscriptionExecutor) {
        return container(connectionFactory, listener, QueueRealtimeChannels.PATTERN,
                messageExecutor, subscriptionExecutor);
    }

    private RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
            MessageListener listener, String pattern, Executor messageExecutor,
            Executor subscriptionExecutor) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(messageExecutor);
        container.setSubscriptionExecutor(subscriptionExecutor);
        container.addMessageListener(listener, new PatternTopic(pattern));
        return container;
    }

    /** Bean·테스트 공용 생성기. initialize()는 호출하지 않는다(Spring afterPropertiesSet이 수행). */
    static ThreadPoolTaskExecutor boundedExecutor(String prefix, int core, int max, int queue,
            RejectedExecutionHandler rejectionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setRejectedExecutionHandler(rejectionHandler);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_WAIT_SECONDS);
        return executor;
    }
}
