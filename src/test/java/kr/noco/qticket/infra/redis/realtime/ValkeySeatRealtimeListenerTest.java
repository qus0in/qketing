package kr.noco.qticket.infra.redis.realtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import tools.jackson.databind.json.JsonMapper;

class ValkeySeatRealtimeListenerTest {

    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ValkeySeatRealtimeListener listener =
            new ValkeySeatRealtimeListener(mapper, events);

    @Test
    void givenMatchingChannel_whenMessageReceived_thenPublishesLocalEvent() {
        SeatRealtimeEvent event = SeatRealtimeEventTest.event();
        listener.onMessage(message(channel(7L), mapper.writeValueAsString(event)), null);

        verify(events).publishEvent(event);
    }

    @Test
    void givenPerformanceMismatch_whenMessageReceived_thenIgnored() {
        SeatRealtimeEvent event = SeatRealtimeEventTest.event();
        listener.onMessage(message(channel(8L), mapper.writeValueAsString(event)), null);

        verifyNoInteractions(events);
    }

    @Test
    void givenMalformedChannelOrPayload_whenMessageReceived_thenIgnored() {
        listener.onMessage(message("qticket:realtime:invalid", "{}"), null);
        listener.onMessage(message(channel(7L), "not-json"), null);

        verifyNoInteractions(events);
    }

    private Message message(String channel, String body) {
        Message message = mock(Message.class);
        given(message.getChannel()).willReturn(channel.getBytes(StandardCharsets.UTF_8));
        given(message.getBody()).willReturn(body.getBytes(StandardCharsets.UTF_8));
        return message;
    }

    private String channel(Long performanceId) {
        return SeatRealtimeChannels.forPerformance(performanceId);
    }
}
