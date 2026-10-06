package kr.noco.qticket;

import org.springframework.boot.SpringApplication;

/** 로컬 실행용: `./gradlew bootTestRun` 이 PostgreSQL 컨테이너와 함께 앱을 띄운다. */
public class TestQticketApplication {

    public static void main(String[] args) {
        SpringApplication.from(QticketApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
