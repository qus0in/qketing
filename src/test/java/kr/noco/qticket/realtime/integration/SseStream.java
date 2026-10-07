package kr.noco.qticket.realtime.integration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** 실제 SSE wire 라인 수신기. 빈 data 스킵, bounded 큐, 제한시간 종료 (T47-21, worker2 소유). */
final class SseStream implements AutoCloseable {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final int QUEUE_CAPACITY = 128;
    private static final int BODY_LIMIT = 4096;
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicBoolean overflow = new AtomicBoolean(false);
    private final InputStream stream;
    private final Thread reader;
    private SseStream(InputStream stream) {
        this.stream = stream;
        this.reader = new Thread(this::drain);
        reader.setDaemon(true);
        reader.start();
    }
    static SseStream open(int port, Long performanceId, String memberId) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        String path = "/api/performances/" + performanceId + "/queue/stream?memberId=" + memberId;
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(CONNECT_TIMEOUT).GET().build();
        HttpResponse<InputStream> response = client.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String detail = describe(port, path, response);
            response.body().close();
            throw new IllegalStateException(detail);
        }
        return new SseStream(response.body());
    }
    private static String describe(int port, String path, HttpResponse<InputStream> response) {
        return "SSE status " + response.statusCode() + " port=" + port + " path=" + path
                + " content-type=" + response.headers().firstValue("Content-Type").orElse("-")
                + " server=" + response.headers().firstValue("Server").orElse("-")
                + " body=" + readLimited(response.body());
    }
    private static String readLimited(InputStream body) {
        try {
            return new String(body.readNBytes(BODY_LIMIT), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return "unreadable";
        }
    }
    private void drain() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                if (!lines.offer(line)) {
                    overflow.set(true);
                    return;
                }
                line = reader.readLine();
            }
        } catch (Exception ignored) {
            Thread.currentThread().interrupt();
        }
    }
    String awaitData(Duration timeout) throws InterruptedException {
        return awaitDataContaining("", timeout);
    }
    String awaitDataContaining(String text, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (overflow.get()) {
                throw new IllegalStateException("SSE queue overflow");
            }
            String line = lines.poll(200L, TimeUnit.MILLISECONDS);
            if (line != null && line.startsWith("data:") && line.length() > 5
                    && line.contains(text)) {
                return line;
            }
        }
        return null;
    }
    @Override
    public void close() throws Exception {
        stream.close();
        reader.join(5000L);
    }
}
