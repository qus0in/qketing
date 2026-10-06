# SemVer 로드맵 (qketing)

이 레포 전용 로드맵이다. 계획의 원본은 HEAD #7이며, 이 문서는 그 내용을 요약·참조한다.
세부 설계는 영역별 plan issue에 기록하고 여기에 복제하지 않는다.

## 1. Milestone

| # | 버전 | 핵심 범위 |
|---|---|---|
| 1 | 0.1.0 — Bootstrap | build/runtime/profile/package skeleton, CI 기본 compile/test/build |
| 2 | 0.2.0 — Persistence Base | PostgreSQL/Flyway/JPA/QueryDSL, 핵심 schema와 repository smoke |
| 3 | 0.3.0 — Ticketing Domain | performance/seat/booking 기본 domain/app 흐름, transaction/idempotency 확정 |
| 4 | 0.4.0 — Valkey Core | cache/queue/active session/seat hold, Lua atomic admission, TTL/key namespace |
| 5 | 0.5.0 — Realtime | WebSocket 좌석 동기화, SSE queue/streaming, 2-node Pub/Sub·reconnect snapshot |
| 6 | 0.6.0 — Identity & Security | Google/Kakao OAuth, passwordless member, HMAC subject·JWT cookie·CSRF·logout/withdrawal |
| 7 | 0.7.0 — SSR/UI | Thymeleaf/Bulma layout, responsive/accessibility/40+ 시인성, metadata/privacy/terms UI |
| 8 | 0.8.0 — Local Distributed POC | app x2 + PostgreSQL + Valkey Docker 통합, concurrency/race/cache/reconnect/graceful shutdown 검증 |
| 9 | 0.9.0 — Observability | structured logs·correlation id, Micrometer/Prometheus/Loki/Grafana/Alertmanager, bounded labels·핵심 alerts |
| 10 | 0.10.0 — AI/RAG | 구현 직전 model/architecture 사용자 확인, Groq/GenAI 예정 연동, pgvector RAG + live SeatQuery + TTL memory |
| 11 | 0.11.0 — Runtime/Deployment | ARM64 Docker runtime, blue/green·readiness·drain·rollback, container memory/network/volume 정책 |
| 12 | 0.12.0 — AWS Release Candidate | CloudFront → ALB → EC2 A/B, RDS/Valkey Serverless/IAM/SSM/OIDC/GHCR, OAuth/WebSocket/SSE/blue-green smoke, 비용 경보·cleanup |
| 13 | 1.0.0 — First Stable | #8~#16의 1.0 필수 범위 완료, 동시성/무중단/보안/개인정보/접근성/관측성 기준 충족, 재현 가능한 배포·롤백·종료·비용 절감 문서화 |

Minor 번호는 0.9에서 멈추지 않는다. 필요하면 0.13.0, 0.14.0 등으로 확장한다.

## 2. Plan 이슈 매핑

- #8 — Core · runtime / architecture / conventions
- #9 — UI · SSR / accessibility / metadata
- #10 — Auth · OAuth / security / privacy
- #11 — Realtime · Valkey / queue / seat / SSE / WebSocket
- #12 — Persistence · PostgreSQL / Flyway / QueryDSL / pgvector
- #13 — AI · RAG / model integration
- #14 — Runtime · Docker / AWS / blue-green / cost
- #15 — Observability · metrics / logs / alerts
- #16 — Delivery · CI/CD / test / completion criteria

Milestone은 특정 layer에 귀속시키지 않고 HEAD #7에서 통합 관리한다.
각 milestone은 여러 plan issue를 횡단할 수 있으며, 하나의 plan이 여러 milestone에 걸칠 수 있다.

## 3. 버전 역할

| 역할 | 관리 위치 |
|---|---|
| 버전 로드맵 / milestone | HEAD #7 (요약은 이 문서) |
| 현재 개발 버전 | `gradle.properties` |
| 빌드 산출물 version | Gradle `project.version` |
| 실제 릴리스 확정 | Git tag `vX.Y.Z` |
| 배포 이미지 version | GHCR `:X.Y.Z` + commit SHA tag |
| 릴리스 기록 | GitHub Release |

- Gradle `version`은 `gradle.properties`에서 관리하고 build script에 중복 하드코딩하지 않는다.
- release workflow는 Git tag와 Gradle version 일치를 검증하고 불일치 시 실패시킨다.
- milestone은 계획된 통합 목표이며, Git tag는 검증이 끝난 릴리스 확정점이다.
- GitHub milestone이 존재한다고 자동으로 release된 것으로 간주하지 않는다.
- `0.x.0-SNAPSHOT` 개발 중에는 breaking change를 허용하고, `1.0.0`부터 최초 stable contract로 본다.

## 4. 운영 규칙

- 구현 `work` issue는 반드시 milestone을 지정한다.
- `work` issue는 HEAD #7과 관련 plan issue를 함께 reference한다.
- 세부 결정은 해당 plan issue에 기록하고 HEAD에는 세부 내용을 복제하지 않는다.
- 세부 issue는 항상 `HEAD: #7`을 유지한다.
- milestone 진행상태와 버전 승격 판단은 HEAD #7에서만 관리한다.
- 범위가 커져도 HEAD 본문을 다시 단일 대형 문서로 만들지 않는다.

## 5. minor 승격 체크리스트

1. milestone의 work 이슈 완료, `./gradlew build` 통과
2. E2E 통과: `gh workflow run e2e.yml --ref dev` 성공 + artifact `e2e-report` 스크린샷 검토 (로컬 `./gradlew e2eTest`)
3. `handle` 이슈로 사용자 승인 → version `X.Y.0` → tag `vX.Y.0` → GitHub Release → `X.(Y+1).0-SNAPSHOT`

## 6. 변경 이력

- 2026-10-06: milestone 13개(0.1.0~1.0.0) 생성, 로드맵 문서 신규 작성
- 2026-10-06: minor 승격 체크리스트에 Playwright E2E 추가 (#25)
