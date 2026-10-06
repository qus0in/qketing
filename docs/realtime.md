# Realtime 착수 (0.5.0)

HEAD: #7 · work: #47 · 설계: #11/#16 · 릴리스 진행: #46

- 범위: WebSocket 좌석 동기화, SSE queue/streaming, 2-node Pub/Sub·reconnect snapshot.
- Spring MVC Servlet stack을 유지하고 Valkey Pub/Sub은 fan-out에만 쓴다.
- 좌석 snapshot은 PostgreSQL 판매 상태와 Valkey hold를 조회한다. reconnect에서 replay에 의존하지 않는다.
- SSE는 queue position/admission·heartbeat 및 streaming 전달 기반을 담당한다. 실제 AI 연동은 0.10.0이다.
- 튜닝 값은 원본 #17에서 blocker가 아닌 측정/확인 항목으로 정의한다. 운영값 확정은 실측 후 기록한다.

## 첫 작업 단위

- worker3: 이벤트 계약과 Valkey Pub/Sub 계층.
- worker4: SSE emitter 연결 관리 기반.
- worker2: 준비 조사 및 검증 기준 요약.
- worker1: 릴리스 노트 조사 중 /tmp 권한 UI로 blocked. 승인 대행 없이 Orchestrator가 노트를 작성했다.
- 기존 queue/hold/booking의 이벤트 발행, WebSocket endpoint, snapshot API와 2-node 통합 검증은 후속 단위로 연결한다.

## 회고

- v0.4.0은 승격 PR #48 머지 SHA `4880be2`의 main E2E success와 artifact 검토 후 tag를 발행했다.
- 현재 CI는 PR build와 수동 E2E만 제공한다. GHCR tag/image workflow와 배포 검증은 아직 없다.
- worker 화면 실측은 worker3 max, worker4 high였다. 사용자 표현(max 2)과 달라 실제 설정을 기록하고 변경하지 않았다.
- 읽기 전용 요약 작업에는 추가 빌드를 배정하지 않는다. 보고 경로는 저장소 tmp/agent-reports를 우선한다.
