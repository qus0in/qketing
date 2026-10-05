# 트러블슈팅

형식: 증상 → 원인 → 해결

- `gh repo view --json autoMergeAllowed` 실패 → gh의 repo view에 해당 필드 없음
  → `gh api repos/<repo> -q .allow_auto_merge`로 조회
- 브랜치 보호 API 403/404 (private 레포) → 무료 플랜의 private 레포는 보호 미지원
  → public 전환 또는 유료 플랜. 먼저 `gh api repos/<repo> -q .visibility` 확인
- PR이 "Expected — Waiting for status" 상태로 머무름 → 보호 규칙 `contexts`가 job 이름과 다름
  → workflow의 job id/`name:`과 `contexts`를 일치시킴
- 머지 후 dev 브랜치가 사라짐 → `delete_branch_on_merge=true`
  → false로 설정하고 `git push origin <sha>:refs/heads/dev`로 복구
- 다음 PR에 이전 커밋이 다시 보이거나 충돌 → squash/rebase 머지로 히스토리가 갈라짐
  → merge commit만 허용, 작업 전 `git merge origin/main`으로 dev 동기화
- `gh pr merge --auto` 실패 "auto merge is not allowed" → 레포 옵션 꺼짐
  → `gh api -X PATCH repos/<repo> -F allow_auto_merge=true`
- `--auto` 실행 시 "Pull request is in clean status" 에러 → 이미 모든 조건 충족
  → `--auto` 없이 `gh pr merge --merge`로 즉시 머지
- CI에서 커밋된 심볼릭 링크가 깨짐 → 로컬 절대경로로 링크 생성
  → `ln -s ../<target> <link>`처럼 상대경로로 생성
- `gh pr checks --watch` 통과 직후 PR이 아직 OPEN → auto-merge 반영에 수 초~수십 초 지연
  → 잠시 후 `gh pr view <n> --json state,mergedAt`로 재확인. 실패로 판단하지 않는다
- 같은 커밋에 CI가 두 번 실행 → workflow에 `push: [dev]`와 `pull_request: [main]` 트리거가 모두 있음
  → 정상 동작. 줄이려면 push 트리거 제거 (PR 전 조기 피드백은 잃음)
