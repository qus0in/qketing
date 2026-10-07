package kr.noco.qticket.realtime.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** 실제 network WebSocket 수신기. 조각 frame은 모아 완성 frame만 취급한다 (T47-21, worker2 소유). */
final class WsClient implements AutoCloseable {

    private static final long CONNECT_TIMEOUT_SECONDS = 10L;
    private final BlockingQueue<String> messages;
    private final StringBuilder fragments = new StringBuilder();
    private final WebSocket socket;

    private WsClient(WebSocket socket, BlockingQueue<String> messages) {
        this.socket = socket;
        this.messages = messages;
    }

    static WsClient connect(int port, Long performanceId) throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        StringBuilder fragments = new StringBuilder();
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
                    boolean last) {
                fragments.append(data);
                if (last) {
                    messages.add(fragments.toString());
                    fragments.setLength(0);
                }
                webSocket.request(1);
                return CompletableFuture.completedStage(null);
            }
        };
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/performances/"
                        + performanceId + "/seats"), listener)
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return new WsClient(socket, messages);
    }

    String awaitContaining(String text, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String message = messages.poll(200L, TimeUnit.MILLISECONDS);
            if (message != null && message.contains(text)) {
                return message;
            }
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "")
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
