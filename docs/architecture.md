# 아키텍처 결정 (qketing / QTicket)

범용 코딩 규칙은 `.agents/skills/spring-boot-conventions` 스킬을 따른다.
여기에는 이 레포에서 확정한 값만 요약한다. 원본은 HEAD #7과 plan #8~#16이며, 바뀌면 이슈가 우선한다.

## 스택 (2026-10-06 확정, #8 #16)

| 항목 | 값 |
| - | - |
| Java | 17 (개발/CI Temurin, 운영 Azul Zulu) |
| Spring Boot / Spring AI | 4.1.1 / 2.0.1 |
| Build | Gradle 9.8.0 Groovy DSL, Wrapper + checksum 고정 |
| Web | Spring MVC only (WebFlux 금지), `spring-boot-starter-websocket`, SSE |
| View | Thymeleaf SSR + Bulma 1.0.4 self-host, HTMX 우선 |
| DB | PostgreSQL 17.11 + Flyway + JPA + OpenFeign QueryDSL 7.7 + pgvector |
| Cache/Queue | Valkey (cache, queue, hold, rate limit, Pub/Sub). source of truth 아님 |
| API docs | springdoc-openapi 3.1.1 |
| Test | JUnit Jupiter + AssertJ + Mockito, 필요 시 H2. Browser E2E 제외 |

## 패키지

- base package: `com.qus0in.qticket`
- 최상위 layer: `ui / app / domain / infra`
- feature: `auth, performance, queue, seat, booking, chat`
- infra: `persistence, redis, ai, storage, security`
- 파일 100줄 이하(import 제외 가능), 메서드 가능한 16줄 이하

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

## 구현 직전 사용자 확인 필요

- AI model id, embedding model, RAG/memory 구조 (#13)
- queue capacity/TTL, Hikari/Tomcat/WebSocket/SSE tuning, OAuth redirect, AWS resource naming (#17)

## 변경 이력

- 2026-10-06: 최초 작성. #8 #16 #17과 #9~#15의 Bootstrap 관련 결정 요약 (#18)
