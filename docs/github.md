# GitHub 운영 (qketing)

일반 규칙은 `.agents/skills/github-workflow` 스킬을 따른다. 여기에는 이 레포의 설정값만 기록한다.

## 브랜치

- `main`: 보호됨. 직접 push 금지(관리자 포함), force push·삭제 금지
- `dev`: 작업 브랜치. `dev` → `main` PR만 사용

## 머지 조건

- 필수 상태 체크: `ci` (`.github/workflows/ci.yml`의 job), strict(최신 main 기준)
- 승인 0명, auto-merge 허용, merge commit만 허용 (squash/rebase 비활성)
- 머지 후 head 브랜치 자동 삭제 꺼짐 (dev 유지)

## CI (`ci` job)

- `.agents/skills/*` 전체를 `skill-authoring/scripts/validate.sh`로 검증 (최대 100줄)
- 추적되는 모든 파일 100줄 이하 (AGENTS.md 규칙), 심볼릭 링크 제외
- 트리거: `dev` push(조기 피드백) + `main` 대상 PR. 그래서 한 커밋에 두 번 실행된다
- 문서만 변경(`docs/`, `.agents/`, `.claude/`, `*.md`)되면 `changes` job이 판별해 `ci` job을 skip한다.
  skip된 job은 필수 체크를 통과로 처리한다
- CI에서만 빼는 것이다. 커밋 전 로컬 검증은 문서·스킬을 **포함해** 그대로 실행한다
  (모든 스킬 `validate.sh <dir> 100`, 추적 파일 전체 100줄 검사)

## 라벨

`memo`(사람 전용), `plan`, `work`, `report`, `handle`(이슈), `feature`, `fix`, `docs`(PR).
의미는 `github-workflow/references/labels.md` 참고.

## Milestone

- SemVer 버전별 milestone 13개 (`0.1.0 — Bootstrap` ~ `1.0.0 — First Stable`). 목록과 범위는 `docs/roadmap.md`
- 구현 `work` 이슈는 milestone을 지정하고 HEAD #7과 관련 plan 이슈를 reference한다
- plan 이슈(#7~#16)는 여러 milestone에 걸치므로 milestone을 지정하지 않는다

## 변경 이력

- 2026-10-05: 기본 라벨 삭제(memo만 유지), 라벨 6종 추가, dev 브랜치 생성, main 보호, CI 추가
- 2026-10-05: `.claude/skills` 심볼릭 링크를 절대경로 → 상대경로(`../.agents/skills`)로 변경
- 2026-10-06: `handle` 라벨 추가 (설정값 입력, 외부 처리 등 사람의 조치가 필요할 때)
- 2026-10-06: SemVer milestone 13개 생성 (#18)
- 2026-10-06: 문서 전용 변경은 CI skip (`changes` job + job 조건)
