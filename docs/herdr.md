# Herdr 환경 (qketing)

일반 사용법은 `.agents/skills/herdr-usage` 스킬을 따른다. 여기에는 이 레포 작업 환경에서 확인한 사실만 기록한다.

## 확인된 환경 (2026-10-05)

- herdr 0.9.3 (stable 채널), 설치 경로 `~/.local/bin/herdr`
- 이 레포의 에이전트들(Claude Code, Codex, OpenCode)은 herdr pane 안에서 실행된다 (`HERDR_ENV=1`)
- 같은 herdr 서버에서 다른 레포의 에이전트(예: opencode)도 함께 돌아간다
  → `herdr agent list`에 다른 프로젝트 pane도 나온다. 이 레포 작업에서는 그 pane들을 제어하지 않는다
- 공식 herdr 스킬(`npx skills add herdrdev/herdr --skill herdr -g`)은 전역에 설치되어 있지 않다.
  필요하면 `herdr --skill`로 이 버전의 원문을 읽는다

## 이 레포에서의 사용 원칙

- 테스트, 서버, 긴 명령은 현재 탭에 형제 pane을 만들어 `--no-focus`로 실행하고, 결과는 `pane read`로 확인한다
- 같은 workspace의 다른 에이전트 pane은 Orchestrator/Workers 방식으로 활용한다 (`docs/orchestration.md`)
- 새 에이전트를 `agent start`로 띄우는 것은 사용자가 요청한 경우에만 한다
- 직접 만든 pane은 작업이 끝나면 정리한다 (`herdr pane close <id>`). 다른 pane은 건드리지 않는다

## 스킬을 공식 스킬과 분리한 이유

- 공식 스킬은 214줄이라 이 레포의 100줄 규칙을 넘는다. 또 버전마다 바뀐다
- 그래서 `herdr-usage`는 요약과 사람 안내에 집중하고, 정확한 문법은 `herdr --skill`과 `--help`로 확인하도록 했다
- 이름이 `herdr`가 아닌 이유: 공식 스킬을 전역 설치했을 때 이름이 충돌하지 않게 하기 위해서

## 종료 후 복구 절차

herdr 재시작(또는 머신 재부팅) 후 workspace를 저장한 상태로 되돌릴 때 쓴다.

1. 저장: `bash .agents/skills/herdr-usage/scripts/ws-save.sh`
2. herdr 종료 (또는 재부팅)
3. herdr 재기동 후 attach
4. 대조: `bash .agents/skills/herdr-usage/scripts/ws-restore.sh` (dry-run, 차이만 출력)
5. 적용: `bash .agents/skills/herdr-usage/scripts/ws-restore.sh --apply`
6. 확인: `bash .agents/skills/herdr-orchestration/scripts/herdr-call.sh status` 로 이름·라벨 대조

### 매니페스트 위치

- 기본: `~/.config/herdr/workspace-layout.json` — 레포에 두지 않는다
- 이유: 머신 고유 절대경로, 휘발성 pane id, 여러 레포(qketing, plantaro)의 workspace가 함께 담긴다.
  레포에 커밋하면 경로 노출과 잦은 변경이 생긴다
- hermes의 `workspace-manifest.json`과 schema가 달라 파일을 분리한다. 공유하면 저장 시 hermes 복구 파일을 덮어쓴다.
- 필요하면 매니페스트 경로 인자나 `HERDR_MANIFEST` 환경변수로 바꾼다

### 사실 기록

- herdr는 `~/.config/herdr/session.json`으로 레이아웃은 복원하지만 에이전트 이름은 복원하지 않는다.
  → 5단계(rename)가 필요하다

## 변경 이력

- 2026-10-06: '종료 후 복구 절차' 섹션 추가 (ws-save/ws-restore, 매니페스트는 `~/.config/herdr`)
- 2026-10-05: `herdr-usage` 스킬 추가 (herdr.dev/agent-guide.md, `herdr --skill` 0.9.3 기반) (#4)
