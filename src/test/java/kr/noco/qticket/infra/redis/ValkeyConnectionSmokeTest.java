package kr.noco.qticket.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kr.noco.qticket.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ValkeyConnectionSmokeTest {
    @Autowired private StringRedisTemplate redis;

    @Test
    void givenValkeyContainer_whenValueIsWrittenAndRead_thenConnectionWorks() {
        String key = "qticket:smoke:" + UUID.randomUUID();
        redis.opsForValue().set(key, "connected");
        assertThat(redis.opsForValue().get(key)).isEqualTo("connected");
        redis.delete(key);
    }
}
