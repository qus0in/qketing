package kr.noco.qticket.infra.redis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("qticket.valkey")
public record ValkeyProperties(Queue queue, Session session, Hold hold) {

    public record Queue(int capacity) {}

    public record Session(Duration ttl) {}

    public record Hold(Duration ttl) {}
}
