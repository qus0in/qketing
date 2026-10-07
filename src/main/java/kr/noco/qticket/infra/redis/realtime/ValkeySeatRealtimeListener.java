package kr.noco.qticket.infra.redis.realtime;

import java.nio.charset.StandardCharsets;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ValkeySeatRealtimeListener implements MessageListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ValkeySeatRealtimeListener.class);

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    public ValkeySeatRealtimeListener(ObjectMapper objectMapper,
                                      ApplicationEventPublisher events) {
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] channelBytes = message.getChannel();
        if (channelBytes == null) {
            LOGGER.warn("Ignoring seat realtime message without channel");
            return;
        }
        String channel = text(channelBytes);
        Long channelPerformanceId = SeatRealtimeChannels.performanceId(channel);
        if (channelPerformanceId == null) {
            LOGGER.warn("Ignoring malformed seat realtime channel={}", channel);
            return;
        }
        SeatRealtimeEvent event = decode(message.getBody(), channel);
        if (event == null) {
            return;
        }
        if (!channelPerformanceId.equals(event.performanceId())) {
            LOGGER.warn("Ignoring mismatched seat realtime event channel={}", channel);
            return;
        }
        events.publishEvent(event);
    }

    private SeatRealtimeEvent decode(byte[] body, String channel) {
        if (body == null) {
            LOGGER.warn("Ignoring seat realtime message without payload channel={}", channel);
            return null;
        }
        try {
            SeatRealtimeEvent event = objectMapper.readValue(text(body), SeatRealtimeEvent.class);
            if (event == null) {
                LOGGER.warn("Ignoring empty seat realtime payload channel={}", channel);
            }
            return event;
        } catch (JacksonException | IllegalArgumentException | NullPointerException exception) {
            LOGGER.warn("Ignoring malformed seat realtime payload channel={}", channel, exception);
            return null;
        }
    }

    private String text(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
