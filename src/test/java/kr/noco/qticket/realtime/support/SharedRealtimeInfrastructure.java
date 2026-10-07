package kr.noco.qticket.realtime.support;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 2-node·adapter 통합 테스트가 공유하는 PG+Valkey 1세트. Spring bean이 아니고 start()로만 기동한다. */
public final class SharedRealtimeInfrastructure implements AutoCloseable {

    private static final DockerImageName POSTGRES =
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres");
    private static final DockerImageName VALKEY =
            DockerImageName.parse("valkey/valkey:8.1.10-alpine3.24");
    private static final int VALKEY_PORT = 6379;

    static volatile SharedRealtimeInfrastructure instance;
    private static boolean hookRegistered;

    private final PostgreSQLContainer postgres;
    private final GenericContainer<?> valkey;
    private final JdbcTemplate jdbc;
    private volatile boolean closed;

    private SharedRealtimeInfrastructure(PostgreSQLContainer postgres,
                                         GenericContainer<?> valkey) {
        this.postgres = postgres;
        this.valkey = valkey;
        this.jdbc = new JdbcTemplate(dataSource(postgres));
    }

    public static synchronized SharedRealtimeInfrastructure start() {
        if (instance == null || instance.closed) {
            PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES);
            postgres.start();
            GenericContainer<?> valkey = new GenericContainer<>(VALKEY);
            try {
                valkey.withExposedPorts(VALKEY_PORT);
                valkey.start();
            } catch (RuntimeException exception) {
                postgres.stop();
                throw exception;
            }
            instance = new SharedRealtimeInfrastructure(postgres, valkey);
            if (!hookRegistered) {
                hookRegistered = true;
                Runtime.getRuntime().addShutdownHook(
                        new Thread(InfraStopGuard::closeCurrent));
            }
        }
        return instance;
    }

    public Map<String, Object> properties() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", postgres.getJdbcUrl());
        props.put("spring.datasource.username", postgres.getUsername());
        props.put("spring.datasource.password", postgres.getPassword());
        props.put("spring.data.redis.host", valkey.getHost());
        props.put("spring.data.redis.port", valkey.getMappedPort(VALKEY_PORT));
        return props;
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            InfraStopGuard.stop(valkey, postgres);
        } finally {
            if (instance == this) {
                instance = null;
            }
        }
    }

    private static DriverManagerDataSource dataSource(PostgreSQLContainer postgres) {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("org.postgresql.Driver");
        source.setUrl(postgres.getJdbcUrl());
        source.setUsername(postgres.getUsername());
        source.setPassword(postgres.getPassword());
        return source;
    }
}
