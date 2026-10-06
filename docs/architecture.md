# 아키텍처 결정 (qketing / QTicket)

범용 코딩 규칙은 `.agents/skills/spring-boot-conventions` 스킬을 따른다.
여기에는 이 레포에서 확정한 값만 요약한다. 원본은 HEAD #7과 plan #8~#16이며, 바뀌면 이슈가 우선한다.

## 스택 (2026-10-06 확정, #8 #16)

| 항목 | 값 |
| - | - |
| Java | 17 (개발/CI Temurin, 운영 Azul Zulu) |
| Spring Boot / Spring AI | 4.1.1 / 2.0.1 |
| Build | Gradle 9.8.0 Groovy DSL, Wrapper + checksum 고정 |
| Web | Spring MVC only (WebFlux 금지), `spring-boot-starter-webmvc`(Boot 4 이름), SSE·WebSocket(0.5.0) |
| View | Thymeleaf SSR + Bulma 1.0.4 self-host, HTMX 우선 |
| DB | PostgreSQL 17.11 + Flyway + JPA + OpenFeign QueryDSL 7.7 + pgvector |
| Cache/Queue | Valkey (cache, queue, hold, rate limit, Pub/Sub). source of truth 아님 |
| API docs | springdoc-openapi 3.1.1 |
| Test | JUnit Jupiter + AssertJ + Mockito, 필요 시 H2 |
| E2E | Playwright Java + Chromium, Gradle `e2eTest`(self-contained). minor 승격 전 실행 (2026-10-06 변경, #25) |

## 패키지

- base package: `kr.noco.qticket` (group `kr.noco`, 2026-10-06 변경, #25)
- 최상위 layer: `ui / app / domain / infra`
- feature: `auth, performance, queue, seat, booking, chat`
- infra: `persistence, redis, ai, storage, security`
- 파일 100줄 이하(import 제외 가능), 메서드 가능한 16줄 이하

## 코딩 규칙 (사용자 지시, #25)

- `var` 금지 (Java·JS). JS는 ES6+ 우선, DOM 조회는 `querySelector` 계열만
- CSS: flex 위주(grid 허용), position은 sticky/fixed만, float 지양, 박스모델·gap은 8px 단위
- 상세는 `frontend-conventions`, `spring-boot-conventions` 스킬

## 0.1.0 Bootstrap에 걸리는 결정

- profile: `application.yml` + `local / test / prod`. Thymeleaf cache는 local/dev off, prod on (#9)
- 오류: custom RuntimeException + error code, API는 ProblemDetail, SSR은 403/404/500/`/error` view (#9 #16)
- 오류 화면도 `layouts/base.html`과 fragment를 재사용하고 내부 정보를 노출하지 않음 (#9)
- 설정값: `APP_BASE_URL` 등 환경 값은 env로 주입, secret을 image에 넣지 않음 (#14)
- 운영 JPA `ddl-auto=validate`, schema는 Flyway만 사용 (#12)
- Docker: ARM64-first multi-stage, arch 하드코딩 금지, stdout 로그, read-only filesystem 검토 (#14 #16)
- 메모리: app container 약 640 MiB, `-XX:MaxRAMPercentage` 약 60%부터 실측 (#14)
- CI: Ubuntu 24.04 runner, `setup-java@v6` + Temurin 17, Action은 commit SHA pin + Dependabot (#16)
- CI: PR은 compile/test/build, main은 linux/arm64 image → GHCR (#16)

## 오류 처리 구조 (0.1.0 구현)

- `app.error`: `ErrorCode`, `BusinessException`. `ui.error`: advice와 requestId
- advice 순서: `MvcInfrastructureErrorAdvice`(404/405, HTML/JSON 분기) > `ApiErrorAdvice`(`@RestController`) > `SsrErrorAdvice`
- API controller는 `ui.<feature>.api`에 둔다. requestId는 `X-Request-Id`(검증) 또는 UUID, 응답 헤더·MDC·ProblemDetail에 같은 값
- 미룸: Boot 기본 ProblemDetail handler의 입력 오류 경로(첫 입력 endpoint에서), ErrorCode의 HttpStatus 분리

## 구현 직전 사용자 확인 필요

- AI model id, embedding model, RAG/memory 구조 (#13)
- queue capacity/TTL, Hikari/Tomcat/WebSocket/SSE tuning, OAuth redirect, AWS resource naming (#17)

## 변경 이력

- 2026-10-06: 패키지 kr.noco, 코딩 규칙, Playwright Java E2E 도입 (#25)
- 2026-10-06: 오류 처리 구조 추가 (#22)
- 2026-10-06: 최초 작성. #8 #16 #17과 #9~#15의 Bootstrap 관련 결정 요약 (#18)
