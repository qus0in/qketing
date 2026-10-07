package kr.noco.qticket.infra.redis.realtime;

import java.util.Objects;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.SeatRealtimePublisherPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ValkeySeatRealtimePublisher implements SeatRealtimePublisherPort {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ValkeySeatRealtimePublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(SeatRealtimeEvent event) {
        Objects.requireNonNull(event);
        String channel = SeatRealtimeChannels.forPerformance(event.performanceId());
        redis.convertAndSend(channel, objectMapper.writeValueAsString(event));
    }
}
