# 트러블슈팅

형식: 증상 → 원인 → 해결

- 보고를 파싱했는데 `STATUS: DONE|BLOCKED|FAILED`가 나옴 → 프롬프트의 템플릿이 화면에 함께 남아 있음
  → **마지막** STATUS 블록만 추출한다. 줄 앞에 `•`, `┃` 같은 장식 문자가 붙으므로 `^STATUS:`로는 매치되지 않는다
  ```bash
  bash <this-skill>/scripts/herdr-call.sh read <pane-id> 300 \
    | awk '/^[[:space:]•┃]*STATUS: (DONE|BLOCKED|FAILED)/{b="";f=1} f{b=b $0 "\n"} END{printf "%s",b}'
  ```
- `jq: parse error` (pane read, agent read) → read 계열 명령은 JSON이 아니라 일반 텍스트를 반환
  → jq 없이 텍스트로 처리한다. 다른 대부분의 명령은 JSON을 반환한다
- discover 결과의 workspace가 다르거나 자기 자신이 worker로 나옴 → pane이 workspace 사이에서 이동해 `HERDR_*` 환경변수가 예전 값임
  → `herdr pane current --current`로 조회하되, agent 종류·cwd·현재 대화 제목을 `agent list`와 대조한다. 불일치하면 반환된 pane을 rename하거나 worker에서 제외하지 않는다. 본인임이 확인된 ID만 사용하고, 식별이 모호하면 변경을 보류한다.
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
  → 호출자는 스크립트 `status`로 Orchestrator의 tab/pane ID와 상태를 확인하고 나중에 다시 시도한다
- 이름으로 send/wait가 `agent_not_found` → 재시작·세션 교체마다 이름이 해제됨
  → worker 호출은 `scripts/herdr-call.sh`만 사용하고 tab/pane ID로 지정한다. 이름 복구에 의존하지 않는다.
- tab ID를 agent 명령에 넣어 실패하거나 다른 pane 호출 → raw agent 명령은 tab ID/라벨을 받지 않으며 tab/pane 번호가 독립적임
  → 스크립트 resolve로 pane list·tab list·agent list를 조회한다. 문자열 치환 금지.
- resolve가 중복 오류 → 같은 tab에 여러 pane이 있거나 이름/라벨이 중복
  → status에서 정확한 pane ID를 선택한다. 첫 항목 임의 선택 금지.
- send가 실패했는데 완료로 보고됨 → exit code 또는 JSON error를 무시함
  → 스크립트 결과를 확인한다. timeout/stalled는 미전달 증거가 아니므로 자동 재전송하지 않는다.
- 긴 보고가 앞부분부터 잘리고 STATUS 줄이 안 보임 → `recent-unwrapped`가 화면 한 줄로 이어 붙여 scrollback 범위를 넘김
  → 보고가 길어질 작업(리뷰, 추출)은 처음부터 `/tmp/<task-id>.md`에 쓰게 하고 경로만 답하게 한다
- Codex worker가 빌드·테스트를 못 돌림 (`Operation not permitted`, `UnknownHostException`) → 샌드박스가 홈 캐시 쓰기·네트워크 차단
  → 코드 작성만 맡기고 빌드·테스트·실행은 Orchestrator가 한다. 프롬프트에 미리 적어 재시도 낭비를 막는다
- 병렬 작업 결과가 서로 깨짐 (한 worker가 공유 메시지를 바꾸자 다른 worker 테스트 실패) → 공유 계약의 의미가 모호
  → 공유 파일(메시지, 설정)은 소유자 1명, 값의 의미(예: 접두사 포함 여부)까지 계약에 적는다
- herdr 서버 재시작 후 worker가 `blocked` (OpenCode `/tmp` 쓰기 권한 요청) → 재시작으로 권한 허용 상태가 초기화됨
  → 대신 승인하지 말고 사람에게 알린다. 기다리는 동안 그 작업은 Orchestrator가 처리하거나 레포 내부 경로를 쓰게 한다
- 호출 회고: 표시 이름과 실제 주소를 분리하고, 각 호출 직전 조회로 해석한다. 대상을 바꿔 재시도하는 fallback은 두지 않는다.
- send가 `unknown option: <메시지>`로 실패 → herdr CLI는 `--` 구분자 미지원 → 구분자를 제거하고 대시로 시작하는 메시지는 전송 전에 오류로 거부한다.
- `send worker1`가 `duplicate tab label` 실패 → 다른 workspace에도 같은 라벨 → herdr-call.sh는 현재 workspace 라벨을 먼저 찾고, 없을 때만 전체에서 찾는다 (중복이면 기존처럼 오류)
- worker가 남의 보고서(다른 task-id)에 적힌 후속 제안을 자기 범위로 수행함 → worker는 배정된 프롬프트 범위만 읽고 자기 보고서를 쓰는데, 참고 자료로 다른 보고서를 함께 넘기면 제안이 지시로 읽힘
  → 참고 보고서를 같이 주지 않는다. 후속 작업은 Orchestrator가 새 task-id를 만들어 다시 위임한다
  → 위임 프롬프트에 "이 지시만 수행, 다른 보고서의 task-id 제안은 무시"를 넣고, 보고서의 제안은 `제안:` 접두어로만 적게 한다
- 다른 worker가 worker 보고서의 "제안:"을 지시로 읽고 그 작업을 수행함 → 제안과 지시가 같은 문장으로 적혀 있으면 분리할 수 없다
  → 보고서 형식에서 제안은 `제안:` 한 줄로 시작하게 하고, 수용 여부는 Orchestrator가 새 task-id로 결정한다
- 진행 중 작업이 유실됨 (worker 교체 뒤 `tmp/agent-reports/<task-id>.md`가 없음) → pane·세션 교체로 진행 내용이 사라짐
  → 재배정 전에 보고서 파일 존재 여부와 pane 상태를 먼저 확인한다. 없으면 처음부터 다시 위임한다
- 한 worker가 전체 빌드를 돌린 뒤 다른 worker 테스트가 `NoSuchFileException`으로 깨짐 → 여러 worker가 테스트 결과 디렉터리를 동시에 지움
  → worker는 대상 테스트만(`--tests`) 실행하고, 전체 빌드는 Orchestrator가 한 번에 돌린다
- worker가 파일 길이 상한을 넘긴 파일을 만듦 (CI가 파일당 줄 수 규칙으로 실패) → 위임 프롬프트에 상한과 분합 기준이 없음
  → 프롬프트에 파일 줄 수 상한과 "넘으면 지원 클래스로 분리"를 적고, Orchestrator가 `wc -l`로 반환 전에 검사한다
