package kr.noco.qticket.realtime.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 정리 시퀀스 담당: Valkey → PG 순서로 멈추되, Valkey stop이 예외여도 PG stop을 반드시 시도한다.
 * 첫 예외를 원본으로 보존해 던지고, PG 예외까지 나면 suppressed로 붙인다.
 * JVM 종료 시에는 "현재" 인프라를 닫는다(고정 인스턴스 바인딩 없음 → close 후 재start에도 최신 대상).
 */
final class InfraStopGuard {

    private InfraStopGuard() {
    }

    static void stop(GenericContainer<?> valkey, PostgreSQLContainer postgres) {
        RuntimeException failure = null;
        try {
            valkey.stop();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            postgres.stop();
        } catch (RuntimeException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    static void closeCurrent() {
        SharedRealtimeInfrastructure current = SharedRealtimeInfrastructure.instance;
        if (current != null) {
            current.close();
        }
    }
}
