package kr.noco.qticket.infra.redis.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** queue realtime event channel 계약 (T47-22). seat 공개 채널과 분리한다. */
final class QueueRealtimeChannels {

    static final String PATTERN = "qticket:queue-events:*";

    private static final String PREFIX = "qticket:queue-events:{";
    private static final Pattern CHANNEL = Pattern.compile(
            "^qticket:queue-events:\\{([1-9][0-9]*)}$");

    private QueueRealtimeChannels() {
    }

    static String forPerformance(Long performanceId) {
        if (performanceId == null || performanceId < 1) {
            throw new IllegalArgumentException("Performance id must be positive");
        }
        return PREFIX + performanceId + "}";
    }

    static Long performanceId(String channel) {
        Matcher matcher = CHANNEL.matcher(channel);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
