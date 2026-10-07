package kr.noco.qticket.realtime.integration;

import java.util.ArrayList;
import java.util.List;
import kr.noco.qticket.QticketApplication;
import kr.noco.qticket.realtime.support.SharedRealtimeInfrastructure;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** 2-node 독립 앱 컨텍스트起動. 인프라는 worker1 공유 support에 위임 (T47-21, worker2 소유). */
final class TwoNodeFixture {

    private static SharedRealtimeInfrastructure infra;

    private TwoNodeFixture() {
    }

    record Node(ConfigurableApplicationContext context, int port) {

        <T> T bean(Class<T> type) {
            return context.getBean(type);
        }
    }

    record Nodes(Node nodeA, Node nodeB) {
    }

    static synchronized Nodes startPair(String... extraArgs) {
        infra = SharedRealtimeInfrastructure.start();
        try {
            return pairedNodes(extraArgs);
        } catch (RuntimeException exception) {
            closeInfra();
            throw exception;
        }
    }

    static Nodes startPair() {
        return startPair(new String[0]);
    }

    private static Nodes pairedNodes(String... extraArgs) {
        Node nodeA = startNode(extraArgs);
        try {
            Nodes nodes = new Nodes(nodeA, startNode(extraArgs));
            System.out.println("two-node ports nodeA=" + nodeA.port()
                    + " nodeB=" + nodes.nodeB().port());
            return nodes;
        } catch (RuntimeException exception) {
            nodeA.context().close();
            throw exception;
        }
    }

    static synchronized void stopPair(Nodes nodes) {
        try {
            nodes.nodeB().context().close();
        } finally {
            try {
                nodes.nodeA().context().close();
            } finally {
                closeInfra();
            }
        }
    }

    private static Node startNode(String... extraArgs) {
        List<String> args = new ArrayList<>();
        args.add("--server.port=0");
        args.add("--qticket.realtime.queue-stream.heartbeat-interval=1s");
        args.add("--qticket.realtime.queue-stream.refresh-interval=120s");
        for (String extra : extraArgs) {
            args.add(extra);
        }
        infra.properties().forEach((key, value) -> args.add("--" + key + "=" + value));
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                QticketApplication.class).run(args.toArray(new String[0]));
        try {
            int port = Integer.parseInt(
                    context.getEnvironment().getProperty("local.server.port"));
            return new Node(context, port);
        } catch (RuntimeException exception) {
            context.close();
            throw exception;
        }
    }

    private static void closeInfra() {
        if (infra != null) {
            try {
                infra.close();
            } catch (RuntimeException ignored) {
                // already closed by shutdown hook
            }
            infra = null;
        }
    }
}
