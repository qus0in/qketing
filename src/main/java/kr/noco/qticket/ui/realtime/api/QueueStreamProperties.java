package kr.noco.qticket.ui.realtime.api;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** queue SSE 운영 tuning. 개발 기본값이며 환경에서 재정의한다. */
@ConfigurationProperties("qticket.realtime.queue-stream")
public record QueueStreamProperties(
        @DefaultValue("15s") Duration heartbeatInterval,
        @DefaultValue("3s") Duration refreshInterval,
        @DefaultValue("30m") Duration timeout) {
}
