# 트러블슈팅

형식: 증상 → 원인 → 해결

- 보고를 파싱했는데 `STATUS: DONE|BLOCKED|FAILED`가 나옴 → 프롬프트의 템플릿이 화면에 함께 남아 있음
  → **마지막** STATUS 블록만 추출한다. 줄 앞에 `•`, `┃` 같은 장식 문자가 붙으므로 `^STATUS:`로는 매치되지 않는다
  ```bash
  herdr agent read <w> --source recent-unwrapped --lines 300 \
    | awk '/^[[:space:]•┃]*STATUS: (DONE|BLOCKED|FAILED)/{b="";f=1} f{b=b $0 "\n"} END{printf "%s",b}'
  ```
- `jq: parse error` (pane read, agent read) → read 계열 명령은 JSON이 아니라 일반 텍스트를 반환
  → jq 없이 텍스트로 처리한다. 다른 대부분의 명령은 JSON을 반환한다
- discover 결과의 workspace가 다르거나 자기 자신이 worker로 나옴 → pane이 workspace 사이에서 이동해 `HERDR_*` 환경변수가 예전 값임
  → `herdr pane current --current`로 현재 pane과 workspace를 조회한다
- pane list의 `name`이 null → 에이전트 이름은 `agent list`에만 있음
  → `agent list`를 pane_id 기준으로 합친다
- Codex worker가 herdr, 네트워크, git 쓰기 작업에서 BLOCKED → Codex 샌드박스가 소켓이나 쓰기를 차단
  → 이런 작업은 Orchestrator가 직접 하거나, worker에게는 파일 읽기·수정만 맡긴다. 권한 상승은 사람이 결정한다
- 박스 문자를 지웠더니 한글이 깨짐 → `LC_ALL=C`에서 `[┃│]` 같은 대괄호 클래스가 바이트 단위로 매치됨
  → `(┃|│)` alternation을 쓰거나 UTF-8 locale을 지정한다
- zsh에서 `for x in "a b"; do set -- $x` 결과가 이상함 → zsh는 따옴표 없는 변수도 단어로 나누지 않음
  → 인자를 직접 쓰거나 `bash -c`로 실행한다
- 경량 모델 worker가 effort를 "알 수 없음"으로 답함 → 일부 CLI는 effort를 화면에 표시하지 않음
  → 화면에 표시가 없으면 effort는 기본값으로 가정하고, 결과 품질로 티어를 보정한다
- 외부 호출자가 `agent_blocked`를 받음 → Orchestrator가 승인 UI에서 사람을 기다리는 중
  → 호출자는 `herdr agent get orchestrator`로 상태를 확인하고 나중에 다시 시도한다
- `agent wait <name>`이 즉시 끝나고 worker는 아직 working → OpenCode 등은 새 프롬프트에서 새 session을 만들어 이름이 해제됨.
  `wait`은 `agent_not_found`로 바로 실패한다 → 프롬프트 직후 `agent list`로 이름을 확인하고 `agent rename <pane_id> <name>`으로 다시 붙인다.
  `wait` 결과는 버리지 말고 exit code를 확인한다
- 긴 보고가 앞부분부터 잘리고 STATUS 줄이 안 보임 → `recent-unwrapped`가 화면 한 줄로 이어 붙여 scrollback 범위를 넘김
  → 보고가 길어질 작업(리뷰, 추출)은 처음부터 `/tmp/<task-id>.md`에 쓰게 하고 경로만 답하게 한다
- Codex worker가 빌드·테스트를 못 돌림 (`Operation not permitted`, `UnknownHostException`) → 샌드박스가 홈 캐시 쓰기·네트워크 차단
  → 코드 작성만 맡기고 빌드·테스트·실행은 Orchestrator가 한다. 프롬프트에 미리 적어 재시도 낭비를 막는다
- 병렬 작업 결과가 서로 깨짐 (한 worker가 공유 메시지를 바꾸자 다른 worker 테스트 실패) → 공유 계약의 의미가 모호
  → 공유 파일(메시지, 설정)은 소유자 1명, 값의 의미(예: 접두사 포함 여부)까지 계약에 적는다
