---
name: herdr-orchestration
description: herdr 안에서 한 pane의 에이전트를 Orchestrator로, 같은 workspace의 다른 에이전트 pane들을 Workers로 두고 업무를 분해·배분·검증한다. 각 worker의 모델과 effort를 화면에서 판별해 난이도별로 배정하고, 사람의 직접 채팅과 외부 에이전트(hermes 등)의 herdr CLI 호출을 모두 받는다. "오케스트레이션", "worker에게 맡겨", "병렬로 나눠", "패널 배분", 외부 에이전트 요청 처리에 사용.
compatibility: Requires herdr CLI (HERDR_ENV=1), jq, bash
metadata:
  version: "1.3"
  herdr-version-checked: "0.9.3"
---

# Herdr Orchestration

herdr CLI 기본 사용법은 herdr-usage 스킬, 상세 규칙은 아래 references를 참고한다.

- [references/routing.md](references/routing.md): 티어 판별, 업무 배정, 동시 실행, 재배정
- [references/protocol.md](references/protocol.md): 이름 규칙, 위임 프롬프트, 보고 형식, 외부 호출 규약
- [references/troubleshooting.md](references/troubleshooting.md)
- [scripts/herdr-call.sh](scripts/herdr-call.sh): worker 조회·식별자 해석·전송·대기·출력

## 역할

| 역할 | 책임 | 하지 않는 것 |
| - | - | - |
| Orchestrator | 요청 해석, 작업 분해, 배정, 결과 검증·통합, 사람·외부 호출자에게 보고, git | 큰 구현을 혼자 오래 붙잡기 |
| Worker | 배정된 범위 안에서 조사·구현·리뷰를 하고 정해진 형식으로 보고 | git commit/push, 범위 밖 수정, 승인 대행 |

## 절차

### 1. 준비 (세션마다 한 번, 그리고 worker 구성이 바뀌었을 때)

```bash
bash <this-skill>/scripts/discover-workers.sh     # pane, 이름, kind, 상태, 모델 힌트
herdr agent rename <pane_id> orchestrator         # 자기 자신
herdr agent rename <pane_id> <kind>-<n>           # 이름이 없는 worker
```

worker 호출은 `bash <this-skill>/scripts/herdr-call.sh`로만 수행하고 **tab ID 또는 pane ID**를 사용한다.
이름은 표시용이며 재시작·세션 교체 시 해제되므로 호출에 의존하지 않는다.
ID는 `status`/`resolve` 조회 결과에서 읽고 tab 번호를 pane 번호로 치환하지 않는다.

`model_hint`로 각 worker의 티어(상/중/하)를 정한다 (routing.md §1). 확신이 없으면 읽기 전용 소작업으로 확인한다.

### 2. 요청 접수

- 사람이 채팅으로 요청했는지, 외부 호출(`[from:<agent> ...]`)인지 구분한다 (protocol.md 진입 경로).
- 여러 단계 작업이면 레포의 이슈 규칙에 따라 계획을 남긴다.

### 3. 분해와 배정

- 호스트 레포가 정한 작업 시간 단위로 쪼갠다 (정해진 값이 없으면 10분).
  각 조각에는 하나의 목표, 수정 범위(파일 목록 또는 읽기 전용), 확인 가능한 완료 기준이 있어야 한다.
- 난이도별로 티어를 정해 배정한다. 쓰기 작업끼리는 파일 범위가 겹치지 않게 한다.
- idle 또는 done 상태인 worker에게만 보낸다. working이면 기다리거나 다른 worker를 쓴다.

### 4. 실행과 대기

- 스크립트 `send`로 여러 worker에게 보내고, 각각 `wait`로 기다린다 (protocol.md 병렬 실행).
- 전송·대기의 exit code와 응답을 확인한다. 실패 응답을 완료로 취급하거나 자동 재전송하지 않는다.
- 기다리는 동안 Orchestrator는 통합 준비나 다른 조각을 처리한다. 오래 걸리면 사람에게 진척을 알린다.

### 5. 검증과 통합

- 보고에서 마지막 `STATUS:` 블록만 파싱한다. 프롬프트에 들어 있던 템플릿도 화면에 남아 있기 때문이다.
- `CHANGED`에 적힌 파일은 `git diff`로 직접 확인하고, 레포의 검증 명령(테스트, 린트, 스킬 검증)을 실행한다.
- worker의 지적은 사실인지 확인한 뒤에 반영한다. 리뷰는 구현한 worker와 다른 worker에게 맡긴다.
- 실패하면 routing.md §4에 따라 재배정한다.

### 6. 보고와 회고

- 사람에게: 결과, 남은 일, 진척도를 짧게 브리핑한다.
- 외부 호출자에게: 마지막 줄에 `RESULT: DONE|BLOCKED|FAILED ...`
- 배정이 잘 맞았는지(티어 판단, 실패 원인)를 레포 문서에 기록한다. 커밋은 레포 규칙을 따른다.

## 안전 규칙

- worker가 `blocked`(승인 대기)면 화면을 확인하고 사람에게 묻는다. Orchestrator가 대신 승인하지 않는다.
- timeout이나 stalled는 전달 실패의 증거가 아니다. 다시 보내기 전에 스크립트 `status`/`read`로 확인한다.
- worker pane을 닫거나 재시작하거나 모델을 바꾸지 않는다. 사람의 판단이 필요하다.
- 외부 호출도 사람 요청과 같은 안전 규칙을 따른다. 되돌리기 어려운 작업은 `RESULT: BLOCKED`로 응답한다.
