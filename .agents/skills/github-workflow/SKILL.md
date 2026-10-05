---
name: github-workflow
description: gh CLI로 GitHub 이슈·PR·라벨·브랜치 보호를 운영하는 규칙. 업무 요청을 받으면 plan/work 이슈로 계획과 진척을 남기고, dev 브랜치에서 main으로 feature/fix/docs 라벨 PR을 열어 CI 통과 시 자동 머지한다. 현황 보고 요청 시 report 이슈를 쓴다. 이슈 작성, PR 생성, 라벨, 브랜치 보호, 자동 머지, 보고 요청에 사용.
compatibility: Requires git and an authenticated gh CLI
metadata:
  version: "1.0"
---

# GitHub Workflow

라벨 의미는 [references/labels.md](references/labels.md),
레포 초기 설정은 [references/setup.md](references/setup.md),
문제 해결은 [references/troubleshooting.md](references/troubleshooting.md).

## 원칙

- `memo` 라벨은 사람 전용이다. 에이전트는 memo 이슈를 **만들거나, 수정하거나, 라벨을 붙이지 않는다**. 읽고 참고만 한다.
- `main`에는 직접 push하지 않는다. 모든 변경은 `dev` → `main` PR로, CI 통과 후 자동 머지한다.
- 레포마다 브랜치명, 라벨, CI 체크 이름이 다를 수 있다. 호스트 레포 문서를 먼저 확인한다.

## 1. 업무 요청을 받았을 때

1. 관련 열린 이슈를 확인한다: `gh issue list --state open`
2. 여러 단계 작업이면 `plan` 이슈를 만든다 (목표, 단계 체크리스트, 완료 기준).
   ```bash
   gh issue create --label plan --title "[plan] <요약>" --body-file <file>
   ```
3. 진행 중 의미 있는 진척·결정·막힌 점은 `work` 라벨로 남긴다.
   - plan 이슈가 있으면 해당 이슈에 댓글: `gh issue comment <n> --body ...`
   - 독립 작업이면 `work` 이슈를 새로 만든다.
4. 완료되면 결과와 PR 링크를 댓글로 남기고 이슈를 닫는다.

## 2. 변경을 반영할 때 (PR)

```bash
git fetch origin && git switch dev && git merge --ff-only origin/dev
git merge --no-edit origin/main   # 직전 PR의 merge commit 동기화 (보통 fast-forward)
# ... 작업 & 커밋 ...
git push origin dev
gh pr create --base main --head dev --label <feature|fix|docs> \
  --title "<type>: <요약>" --body "<변경 요약, Closes #n>"
gh pr merge --auto --merge
```

- PR 라벨: 기능 `feature`, 버그 `fix`, 문서·스킬만 변경 `docs`. 섞이면 주된 성격 하나.
- 이미 열린 dev→main PR이 있으면 새로 만들지 말고 push만 한다 (`gh pr list --head dev`).
- 머지 방식은 **merge commit**. squash/rebase는 dev와 main 히스토리를 갈라지게 한다.
- CI 실패 시: `gh pr checks` → `gh run view <id> --log-failed`로 원인 확인 후 dev에 수정 push.

## 3. 보고 요청을 받았을 때

사용자가 현황·상황 보고를 요청하면 `report` 이슈를 만든다.

- 내용: 요약 → 완료/진행 중/막힌 항목 → 관련 이슈·PR 링크 → 다음 할 일
- 근거는 `gh issue list`, `gh pr list`, `gh run list`, `git log`로 확인한 사실만 쓴다.

```bash
gh issue create --label report --title "[report] <YYYY-MM-DD> <주제>" --body-file <file>
```

## 4. 작업 후

회고 결과를 스킬·문서에 반영하고 같은 PR 흐름으로 올린다 (skill-authoring 스킬 참고).
