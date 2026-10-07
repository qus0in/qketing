package kr.noco.qticket.ui.realtime.websocket;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeatWebSocketProperties.class)
public class SeatWebSocketPropertiesConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    SeatWebSocketWatchdog seatWebSocketWatchdog(SeatWebSocketSessions sessions,
                                                SeatWebSocketProperties properties) {
        return new SeatWebSocketWatchdog(sessions, properties);
    }
}
