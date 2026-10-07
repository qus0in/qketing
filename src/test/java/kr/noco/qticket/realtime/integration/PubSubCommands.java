package kr.noco.qticket.realtime.integration;

import io.lettuce.core.KillArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisServerCommands;
import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

/** typed Lettuce CLIENT 명령 helper. raw execute의 StatusOutput 한계를 피한다 (T47-56, worker2 소유). */
final class PubSubCommands {

    private PubSubCommands() {
    }

    static long killPubsub(StringRedisTemplate redis) {
        Long killed = redis.execute(
                (RedisCallback<Long>) connection -> server(connection).clientKill(pubsubKill()));
        return killed == null ? -1L : killed;
    }

    static long pubsubCount(StringRedisTemplate redis) {
        Long lines = redis.execute((RedisCallback<Long>) connection -> {
            Object result = connection.execute("CLIENT", bytes("LIST"), bytes("TYPE"),
                    bytes("pubsub"));
            String text = result instanceof byte[] data ? new String(data, StandardCharsets.UTF_8)
                    : String.valueOf(result);
            return text.lines().filter(line -> !line.isBlank()).count();
        });
        return lines == null ? 0L : lines;
    }

    private static KillArgs pubsubKill() {
        return KillArgs.Builder.typePubsub();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RedisServerCommands<byte[], byte[]> server(RedisConnection connection) {
        Object nativeConnection = connection.getNativeConnection();
        if (nativeConnection instanceof StatefulRedisConnection stateful) {
            return ((StatefulRedisConnection<byte[], byte[]>) stateful).sync();
        }
        if (nativeConnection instanceof RedisAsyncCommands async) {
            StatefulRedisConnection<byte[], byte[]> stateful =
                    (StatefulRedisConnection<byte[], byte[]>) async.getStatefulConnection();
            return stateful.sync();
        }
        throw new IllegalStateException("Unsupported native connection " + nativeConnection);
    }
}
