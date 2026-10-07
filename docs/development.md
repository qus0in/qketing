# 로컬 개발 (qketing)

## 준비

- JDK: Temurin 17. sudo 없이 `~/.jdks/jdk-17.0.20.1+1`에 설치 (Adoptium API tarball, sha256 검증)
  ```bash
  export JAVA_HOME=$(echo ~/.jdks/jdk-17*/Contents/Home)
  ```
- Gradle은 설치하지 않는다. `./gradlew` (9.8.0, `distributionSha256Sum` 고정)만 사용한다
- Docker: arm64 엔진. 0.2.0부터 테스트·E2E·로컬 실행이 **Testcontainers PostgreSQL**(`pgvector/pgvector:pg17`)을
  자동 기동하므로 Docker 데몬이 떠 있어야 한다 (#27)

## 명령

```bash
./gradlew build                                             # compile + test + bootJar
./gradlew bootTestRun --args='--server.address=127.0.0.1'   # local profile + PostgreSQL 컨테이너, http://127.0.0.1:8080
./gradlew processResources                                  # 실행 중 템플릿 변경 반영 (devtools 재시작)
docker build -t qketing:dev .                               # arm64 이미지
```

- `bootRun`은 이제 JPA가 `ddl-auto=validate`로 DB를 요구하므로 로컬에서 단독 실행하지 않는다.
  로컬 실행은 `bootTestRun`을 쓴다 (`TestQticketApplication`이 `TestcontainersConfiguration`을 함께 로드)
- `bootRun`/운영은 `SPRING_DATASOURCE_*` 등 env로 실제 PostgreSQL 접속 정보를 주입한다
- 기본 profile은 `local` (Thymeleaf cache off, devtools 포함). 운영은 `SPRING_PROFILES_ACTIVE=prod`
- actuator는 `health`만 노출한다

## 영속 (Flyway / JPA)

- schema는 Flyway만 관리한다. migration 위치는 `src/main/resources/db/migration`
- `ddl-auto=validate`이므로 entity를 바꾸면 schema 변경은 **새 `V<N>__*.sql` 파일로만** 추가한다.
  기존 V 파일 수정·삭제 금지, 엔티티와 어긋나면 앱이 기동에 실패한다 (#12)
- H2 결과로 PostgreSQL locking/pgvector/constraint를 검증하지 않는다 (#12)

## 동시성·멱등·rollback 테스트 (0.3.0)

- 위치: `src/test/java/kr/noco/qticket/app/booking/`
- 실행: `./gradlew test --tests 'kr.noco.qticket.app.booking.*'`
- 이 테스트들에는 `@Transactional`을 쓰지 않는다. 테스트 트랜잭션이 app service의
  실제 transaction 경계·rollback을 가리기 때문이다 (#33)
- 에이전트 보고 파일은 `tmp/agent-reports/`에 둔다 (`build/`는 clean으로 지워짐)

## E2E (Playwright)

Playwright **Java**를 Gradle `e2eTest` source set으로 실행한다. Node/npm은 쓰지 않는다.

```bash
./gradlew playwrightInstall   # 로컬 Chromium: ~/Library/Caches/ms-playwright (최초 1회)
./gradlew e2eTest             # @SpringBootTest random port + Testcontainers PostgreSQL
open build/reports/e2e        # 스크린샷 (실패 지점 포함), git 무관 경로
```

- `check`/`build`에는 포함하지 않는다. 실행 중인 `qketing-serve` 컨테이너(hermes 화면 체크용)와 무관하다
- 테스트·E2E 모두 Docker(Testcontainers)가 필요하다. Docker가 꺼져 있으면 컨테이너 기동 단계에서 실패한다
- CI: `ci.yml` build는 runner Docker로, `e2e.yml`(수동)은 `playwrightInstall -Pwith-deps`로
  OS 의존성과 함께 설치하고 artifact `e2e-report`를 남긴다

## 트러블슈팅

- `Unable to locate a Java Runtime` → `/usr/bin/java`·`java_home`만 보면 사용자 설치 JDK를 놓칠 수 있음 → 위의 `~/.jdks` Temurin 경로를 명령 단위 `JAVA_HOME`/`PATH`에 적용한다. 환경 실패로 생기지 않은 테스트 결과를 이전 XML로 대신 집계하지 않는다.
- `Port 8080 was already in use`인데 `lsof`에 안 보임 → Tailscale이 tailnet IP(100.x)의 8080을 사용 중
  → `--server.address=127.0.0.1`로 loopback에만 바인딩한다. `netstat -anv -p tcp | grep '\.8080 '`로 확인
- 템플릿을 고쳤는데 화면이 그대로 → bootRun은 `build/resources`를 읽음 → `./gradlew processResources`
- Codex worker는 샌드박스 때문에 Gradle을 못 돌린다 → 빌드·테스트·실행은 orchestrator가 한다
- `connect: can't assign requested address`, Gradle `Could not connect to the Gradle daemon` → macOS uptime 49.7일 초과 시
  TCP `TIME_WAIT`가 만료되지 않는 커널 버그로 ephemeral port 고갈 (`netstat -an -p tcp | grep -c TIME_WAIT` ≈ 16000)
  → 재부팅만 해결. `uptime` 확인, 장기 가동 머신은 주기적으로 재부팅 (2026-10-06, uptime 50일에서 발생)

## 변경 이력

- 2026-10-06: 동시성·멱등·rollback 테스트 위치와 @Transactional 금지 추가 (#33)
- 2026-10-06: 최초 작성 (0.1.0 Bootstrap, #22)
- 2026-10-06: E2E 실행 방법 추가 (#25)
- 2026-10-06: 0.2.0 영속 기반 — bootTestRun(Testcontainers), Flyway migration 규칙 추가 (#27)
