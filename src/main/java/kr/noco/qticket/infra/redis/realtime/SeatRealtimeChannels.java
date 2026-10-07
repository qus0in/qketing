package kr.noco.qticket.infra.redis.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SeatRealtimeChannels {

    static final String PATTERN = "qticket:realtime:*";

    private static final String PREFIX = "qticket:realtime:{";
    private static final Pattern CHANNEL = Pattern.compile(
            "^qticket:realtime:\\{([1-9][0-9]*)}$");

    private SeatRealtimeChannels() {
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
