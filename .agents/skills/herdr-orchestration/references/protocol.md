# 프로토콜: 위임 · 보고 · 외부 호출

## 호출 규칙

- worker 호출은 `bash <this-skill>/scripts/herdr-call.sh`로만 수행한다.
- 대상은 **tab ID 또는 pane ID**다. 에이전트 이름은 표시용이며 호출에 의존하지 않는다.
- 이름은 재시작·세션 교체마다 해제될 수 있다. `orchestrator`, `<kind>-<n>`은 역할 표시에만 쓴다.
- 스크립트는 pane ID → tab ID → 에이전트 이름 → tab 라벨 순서로 실제 JSON 조회를 한다.
- 이름·라벨 입력 지원은 진단/기존 호출 이관용이다. 새 호출에는 status에서 읽은 ID를 사용한다.
- 결과는 pane ID 하나여야 한다. 없는 대상·중복 이름/라벨·여러 pane의 tab은 오류로 종료한다.
- tab/pane 번호는 서로 독립적이다. 문자열 치환으로 ID를 만들지 않는다.
- ID는 해당 서버에서만 유효하다. pane 이동·삭제 후에는 status를 다시 조회한다.

## 위임 프롬프트 (Orchestrator → Worker)

```text
[orchestrator→<worker>] <task-id> (난이도: 상|중|하)
목표: <한 문장>
맥락: <읽어야 할 파일·이슈 링크>
범위: <수정 가능한 파일 목록 | 읽기 전용>
제약: git commit/push 금지, 범위 밖 수정 금지, 승인 필요 시 BLOCKED로 보고
완료 기준: <확인 가능한 조건>
보고 형식:
STATUS: DONE|BLOCKED|FAILED
SUMMARY: <3줄 이내>
FINDINGS: <항목별 한 줄, 없으면 none>
CHANGED: <수정한 파일 목록, 없으면 none>
```

- task-id는 `T<이슈번호>-<순번>`으로 이슈와 연결한다. 프롬프트에는 하나의 목표만 담는다.
- 메시지는 파일에 실제 줄바꿈으로 작성한다. `send <id> -`는 stdin을 읽는다.

## 병렬 실행

```bash
CALL=<this-skill>/scripts/herdr-call.sh
bash "$CALL" status                      # pane/tab ID·라벨·이름·상태 JSON lines
bash "$CALL" resolve '<worker-tab-id>'    # 조회로 연결된 pane ID 하나
bash "$CALL" send '<worker-tab-id>' /tmp/task-a.md
bash "$CALL" send '<other-pane-id>' /tmp/task-b.md
bash "$CALL" wait '<worker-tab-id>' 60000
bash "$CALL" read '<worker-tab-id>' 300    # 마지막 STATUS 블록만 파싱
```

- send는 대기 없이 전달한다. CLI exit code와 JSON error를 확인한 성공 응답만 반환한다.
- 전달 성공은 turn 시작·완료를 보장하지 않는다. wait/read로 진행과 마지막 보고를 확인한다.
- wait의 timeout은 양의 정수(ms), read 줄수는 양의 정수다. 실패 exit code를 버리지 않는다.
- read는 일반 텍스트다. working 중 history 읽기가 거부되면 status로 확인하고 완료 뒤 읽는다.
- timeout/stalled 후 자동 재전송하지 않는다. status/read로 먼저 실제 진행을 확인한다.
- 긴 결과는 호스트의 보고 파일 규칙을 따른다. 다시 읽어도 잘리면 임시 Markdown 파일로 받는다.

## 진입 경로

| 호출자 | 요청 방식 | 응답 방식 |
| - | - | - |
| 사람 | Orchestrator pane 채팅 | 채팅, 필요하면 이슈 |
| 외부 에이전트 | 스크립트 send로 Orchestrator의 tab/pane ID 지정 후 wait/read | 마지막 RESULT, 상세는 이슈 |

### 외부 호출 규약

원격 호출도 같은 스크립트를 해당 머신의 Herdr pane 환경에서 실행한다.
로컬 ID를 원격 ID로 재사용하지 않는다. 원격 전달 경로는 호스트의 연결 정책을 따른다.
이 스크립트는 machine 선택·원격 설치·세션 변경을 수행하지 않는다.

요청 첫 줄에 출처와 회신 방식을 적는다.

```text
[from:hermes reply:issue|inline] <요청 내용>
```

- Orchestrator는 마지막에 아래 결과를 남긴다. 호출자는 read 결과의 마지막 RESULT만 파싱한다.
  ```text
  RESULT: DONE <이슈 또는 PR URL | 요약>
  RESULT: BLOCKED <사람 확인이 필요한 이유>
  RESULT: FAILED <원인>
  ```
- 외부 작업은 plan/work 이슈에 출처를 기록한다. 보고 요청이면 report 이슈를 쓴다.
- 외부 호출도 같은 안전 규칙을 따른다. 되돌리기 어려운 작업은 사람 확인을 받는다.
- 설정값·외부 처리로 막히면 handle 이슈를 만들고 `RESULT: BLOCKED <handle 이슈 URL>`로 답한다.
- working 중 전달된 프롬프트는 큐에 들어갈 수 있다. status로 확인하고 실행을 추정하지 않는다.
- blocked는 승인·질문 대기다. 대신 승인하지 않고 사람에게 알린다.
