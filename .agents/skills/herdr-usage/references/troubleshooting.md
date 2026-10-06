# 트러블슈팅

형식: 증상 → 원인 → 해결

- 에이전트가 감지되지 않거나 상태가 틀림 → 화면 감지·integration 문제
  → `herdr agent list`, `herdr agent explain <target> --json`, `herdr integration status`.
  integration이 화면 감지를 대체한다고 가정하지 않는다 (상태, 세션 복원, 또는 둘 다 제공)
- 키바인딩이 반응하지 않음 → 외부 터미널이나 OS가 해당 키 조합을 먼저 가로챔
  → https://herdr.dev/docs/keyboard/ 에서 안전한 조합을 고르거나 터미널 설정에서 해제
- 시작 실패, socket API 이상 → 로그와 런타임 상태 확인
  → 로그: `~/.config/herdr/` (named 세션은 `sessions/<name>/`), Windows는 `%APPDATA%\herdr\`.
  `herdr status`, `herdr status server`, `herdr status client`
- 새 기능 명령이 "method not found" → 업데이트 후 클라이언트와 서버 버전이 다름
  → `herdr status`로 버전과 protocol 확인. 사용자 동의 없이 서버를 중지하거나 업그레이드하지 않는다
- pane 안에서 `herdr` 실행이 막힘 → 중첩 실행 차단 (의도된 동작)
  → 이미 attach된 상태다. CLI 하위 명령만 사용한다
- 공식 스킬 문서의 옵션이 `--help`에 없음 → 문서와 설치 버전 사이의 차이
  → 설치된 바이너리의 `herdr <group>` 출력을 따른다
  (예: 0.9.3에서 `detection` source는 `agent read`에만 있고 `pane read`에는 없다)
- 설정 변경 후 이상 동작 → config.toml 오류
  → `herdr config check`로 검증한 뒤 `herdr server reload-config`
- CLI 종료 코드 → 1: 서버 에러 (stderr에 JSON), 2: 문법 에러

- 재시작 후 topology는 있는데 agent 이름이 없음 → session.json은 이름을 복원하지 않음 → ws-save 매니페스트로 dry-run 후 ws-restore --apply에서 저장 이름 재부여.
- tab 번호를 pane 번호로 바꿔 잘못 복구 → 두 ID의 번호는 독립적 → 목록·생성 응답으로 연결하고 문자열 치환 금지.
- 복구가 중복 라벨/schema 오류로 중단 → 연결이 모호하거나 저장 파일 불량 → 파일·현 구성을 확인하고 다시 dry-run, 첫 항목 임의 선택 금지.
- 복구 출력에 skip이 있음 → 저장 이름 없음/cwd·kind·이름 점유 충돌 → 임의 이름·재시작 없이 사람이 대상을 확인, 기존 pane을 닫지 않는다.
- 정확한 분할 비율이 안 돌아옴 → 매니페스트는 pane 바인딩만 저장 → session.json의 기존 topology를 재사용하거나 사람이 크기를 조정한다.
- 다른 도구의 파일이 schema 오류로 거부되거나 덮어써짐 → 매니페스트 schema가 다름 → 기본 `~/.config/herdr/workspace-layout.json`을 사용하고 다른 도구와 매니페스트 파일 공유 금지.
