package kr.noco.qticket.ui.realtime.websocket;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * seat WebSocket 전송 한계. 개발 기본값이며 환경에서 재정의한다.
 * sendTimeLimit/bufferSizeLimit은 ConcurrentWebSocketSessionDecorator의 ms/bytes 한도다.
 * pendingLimit은 snapshot 조회 중 버퍼링할 최대 invalidation 수로, 초과 시 연결을 닫고 재연결을 유도한다.
 * sendHoldLimit은 첫 blocking send가 이 시간을 넘기면 watchdog이 연결을 강제 종료하는 자동 deadline이다.
 * resyncInterval은 무이벤트 구간에서도 authoritative snapshot을 다시 보내는 주기다.
 */
@ConfigurationProperties("qticket.realtime.seat-websocket")
public record SeatWebSocketProperties(
        @DefaultValue("5s") Duration sendTimeLimit,
        @DefaultValue("262144") int bufferSizeLimit,
        @DefaultValue("256") int pendingLimit,
        @DefaultValue("7s") Duration sendHoldLimit,
        @DefaultValue("15s") Duration resyncInterval) {

    private static final Duration DEFAULT_SEND_TIME_LIMIT = Duration.ofSeconds(5);
    private static final int DEFAULT_BUFFER_SIZE_LIMIT = 262144;
    private static final int DEFAULT_PENDING_LIMIT = 256;
    private static final Duration DEFAULT_SEND_HOLD_LIMIT = Duration.ofSeconds(7);
    private static final Duration DEFAULT_RESYNC_INTERVAL = Duration.ofSeconds(15);

    /** 3-arg 호환 생성자가 있어도 binder가 이 canonical 생성자를 쓰도록 명시한다(T47-53 부팅 결함). */
    @ConstructorBinding
    public SeatWebSocketProperties {
        if (sendTimeLimit == null || sendTimeLimit.isNegative() || sendTimeLimit.isZero()) {
            sendTimeLimit = DEFAULT_SEND_TIME_LIMIT;
        }
        if (bufferSizeLimit <= 0) {
            bufferSizeLimit = DEFAULT_BUFFER_SIZE_LIMIT;
        }
        if (pendingLimit <= 0) {
            pendingLimit = DEFAULT_PENDING_LIMIT;
        }
        if (sendHoldLimit == null || sendHoldLimit.isNegative() || sendHoldLimit.isZero()) {
            sendHoldLimit = DEFAULT_SEND_HOLD_LIMIT;
        }
        if (resyncInterval == null || resyncInterval.isNegative() || resyncInterval.isZero()) {
            resyncInterval = DEFAULT_RESYNC_INTERVAL;
        }
    }

    /** 기존 3-arg 호출 호환. 새 설정은 default를 쓴다. */
    public SeatWebSocketProperties(Duration sendTimeLimit, int bufferSizeLimit, int pendingLimit) {
        this(sendTimeLimit, bufferSizeLimit, pendingLimit, null, null);
    }

    public static SeatWebSocketProperties defaults() {
        return new SeatWebSocketProperties(DEFAULT_SEND_TIME_LIMIT, DEFAULT_BUFFER_SIZE_LIMIT,
                DEFAULT_PENDING_LIMIT, DEFAULT_SEND_HOLD_LIMIT, DEFAULT_RESYNC_INTERVAL);
    }
}
