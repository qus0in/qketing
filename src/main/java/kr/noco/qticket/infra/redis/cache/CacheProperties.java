package kr.noco.qticket.infra.redis.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** performance read cache 설정 (#40). 추후 infra.redis.ValkeyProperties로 통합 검토. */
@ConfigurationProperties("qticket.cache")
public record CacheProperties(Duration performanceTtl) {}
