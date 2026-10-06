---
name: herdr-usage
description: Herdr(AI 코딩 에이전트용 터미널 워크스페이스 매니저) 활용법. 사람에게 설치·개념·마우스/키보드 사용·설정·진단을 안내하거나, herdr pane 안(HERDR_ENV=1)에서 에이전트가 pane 분할, 명령 실행, 출력 읽기, 다른 에이전트 시작·프롬프트·대기를 할 때 사용. "herdr", "pane", "workspace", "다른 에이전트에게 맡겨", "병렬 에이전트" 같은 요청에 사용.
compatibility: Requires herdr CLI; workspace scripts require bash, jq and HERDR_ENV=1
metadata:
  version: "1.1"
  herdr-version-checked: "0.9.3"
---

# Herdr 활용

Herdr는 tmux 같은 멀티플렉서이지만 **마우스 우선이고 에이전트를 인식**한다.
백그라운드 서버가 터미널을 소유하므로 detach나 SSH 연결이 끊겨도 pane은 계속 실행된다.
사이드바에는 각 에이전트 상태(`working`/`blocked`/`done`/`idle`/`unknown`)가 표시된다.

## 0. 상황 판별

```bash
test "${HERDR_ENV:-}" = 1 && echo inside || echo outside
```

| 상황 | 할 일 |
| - | - |
| 사람이 설치·사용법·문제를 물음 | [references/human-guide.md](references/human-guide.md) |
| pane 안에서 herdr를 직접 제어해야 함 | [references/agent-control.md](references/agent-control.md) |
| workspace 저장·복구 | [references/workspace-restore.md](references/workspace-restore.md) |
| 감지 오류·키 미동작·시작 문제 | [references/troubleshooting.md](references/troubleshooting.md) |

- `HERDR_ENV=1`이면 사람은 이미 attach된 상태다. pane 안에서 `herdr`를 실행하라고 안내하지 않는다 (중첩 실행은 차단된다).
- `HERDR_ENV`가 없으면 herdr 세션을 제어하지 않는다. 사람 안내만 한다.

## 1. 권위 있는 출처

설치된 버전의 바이너리가 기준이다. 이 스킬은 요약일 뿐이다.

```bash
herdr --version      # 버전
herdr --skill        # 이 버전용 공식 에이전트 스킬 전문
herdr --help         # 전체 명령
herdr pane           # 그룹 도움말 (agent, workspace, tab, worktree 등도 같은 방식)
```

- 키바인딩, 설정 키, CLI 플래그를 지어내지 않는다. 확실하지 않으면 위 명령이나 https://herdr.dev/docs/ 를 확인한다.
- 탐색 목적으로 bare `herdr`를 실행하지 않는다 (TUI가 실행되거나 attach된다).
- 변경 명령(`workspace create` 등)은 인자 없이도 기본값으로 실행되므로, 도움말을 보려고 실행해 보지 않는다.

## 2. 개념 (가르치는 순서)

1. **Session**: 영속 백그라운드 서버 네임스페이스. 대부분 기본 세션 하나면 충분하다
2. **Workspace**: 레포·작업 단위 컨테이너. 사이드바 상태가 여기로 집계된다
3. **Tab**: workspace 안의 레이아웃 (예: `agents`, `logs`, `server`)
4. **Pane**: 실제 터미널. 오른쪽이나 아래로 분할된다
5. **Agent**: pane 안에서 인식된 코딩 에이전트 프로세스
6. **Modes**: terminal(키를 pane으로 보냄), prefix(`ctrl+b` 후 키 하나), navigate

ID 형식: workspace `w1`, tab `w1:t1`, pane `w1:p1`. 불투명한 값이므로 항상 JSON 응답에서 읽는다.

## 3. 핵심 규칙

- 사람에게는 **마우스를 먼저** 가르친다. 클릭, 경계 드래그, 우클릭 메뉴로 모두 할 수 있다.
- Herdr는 tmux가 아니다. tmux 명령, `.tmux.conf` 문법을 안내하지 않는다.
- 자동화나 스크립트는 CLI 레퍼런스와 socket API 문서를 안내한다.
  https://herdr.dev/docs/cli-reference/ , https://herdr.dev/docs/socket-api/
- 자신이 만들지 않은 workspace, tab, pane, session은 닫지 않는다.
- 활성 세션에서 `herdr server stop`을 실행하지 않는다 (모든 pane 프로세스가 종료된다).
- 공식 스킬 설치(`npx skills add herdrdev/herdr --skill herdr -g`)나 사용자 설정 파일 수정은 먼저 묻는다.

## 4. Workspace 저장·복구

- [scripts/ws-save.sh](scripts/ws-save.sh)로 workspace/tab 라벨·pane cwd·agent kind/이름을 JSON에 저장한다.
- 기본 경로는 `${HERDR_MANIFEST:-$HOME/.config/herdr/workspace-layout.json}`이다.
- [scripts/ws-restore.sh](scripts/ws-restore.sh)는 기본 dry-run으로 차이를 출력한다. 검토 후 `--apply`로 복구한다.
- Herdr의 `~/.config/herdr/session.json`은 topology를 복원하지만 에이전트 이름은 복원하지 않는다.
- 복구는 부족한 구성 생성·agent start·rename이다. 기존 pane을 종료·재시작하거나 이름을 임의 생성하지 않는다.
- tab ID → pane ID는 조회로만 연결한다. 서로의 번호를 문자열 치환하지 않는다.
- worker 호출은 형제 스킬 herdr-orchestration의 `scripts/herdr-call.sh`를 tab/pane ID로 사용한다.
- 분할 방향·비율·대화 세션·실행 인자는 이 매니페스트의 저장 범위가 아니다. 상세 절차와 충돌 처리는 위 reference를 따른다.
