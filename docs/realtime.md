# Realtime 착수 (0.5.0)

HEAD: #7 · work: #47 · 설계: #11/#16 · 릴리스 진행: #46

- 범위: WebSocket 좌석 동기화, SSE queue/streaming, 2-node Pub/Sub·reconnect snapshot.
- Spring MVC Servlet stack을 유지하고 Valkey Pub/Sub은 fan-out에만 쓴다.
- 좌석 snapshot은 PostgreSQL 판매 상태와 Valkey hold를 조회한다. reconnect에서 replay에 의존하지 않는다.
- SSE는 queue position/admission·heartbeat 및 streaming 전달 기반을 담당한다. 실제 AI 연동은 0.10.0이다.
- 튜닝 값은 원본 #17에서 blocker가 아닌 측정/확인 항목으로 정의한다. 운영값 확정은 실측 후 기록한다.

## 첫 작업 단위

- worker3: T47-4 이벤트 계약과 Valkey Pub/Sub 계층 코드·단위 테스트 작성 완료. T47-7 실행 검증·SSE 교차 리뷰 배정.
- worker4: T47-5 SSE emitter 연결 관리 기반 코드·단위 테스트 작성 완료. T47-8 Pub/Sub 교차 리뷰 배정.
- worker2: 준비 조사 및 검증 기준 요약.
- worker1: 릴리스 노트 조사 중 /tmp 권한 UI로 blocked. 승인 대행 없이 Orchestrator가 노트를 작성했다.
- worker1의 남은 노트 확인은 worker2 T47-6으로 재배정. 반복 읽기·검증·빌드는 worker 담당이며 결과 검토 전 기능 완료로 처리하지 않는다.
- 기존 queue/hold/booking의 이벤트 발행, WebSocket endpoint, snapshot API와 2-node 통합 검증은 후속 단위로 연결한다.

## 진척 집계 기준

- 7개 작업 단위: 이벤트/PubSub 기반, SSE 연결 기반, producer 연동, WebSocket endpoint, snapshot API, SSE queue/stream endpoint, 2-node/reconnect 검증.
- 코드 초안 2/7(약 29%). 아직 실행·교차 리뷰 검증 전이며 milestone 완료율과 구분한다.
- 최종 리뷰: 현재 snapshot 전송 후 register 방식에는 등록 직전 live event 누락 위험이 있다. snapshot 공급자·이벤트 버퍼 계약을 후속 검토한 뒤 endpoint를 연결한다.

## 다음 회차 (2026-10-07)

| 탭 | 작업 | 소유 범위 |
| - | - | - |
| worker1 | T47-10 authoritative snapshot 원본 조회 | app/realtime/snapshot·신규 PG/hold read adapter |
| worker3 | T47-11 seat producer·WebSocket endpoint | hold/booking·WS·starter 의존성 |
| worker4 | T47-12 컴파일/경합 수정·snapshot API·queue SSE endpoint | SSE·REST·queue snapshot read |
| worker2 | T47-13 2-node 계획/테스트 준비·단독 실행 검증 | 신규 통합 테스트·보고서 |

- T47-7은 compileJava 성공·compileTestJava IOException 누락으로 실패해 테스트가 실행되지 않았다. 교차 리뷰 결과에 따라 worker4가 먼저 수정한다.
- 이번 배분으로 WebSocket·snapshot API는 구현 착수, 2-node는 계획/테스트 준비 착수. 아직 통과 결과가 없어 초안 집계 2/7을 유지한다.
- snapshot은 PG 판매 상태와 Valkey hold TTL을 병합하며 SOLD 우선·actor 식별자 비노출. 공유 서비스 API와 소유자를 프롬프트에서 동결했다.
- 다른 workspace의 worker2 이름이 로컬 탭 라벨을 가리는 라우팅 오류를 발견했다. 잘못 전달된 지시는 철회하고 라벨 우선으로 수정 후 resolve=wW:p5를 확인해 재배정했다.

## 실행 검증 (2026-10-07 09:42 KST)

- T47-14는 기본 Java 탐색만으로 JDK를 못 찾아 래퍼 실행 전 실패했다. docs/development.md의 기존 Temurin 경로를 적용한 T47-18은 `./gradlew build --no-daemon` 성공(exit 0, 29초).
- 이번 실행 XML 41개를 Orchestrator가 로그·수정시각과 대조: tests 121, failures/errors/skipped 모두 0. 컴파일·JAR 생성·전체 test 통과이며 E2E/2-node는 포함하지 않는다.
- 코드 구현 집계는 5/7(약71%): Pub/Sub 기반·SSE 기반·WebSocket·snapshot·queue SSE. producer는 seat hold/afterCommit만 연결되고 queue atomic publish가 남아 있으며, 2-node 통합 검증은 계획만 작성된 상태다.
- 실제 WebSocket handshake·REST snapshot 요청 및 2-node fan-out/reconnect 테스트는 미실행이다. 단위 테스트 통과를 이 통합 완료 기준으로 대신하지 않는다.
- 후속: WS 느린 클라이언트/close 경합, SSE snapshot 실패 처리·버퍼 상한·이벤트 신선도, 실 Valkey snapshot adapter 테스트를 점검하고 2-node 실제 검증을 추가한다.
- 회고: 준비 완료를 검증 성공으로 간주해 후속 실행을 놓치지 않도록 SKILL/protocol/routing에 실행자 인계·명령/exit code/이번 산출물 확인 규칙을 추가했다. 커밋하지 않는다.

