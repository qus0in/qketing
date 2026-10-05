# 에이전트 제어 레시피 (HERDR_ENV=1 전용)

요약본이다. 정확한 문법과 동작은 `herdr --skill`, `herdr <group>`(예: `herdr pane`) 출력을 따른다.
대부분의 명령은 JSON을 반환한다. ID는 응답에서 읽고, 예측하지 않는다.

## 현재 위치 파악

```bash
printf '%s\n' "$HERDR_WORKSPACE_ID" "$HERDR_TAB_ID" "$HERDR_PANE_ID"
herdr pane current --current
herdr pane list --workspace "$HERDR_WORKSPACE_ID"
herdr agent list
```

## 일반 명령을 옆 pane에서 실행

사용자가 따로 요청하지 않으면 **현재 탭의 형제 pane + 현재 cwd + 포커스 유지**가 기본이다.

```bash
herdr pane layout --pane "$HERDR_PANE_ID"        # 넓으면 right, 좁거나 길면 down
P=$(herdr pane split --current --direction right --cwd "$PWD" --no-focus | jq -r .result.pane.pane_id)
herdr pane run "$P" "npm test"
herdr pane wait-output "$P" --match "passed" --timeout 120000   # 또는 --regex
herdr pane read "$P" --source recent-unwrapped --lines 120
```

- `pane run`: 명령 텍스트와 Enter를 한 번에 보낸다.
- `wait-output`: 이미 출력된 내용도 매치된다. `--timeout`을 빼면 무한히 기다린다.
- read source: `visible`(현재 화면), `recent`(soft wrap 포함), `recent-unwrapped`(로그·대화 기록에 권장).
  `detection`(감지용 스냅샷)은 `agent read`에서만 쓸 수 있다 (0.9.3 기준)
- 색상이 근거로 필요할 때만 `--format ansi`를 쓴다.

## 다른 코딩 에이전트에게 작업 맡기기

```bash
herdr agent                                                    # 설치된 kind와 옵션 확인
herdr agent start reviewer --kind codex --pane "$P"            # 쉘 프롬프트 상태인 pane이어야 함
herdr agent prompt reviewer "현재 diff를 리뷰하고 조치가 필요한 것만 보고" --wait --timeout 120000
herdr agent read reviewer --source recent-unwrapped --lines 120
```

- 이름 규칙은 `[a-z][a-z0-9_-]{0,31}`이며, 살아 있는 에이전트끼리 이름이 겹치면 안 된다. 에이전트가 끝나면 이름이 해제된다.
- 에이전트 고유 인자는 `--` 뒤에 쓴다: `agent start x --kind codex --pane P -- <args>`
- `agent start`는 레이아웃을 만들지 않는다. 먼저 `pane split`으로 pane을 만든다.
- `--wait`는 `idle`, `done`, `blocked` 중 처음 안정된 상태까지 기다린다. 일반 작업에서는 `--until`을 붙이지 않는다.
- 특정 상태만 기다릴 때: `herdr agent wait reviewer --until blocked --timeout 120000`
- UI 키 입력: `herdr agent send-keys reviewer esc` (`ctrl+c` 등)

## 상태 해석

| 상태 | 의미 |
| - | - |
| `idle`, `done` | 입력 대기 중 (done은 아직 아무도 보지 않은 완료) |
| `working` | 작업 중 |
| `blocked` | 승인·질문 UI를 기다림 → 내용을 확인하고 **사용자에게 물어본 뒤** 응답 |
| `unknown` | 분류할 수 없음. 완료되었다는 뜻이 아니다 |

## 실패 처리

- `agent_blocked`: 입력을 보내기 전에 거부된 것이다. `agent get`, `agent read`로 화면을 확인한다.
- `timeout`, `agent_prompt_stalled`: 전달이 안 됐다는 증거가 아니다. **그대로 다시 보내지 않는다**. 먼저 read로 확인한다.
- `agent_not_ready`(시작 중 blocked): 이름은 유지되므로 read나 send-keys로 처리한다.
- 응답이 길어서 read로 다 안 보이면, 마지막 수단으로 임시 파일에 Markdown으로 쓰게 하고 경로만 답하게 한다.

## 원격 머신

`herdr --machine <label-or-id> <command>`. 저장된 프로필만 쓸 수 있고, 로컬 ID나 `--current`는 원격 pane을 가리키지 않는다.
`--session`, `--remote`와 함께 쓰지 않는다. 연결이 실패해도 변경이 적용됐을 수 있으므로 상태를 확인한 뒤 재시도한다.

## 금지

- 다른 클라이언트가 포커스한 pane에 의존 → `--current`, 명시적 ID, 에이전트 이름을 쓴다
- 직접 만들지 않은 리소스 닫기, `workspace close --group`으로 에러 우회하기
- `herdr server stop`, 메인 herdr 프로세스 kill → 실험은 이름 있는 테스트 세션에서 한다
- 사용자 확인 없이 `--trust-repository` 사용
