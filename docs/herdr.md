# Herdr 환경 (qketing)

일반 사용법은 `.agents/skills/herdr-usage` 스킬을 따른다. 여기에는 이 레포 작업 환경에서 확인한 사실만 기록한다.

## 확인된 환경 (2026-10-05)

- herdr 0.9.3 (stable 채널), 설치 경로 `~/.local/bin/herdr`
- 이 레포의 에이전트(Claude Code)는 herdr pane 안에서 실행된다 (`HERDR_ENV=1`)
- 같은 herdr 서버에서 다른 레포의 에이전트(예: opencode)도 함께 돌아간다
  → `herdr agent list`에 다른 프로젝트 pane도 나온다. 이 레포 작업에서는 그 pane들을 제어하지 않는다
- 공식 herdr 스킬(`npx skills add herdrdev/herdr --skill herdr -g`)은 전역에 설치되어 있지 않다.
  필요하면 `herdr --skill`로 이 버전의 원문을 읽는다

## 이 레포에서의 사용 원칙

- 테스트, 서버, 긴 명령은 현재 탭에 형제 pane을 만들어 `--no-focus`로 실행하고, 결과는 `pane read`로 확인한다
- 리뷰 같은 병렬 에이전트 작업은 사용자가 요청한 경우에만 `agent start`로 시작한다
- 직접 만든 pane은 작업이 끝나면 정리한다 (`herdr pane close <id>`). 다른 pane은 건드리지 않는다

## 스킬을 공식 스킬과 분리한 이유

- 공식 스킬은 214줄이라 이 레포의 100줄 규칙을 넘는다. 또 버전마다 바뀐다
- 그래서 `herdr-usage`는 요약과 사람 안내에 집중하고, 정확한 문법은 `herdr --skill`과 `--help`로 확인하도록 했다
- 이름이 `herdr`가 아닌 이유: 공식 스킬을 전역 설치했을 때 이름이 충돌하지 않게 하기 위해서

## 변경 이력

- 2026-10-05: `herdr-usage` 스킬 추가 (herdr.dev/agent-guide.md, `herdr --skill` 0.9.3 기반) (#4)