## 두 노드 실제 실행 (2026-10-07 10:03 KST)

- T47-20/21/22/23으로 fixture·실제 클라이언트·queue/WS·SSE를 분리했다. 초기 컴파일 오류(TC2 비제네릭 타입·제거된 heartbeat 호출)는 소유 worker가 수정한 뒤 새 실행 단위 T47-24에서 재검증했다.
- `./gradlew test --tests 'kr.noco.qticket.realtime.integration.*' --no-daemon`: 3/3 통과, failures/errors/skipped 0. 독립 두 앱이 PG/Valkey 한 세트를 공유한다.
- 실행 XML의 포트 쌍: fan-out 65316/65322, SSE 65336/65344, reconnect 65370/65379. 실제 TCP WebSocket·HTTP/SSE 연결이며 mock 노드로 대체하지 않았다.
- 검증: node A hold→node B SEAT_CHANGED; disconnect 중 node A 판매→node B reconnect SNAPSHOT의 SOLD/held=false/TTL=0; SSE 실제 data·subscriber 격리·재연결 snapshot.
- 후속 전체 `./gradlew build --no-daemon`: exit 0, 53 suites·139 tests, failures/errors/skipped 모두 0. 컴파일·JAR·전체회귀 통과, Orchestrator가 이번 XML/로그 직접 대조.
- 증거: `tmp/agent-reports/T47-24.md`, `T47-24-integration.log`, `T47-24-build.log`, `T47-24-integration-results/`(별도 보존 XML3개).
- 작업 단위 기준 진척 5/7(71%)→7/7(100%). E2E·최종 부하/운영 점검 및 릴리스 검증을 포함한 완료율은 아니다. #47은 후속 점검 추적을 위해 유지한다.
- 남은 점검: Pub/Sub 실행기 bounded thread pool, WS 송신 지연 상한 실측, SSE wire JSON·신선도 테스트 강화, queue waiting TTL 의미 문서화. 기존 waiting score 기반 만료 청소도 있으므로 key PEXPIRE 갱신만으로 개인 TTL이 늘어난다고 해석하지 않는다.
- 회고는 docs에만 추가했다. 미커밋 SKILL.md/protocol.md/routing.md는 이번 회차에 수정하거나 커밋하지 않았다.

## 회차 결과와 진척 (2026-10-07 11:18 KST)

- 구현 진척 **7/7(100%) 유지**: 이벤트/PubSub 기반·SSE 연결 기반·producer 연동·WebSocket endpoint·snapshot API·SSE queue/stream endpoint·2-node/reconnect 검증까지 산출·검증 완료.
- **최종 실행 결과(T47-56)**: targeted **23 suites · 31 tests · 0 fail**(2-node integration **11 전부 pass**) · full build **72 suites · 169 tests · 0 fail** · e2e **5 tests · 0 fail** · TCP `CLIENT KILL` **killed=4 → 자동 재연결 새 JSON event 수신** · 403 10 round 30 opens 비200 0 → **원인 미확정 유지** · 11:18 홈/404 스크린샷. 이전 기준: T47-39 60 suites·153 tests·e2e 5.
- **E2E 통과**는 위 결과로 기록한다. **운영 부분점검**(WS 전송 한도 3겹·주기 복구는 수정 반영, WS 주기 heartbeat 부재, message drop 후 15s 자동 resync 복구와 연결 종료의 차이, subscription 거부(포화) 후 recovery·실부하 튜닝 미측정(자동 재구독은 검증 완료), SSE backpressure·403)은 `docs/realtime-operations.md` §6으로 **분리 기록**하며 구현 진척률에는 넣지 않는다.

## 회고

- v0.4.0은 승격 PR #48 머지 SHA `4880be2`의 main E2E success와 artifact 검토 후 tag를 발행했다.
- 현재 CI는 PR build와 수동 E2E만 제공한다. GHCR tag/image workflow와 배포 검증은 아직 없다.
- worker 화면 실측은 worker3 max, worker4 high였다. 사용자 표현(max 2)과 달라 실제 설정을 기록하고 변경하지 않았다.
- 읽기 전용 요약 작업에는 추가 빌드를 배정하지 않는다. 보고 경로는 저장소 tmp/agent-reports를 우선한다.
- 회고: record에 다중 생성자가 있으면 `@ConstructorBinding`을 **canonical 생성자**에 명시한다 — 3-arg 호환 생성자와의 binder 선택 모호성이 부팅 failure로 이어진다(T47-52, 11 fail → 수정 후 해소).
- 회고: Redis `CLIENT KILL`은 native **실 type을 javap로 확인한 뒤 typed API**를 쓴다 — raw `execute`는 StatusOutput 미지원, 추측 import/cast는 compile error·CCE(T47-54/56).
- 회고: 시도별 로그를 덮어쓰지 말고 분리한다(T47-56 attempt 로그 덮어쓰기 발생 → 이후부터 시도별 분리).
