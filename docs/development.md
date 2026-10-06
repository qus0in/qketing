# 로컬 개발 (qketing)

## 준비

- JDK: Temurin 17. sudo 없이 `~/.jdks/jdk-17.0.20.1+1`에 설치 (Adoptium API tarball, sha256 검증)
  ```bash
  export JAVA_HOME=$(echo ~/.jdks/jdk-17*/Contents/Home)
  ```
- Gradle은 설치하지 않는다. `./gradlew` (9.8.0, `distributionSha256Sum` 고정)만 사용한다
- Docker: arm64 엔진

## 명령

```bash
./gradlew build                                        # compile + test + bootJar
./gradlew bootRun --args='--server.address=127.0.0.1'  # local profile, http://127.0.0.1:8080
./gradlew processResources                             # 실행 중 템플릿 변경 반영 (devtools 재시작)
docker build -t qketing:dev .                          # arm64 이미지
```

- 기본 profile은 `local` (Thymeleaf cache off, devtools 포함). 운영은 `SPRING_PROFILES_ACTIVE=prod`
- actuator는 `health`만 노출한다

## E2E (Playwright)

Playwright **Java**를 Gradle `e2eTest` source set으로 실행한다. Node/npm은 쓰지 않는다.

```bash
./gradlew playwrightInstall   # 로컬 Chromium: ~/Library/Caches/ms-playwright (최초 1회)
./gradlew e2eTest             # @SpringBootTest random port로 앱을 직접 띄우고 종료
open build/reports/e2e        # 스크린샷 (실패 지점 포함), git 무관 경로
```

- `check`/`build`에는 포함하지 않는다. 실행 중인 `qketing-serve` 컨테이너(hermes 화면 체크용)와 무관하다
- CI: `e2e.yml`(수동)에서 `playwrightInstall -Pwith-deps`로 runner에 OS 의존성과 함께 설치, artifact `e2e-report`

## 트러블슈팅

- `Port 8080 was already in use`인데 `lsof`에 안 보임 → Tailscale이 tailnet IP(100.x)의 8080을 사용 중
  → `--server.address=127.0.0.1`로 loopback에만 바인딩한다. `netstat -anv -p tcp | grep '\.8080 '`로 확인
- 템플릿을 고쳤는데 화면이 그대로 → bootRun은 `build/resources`를 읽음 → `./gradlew processResources`
- Codex worker는 샌드박스 때문에 Gradle을 못 돌린다 → 빌드·테스트·실행은 orchestrator가 한다
- `connect: can't assign requested address`, Gradle `Could not connect to the Gradle daemon` → macOS uptime 49.7일 초과 시
  TCP `TIME_WAIT`가 만료되지 않는 커널 버그로 ephemeral port 고갈 (`netstat -an -p tcp | grep -c TIME_WAIT` ≈ 16000)
  → 재부팅만 해결. `uptime` 확인, 장기 가동 머신은 주기적으로 재부팅 (2026-10-06, uptime 50일에서 발생)

## 변경 이력

- 2026-10-06: 최초 작성 (0.1.0 Bootstrap, #22)
- 2026-10-06: E2E 실행 방법 추가 (#25)
