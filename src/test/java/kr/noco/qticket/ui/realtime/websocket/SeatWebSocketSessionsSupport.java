package kr.noco.qticket.ui.realtime.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** WS 테스트 공용 mock session. */
final class SeatWebSocketSessionsSupport {

    private SeatWebSocketSessionsSupport() {
    }

    static WebSocketSession mockSession(List<String> frames) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("ws-1");
        given(session.isOpen()).willReturn(true);
        try {
            doAnswer(invocation -> {
                frames.add(((TextMessage) invocation.getArgument(0)).getPayload());
                return null;
            }).when(session).sendMessage(any(TextMessage.class));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return session;
    }

    /** 첫 sendMessage를 latch로 붙잡는 mock session (T47-30 첫 blocking send 실측용). */
    static WebSocketSession blockingSession(CountDownLatch blocked, CountDownLatch release) {
        return countingBlockingSession(blocked, release, 1);
    }

    /** blockAt번째 sendMessage부터 latch로 붙잡는 mock session (그 전 send는 즉시 성공). */
    static WebSocketSession countingBlockingSession(CountDownLatch blocked, CountDownLatch release,
            int blockAt) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("ws-1");
        given(session.isOpen()).willReturn(true);
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        try {
            doAnswer(invocation -> {
                if (calls.incrementAndGet() >= blockAt) {
                    blocked.countDown();
                    release.await(5, java.util.concurrent.TimeUnit.SECONDS);
                }
                return null;
            }).when(session).sendMessage(any(TextMessage.class));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return session;
    }

    /**
     * close()가 blocked send의 latch를 풀어주는 mock session (T47-44 raw close 해제 실측).
     * 2번째 sendMessage부터 blocked/released latch로 붙잡고, close(status)가 released를 내려
     * 실제 socket close처럼 IOException으로 송신을 중단시킨다.
     */
    static WebSocketSession closeReleasingSession(CountDownLatch blocked, CountDownLatch released) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn("ws-1");
        given(session.isOpen()).willReturn(true);
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        try {
            doAnswer(invocation -> {
                if (calls.incrementAndGet() >= 2) {
                    blocked.countDown();
                    released.await(5, java.util.concurrent.TimeUnit.SECONDS);
                    throw new java.io.IOException("closed");
                }
                return null;
            }).when(session).sendMessage(any(TextMessage.class));
            doAnswer(invocation -> {
                released.countDown();
                return null;
            }).when(session).close(any(org.springframework.web.socket.CloseStatus.class));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return session;
    }
}
