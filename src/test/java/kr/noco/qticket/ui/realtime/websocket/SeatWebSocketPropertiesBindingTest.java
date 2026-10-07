package kr.noco.qticket.ui.realtime.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** 실제 Spring property binder로 SeatWebSocketProperties 부팅 바인딩 회귀 검증 (T47-53). */
class SeatWebSocketPropertiesBindingTest {

    private static final String PREFIX = "qticket.realtime.seat-websocket.";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void givenNoProperties_whenBound_thenDefaultsApplied() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            SeatWebSocketProperties properties = context.getBean(SeatWebSocketProperties.class);
            assertThat(properties.sendTimeLimit()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.bufferSizeLimit()).isEqualTo(262144);
            assertThat(properties.pendingLimit()).isEqualTo(256);
            assertThat(properties.sendHoldLimit()).isEqualTo(Duration.ofSeconds(7));
            assertThat(properties.resyncInterval()).isEqualTo(Duration.ofSeconds(15));
        });
    }

    @Test
    void givenOverrides_whenBound_thenOverridesApplied() {
        runner.withPropertyValues(
                        PREFIX + "send-time-limit=2s",
                        PREFIX + "pending-limit=10",
                        PREFIX + "send-hold-limit=3s",
                        PREFIX + "resync-interval=20s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SeatWebSocketProperties properties =
                            context.getBean(SeatWebSocketProperties.class);
                    assertThat(properties.sendTimeLimit()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.pendingLimit()).isEqualTo(10);
                    assertThat(properties.sendHoldLimit()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.resyncInterval()).isEqualTo(Duration.ofSeconds(20));
                });
    }

    @Test
    void givenInvalidValues_whenBound_thenValidationDefaultsApplied() {
        runner.withPropertyValues(
                        PREFIX + "pending-limit=0",
                        PREFIX + "send-hold-limit=0s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SeatWebSocketProperties properties =
                            context.getBean(SeatWebSocketProperties.class);
                    assertThat(properties.pendingLimit()).isEqualTo(256);
                    assertThat(properties.sendHoldLimit()).isEqualTo(Duration.ofSeconds(7));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SeatWebSocketProperties.class)
    static class TestConfig {
    }
}
