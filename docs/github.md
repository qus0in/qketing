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

## 라벨

`memo`(사람 전용), `plan`, `work`, `report`(이슈), `feature`, `fix`, `docs`(PR).
의미는 `github-workflow/references/labels.md` 참고.

## 변경 이력

- 2026-10-05: 기본 라벨 삭제(memo만 유지), 라벨 6종 추가, dev 브랜치 생성, main 보호, CI 추가
- 2026-10-05: `.claude/skills` 심볼릭 링크를 절대경로 → 상대경로(`../.agents/skills`)로 변경
