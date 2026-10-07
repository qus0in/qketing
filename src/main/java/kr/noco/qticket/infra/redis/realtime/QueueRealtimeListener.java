package kr.noco.qticket.infra.redis.realtime;

import java.nio.charset.StandardCharsets;
import kr.noco.qticket.app.realtime.QueueRealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** queue-events 채널을 decode해 worker4 bridge용 Spring event로 전달한다 (memberId는 bridge가 비공개 유지). */
@Component
public class QueueRealtimeListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueRealtimeListener.class);

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    public QueueRealtimeListener(ObjectMapper objectMapper, ApplicationEventPublisher events) {
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] channelBytes = message.getChannel();
        if (channelBytes == null) {
            LOGGER.warn("Ignoring queue realtime message without channel");
            return;
        }
        String channel = new String(channelBytes, StandardCharsets.UTF_8);
        Long channelPerformanceId = QueueRealtimeChannels.performanceId(channel);
        if (channelPerformanceId == null) {
            LOGGER.warn("Ignoring malformed queue realtime channel={}", channel);
            return;
        }
        QueueRealtimeEvent event = decode(message.getBody(), channel);
        if (event == null) {
            return;
        }
        if (!channelPerformanceId.equals(event.performanceId())) {
            LOGGER.warn("Ignoring mismatched queue realtime event channel={}", channel);
            return;
        }
        events.publishEvent(event);
    }

    private QueueRealtimeEvent decode(byte[] body, String channel) {
        if (body == null) {
            LOGGER.warn("Ignoring queue realtime message without payload channel={}", channel);
            return null;
        }
        try {
            QueueRealtimeEvent event = objectMapper.readValue(
                    new String(body, StandardCharsets.UTF_8), QueueRealtimeEvent.class);
            if (event == null) {
                LOGGER.warn("Ignoring empty queue realtime payload channel={}", channel);
            }
            return event;
        } catch (RuntimeException exception) {
            LOGGER.warn("Ignoring malformed queue realtime payload channel={}", channel, exception);
            return null;
        }
    }
}
