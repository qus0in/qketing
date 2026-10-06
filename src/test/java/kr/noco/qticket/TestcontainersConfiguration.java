package kr.noco.qticket;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 테스트·E2E·bootTestRun 공용 PostgreSQL. pgvector 확장이 있는 이미지를 쓴다 (#28 결정 1A). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName POSTGRES =
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(POSTGRES);
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> valkey() {
        GenericContainer<?> container = new GenericContainer<>(
                DockerImageName.parse("valkey/valkey:8.1.10-alpine3.24"));
        return container.withExposedPorts(6379);
    }
}
