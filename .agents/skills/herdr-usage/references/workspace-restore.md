# Workspace 매니페스트 저장·복구

## 저장

Herdr는 `~/.config/herdr/session.json`으로 workspace·tab 라벨·pane cwd를 재시작 시 복원한다.
에이전트 이름은 복원하지 않는다. 종료 전에 이 스킬의 매니페스트를 별도로 저장한다.

```bash
bash <this-skill>/scripts/ws-save.sh
bash <this-skill>/scripts/ws-save.sh /tmp/workspace-layout.json
```

- 기본 경로는 `${HERDR_MANIFEST:-$HOME/.config/herdr/workspace-layout.json}`이다.
- version 1 JSON: workspaces(workspace_id·label), tabs(tab_id·workspace_id·label),
  panes(pane_id·tab_id·workspace_id·index·cwd·kind·name), saved_at.
- 현재 서버의 모든 workspace를 조회하고 각각 tab/pane 목록을 합친다. 저장은 임시 파일→rename으로 교체한다.
- 파일 권한은 600이다. cwd·라벨·이름을 포함하므로 공유 경로에 게시하기 전 내용을 확인한다.
- 이름 없는 에이전트도 null name으로 저장된다. 자동 복구 시 임의 이름을 만들지 않는다.

## 복구 (먼저 dry-run)

```bash
bash <this-skill>/scripts/ws-restore.sh /tmp/workspace-layout.json
bash <this-skill>/scripts/ws-restore.sh --apply /tmp/workspace-layout.json
```

- 경로 생략 시 저장과 같은 기본값을 쓴다. HERDR_ENV=1인 Herdr pane, bash, jq, herdr가 필요하다.
- dry-run은 would/skip으로 차이를 출력한다. 생성·기동·rename은 `--apply`에서만 한다.
- 작업 전에 매니페스트 schema·부모 연결·중복 ID/이름·pane index를 검증한다. 읽기 실패는 복구하지 않는다.
- workspace/tab은 기존 ID와 라벨을 우선 확인한다. ID가 바뀌면 같은 부모의 유일한 라벨로 연결한다.
- 같은 라벨이 여럿이면 오류로 중단한다. 무라벨(null) 구성도 중복되면 구분할 수 없어 중단한다.
- 신규 workspace의 기본 tab을 첫 저장 tab에 연결하고 라벨을 복구한다. 불필요한 기본 tab을 추가로 남기지 않는다.
- pane은 해당 tab의 저장 ID를 우선 확인하고, 없으면 저장된 index와 현재 조회 순서로 연결한다.
- tab/pane ID는 각각 실제 조회·생성 응답에서 읽는다. 번호 치환·ID 정렬로 순서를 추정하지 않는다.
- 부족한 pane은 right split으로 생성하고 저장 cwd를 적용한다. 새로운 구성에서도 포커스를 유지한다.
- cwd가 다르면 기존 pane을 이동·종료하지 않고 skip한다. 기존 추가 tab/pane은 제거하지 않는다.
- 저장 kind/name이 있고 비어 있는 shell이면 agent start, 같은 kind의 agent 이름이 다르면 rename한다.
- 저장 이름이 없거나 다른 kind가 실행 중이거나 이름이 다른 pane에 점유되어 있으면 skip한다.
- shell이 사용 가능한지는 `agent start`가 판정한다. 실패 시 자동 입력·재시작·재전송하지 않는다.
- 이미 일치하는 구성에는 변경하지 않는다. 마지막 ok는 비교 완료를 뜻하며 skip의 성공을 뜻하지 않는다.
- apply가 중간에 실패해도 기존 pane을 닫아 rollback하지 않는다. dry-run으로 다시 확인한 뒤 필요한 항목만 진행한다.

## 복구 한계와 검증

- 현재 Herdr session 복원이 분할 구조를 유지한 경우 기존 pane을 재사용한다.
- 사라진 topology를 만드는 경우 pane 개수·cwd·종류·이름을 복구한다. 정확한 분할 방향·크기·위치는 보장하지 않는다.
- 에이전트 대화 세션, resume 인자, 모델/effort, shell 명령·환경변수는 저장하지 않는다.
- 같은 cwd의 여러 pane은 ID가 사라지면 index로 연결되므로 apply 전에 dry-run의 대상·현 구성을 확인한다.
- 실제 검증은 안전한 기존/테스트 workspace에서 진행한다. 타인의 pane을 닫거나 서버를 중지해 검증하지 않는다.
- 정상 흐름: 저장→dry-run→apply→다시 dry-run에서 변경 없음 확인. 무명 agent·종류 충돌·중복 라벨·불량 schema도 확인한다.
- 작업 중 다른 사람이 구성을 바꾸면 snapshot이 달라질 수 있다. 동시 topology 변경을 피하고 dry-run을 다시 확인한다.
- worker 호출은 herdr-orchestration의 `scripts/herdr-call.sh`를 tab/pane ID로 사용한다. 복구된 이름에 호출을 의존하지 않는다.

## 회고

- 표시 이름과 pane 주소는 수명이 다르다. 이름은 저장된 값만 재부여하고 주소는 조회로 새로 연결한다.
- 복구 도구는 기존 process를 유지하며 부족한 항목만 추가해야 한다. 충돌은 추측으로 덮지 않고 skip/오류로 드러낸다.
