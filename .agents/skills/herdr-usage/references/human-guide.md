# 사람 안내: 설치 · 첫 실행 · 키보드 · 설정

출처: https://herdr.dev/agent-guide.md , 상세: https://herdr.dev/docs/

## 설치

```bash
# Linux / macOS
curl -fsSL https://herdr.dev/install.sh | sh
```

```powershell
# Windows PowerShell
powershell -ExecutionPolicy Bypass -c "irm https://herdr.dev/install.ps1 | iex"
```

```cmd
:: PowerShell이 보안 정책으로 막힐 때 (Command Prompt)
curl.exe -fsSLo install.cmd https://herdr.dev/install.cmd && install.cmd && del install.cmd
```

- Homebrew, mise, Nix 설치: https://herdr.dev/docs/install/
- 직접 설치는 stable 채널을 쓰며 `herdr update`로 갱신한다. 패키지 매니저로 설치했으면 그 매니저로 갱신한다.
- preview 채널은 `herdr channel set preview`로 직접 선택해야 한다.

## 첫 실행 순서

1. 프로젝트로 `cd`한 뒤 `herdr`를 실행한다. 기본 세션이 시작되거나 attach되고, workspace가 자동 생성된다.
   이미 pane 안(`HERDR_ENV=1`)이면 이 단계를 건너뛴다.
2. pane에서 `claude`, `codex` 같은 에이전트를 실행하면 자동으로 감지된다.
   integration이 있으면 설치한다 (예: `herdr integration install claude`는 네이티브 세션 복원을 추가한다.
   Claude의 상태 표시는 여전히 화면 감지로 한다). 지원 목록: https://herdr.dev/docs/agents/
3. 마우스로 시작한다: 클릭해서 포커스, 경계 드래그로 크기 조절, 우클릭 메뉴, 드래그로 복사.
4. 분할은 우클릭 메뉴, 또는 `prefix+v`(오른쪽), `prefix+minus`(아래). 새 탭은 `prefix+c`.
5. detach는 `prefix+q`(`ctrl+b`를 눌렀다 뗀 뒤 `q`)나 터미널 닫기. 모든 것이 계속 실행되며, `herdr`로 다시 attach한다.
6. 완전히 종료하려면 `herdr server stop`.

## 키보드

- 기본 prefix는 `ctrl+b`. `prefix+?`로 현재 바인딩 전체를 볼 수 있다.
- prefix 없이 `ctrl+alt` 조합을 쓰는 검증된 설정은 https://herdr.dev/docs/keyboard/ 를 권한다. 즉흥적으로 만들지 않는다.
- prefix를 포함한 모든 바인딩은 config의 `[keys]`에서 바꿀 수 있다.
- 커스텀 키 초기화: `herdr config reset-keys` (config.toml을 백업한 뒤 제거한다)

## 설정

- 파일 위치: `~/.config/herdr/config.toml` (Linux/macOS), `%APPDATA%\herdr\config.toml` (Windows). 파일이 없어도 동작한다.
- 기본값 출력: `herdr --default-config`
- 실행 중인 서버에 반영: `herdr server reload-config`
- 섹션: `[keys]`, `[theme]`, `[ui]`, `[terminal]`, `[update]`. 전체 레퍼런스: https://herdr.dev/docs/configuration/

## 원격 · 세션

- SSH로 접속해 원격에서 `herdr`를 실행하거나(tmux와 같은 방식), 로컬에서 `herdr --remote <host>`로 얇은 클라이언트로 attach한다.
  각각의 장단점: https://herdr.dev/docs/how-to-work/
- 이름 있는 세션: `herdr session attach <name>`. 완전히 분리된 네임스페이스다.
- detach, 재시작, 업데이트 후 무엇이 유지되는지: https://herdr.dev/docs/session-state/
