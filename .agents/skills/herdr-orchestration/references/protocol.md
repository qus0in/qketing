# 프로토콜: 위임 · 보고 · 외부 호출

## 이름 규칙

- Orchestrator: `orchestrator`
- Workers: `<kind>-<n>` (예: `codex-1`, `opencode-2`). 모델 이름이 아니라 슬롯 이름을 쓴다 (모델은 바뀔 수 있다)
- 이름은 에이전트가 종료되면 해제된다. 세션을 시작할 때마다 `herdr agent list`로 확인하고 다시 붙인다.
  ```bash
  herdr agent rename <pane_id> <name>
  ```

## 위임 프롬프트 (Orchestrator → Worker)

```
[orchestrator→<worker>] <task-id> (난이도: 상|중|하)
목표: <한 문장>
맥락: <읽어야 할 파일·이슈 링크>
범위: <수정 가능한 파일 목록 | 읽기 전용>
제약: git commit/push 금지, 범위 밖 파일 수정 금지, 승인이 필요하면 하지 말고 BLOCKED로 보고, <레포 규칙>
완료 기준: <확인 가능한 조건>
보고 형식:
STATUS: DONE|BLOCKED|FAILED
SUMMARY: <3줄 이내>
FINDINGS: <항목별 한 줄, 없으면 none>
CHANGED: <수정한 파일 목록, 없으면 none>
```

- task-id는 `T<이슈번호>-<순번>` 형식으로 이슈와 연결한다.
- 프롬프트는 길어도 된다. 대신 하나의 목표만 담는다.

## 병렬 실행

```bash
herdr agent prompt codex-1 "<프롬프트>"      # --wait 없이 보내면 바로 반환된다
herdr agent prompt opencode-1 "<프롬프트>"
herdr agent wait codex-1 --timeout 600000     # 순서대로 기다려도 실행은 이미 병렬이다
herdr agent read codex-1 --source recent-unwrapped --lines 300   # 마지막 STATUS 블록만 파싱
```

- `agent read`는 JSON이 아니라 텍스트를 반환한다. 파싱 방법은 troubleshooting.md를 참고한다.
- 보고가 잘려서 보이면 `--lines`를 늘린다. 그래도 안 되면 `/tmp/<task-id>.md`에 쓰게 하고 경로만 받는다.

## 진입 경로

| 호출자 | 요청 방식 | 응답 방식 |
| - | - | - |
| 사람 (직접 채팅) | Orchestrator pane에 입력 | 채팅으로 답변. 필요하면 이슈 |
| 외부 에이전트 (hermes 등) | `herdr agent prompt orchestrator "<요청>" --wait` | 마지막 줄 `RESULT: ...`, 상세는 이슈 |

### 외부 호출 규약

다른 머신의 호출자는 저장된 프로필로 접근한다: `herdr --machine <label> agent prompt orchestrator "..." --wait`

요청 첫 줄에 출처와 회신 방식을 적는다.

```
[from:hermes reply:issue|inline] <요청 내용>
```

- Orchestrator는 마지막에 다음 형식 중 하나로 답한다. 호출자는 `agent read` 결과에서 마지막 `RESULT:` 줄을 파싱한다.
  ```
  RESULT: DONE <이슈 또는 PR URL | 한 줄 요약>
  RESULT: BLOCKED <사람 확인이 필요한 이유>
  RESULT: FAILED <원인>
  ```
- 외부 요청 작업은 `plan`이나 `work` 이슈에 `from:<호출자>`를 남겨 추적한다. 보고 요청이면 `report` 이슈를 쓴다.
- 외부 요청은 사람 요청과 같은 안전 규칙을 따른다. 되돌리기 어려운 작업(삭제, 외부 게시, 설정 변경)은
  `RESULT: BLOCKED`로 응답하고 사람의 확인을 기다린다.
- 설정값이나 외부 처리가 필요해서 막히면 `handle` 이슈를 만들고 `RESULT: BLOCKED <handle 이슈 URL>`로 응답한다.
- Orchestrator가 작업 중(`working`)일 때 들어온 프롬프트는 큐에 쌓인다. 호출자는 `agent get`으로 상태를 보고 보낸다.
  `blocked` 상태면 `agent_blocked`로 거부되므로 나중에 다시 시도한다.
