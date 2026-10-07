package kr.noco.qticket.infra.redis.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ValkeySeatRealtimePublisherTest {

    @Test
    void givenEvent_whenPublished_thenJsonUsesPerformanceChannel() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ObjectMapper mapper = JsonMapper.builder().build();
        SeatRealtimeEvent event = SeatRealtimeEventTest.event();

        new ValkeySeatRealtimePublisher(redis, mapper).publish(event);

        String channel = "qticket:realtime:{" + event.performanceId() + "}";
        verify(redis).convertAndSend(eq(channel), eq(mapper.writeValueAsString(event)));
        assertThat(channel).isEqualTo(SeatRealtimeChannels.forPerformance(7L));
    }

    @Test
    void givenNullEvent_whenPublished_thenRejected() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValkeySeatRealtimePublisher publisher = new ValkeySeatRealtimePublisher(
                redis, JsonMapper.builder().build());

        assertThatNullPointerException().isThrownBy(() -> publisher.publish(null));
        verifyNoInteractions(redis);
    }
}
