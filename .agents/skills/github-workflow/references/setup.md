# 레포 초기 설정 (dev → main, CI 통과 시 자동 머지)

`<repo>`=`owner/name`, `<check>`=CI job 이름(필수 상태 체크로 쓰임).

## 1. dev 브랜치

```bash
git switch -c dev && git push -u origin dev
```

## 2. 레포 옵션

```bash
gh api -X PATCH repos/<repo> \
  -F allow_auto_merge=true -F allow_merge_commit=true \
  -F allow_squash_merge=false -F allow_rebase_merge=false \
  -F delete_branch_on_merge=false
```

- `delete_branch_on_merge=false`: true면 머지 후 dev가 삭제된다.
- merge commit만 허용해서 dev와 main 히스토리가 갈라지지 않게 한다.

## 3. main 보호

```bash
gh api -X PUT repos/<repo>/branches/main/protection --input - <<'EOF'
{
  "required_status_checks": { "strict": true, "contexts": ["<check>"] },
  "enforce_admins": true,
  "required_pull_request_reviews": { "required_approving_review_count": 0 },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
EOF
```

- `contexts`는 workflow 이름이 아니라 **job 이름**(또는 `name:`)과 일치해야 한다.
- 승인 0명: 1인 레포에서도 PR 필수 + 자동 머지가 가능하다.
- `enforce_admins: true`면 관리자도 main에 직접 push할 수 없다.

## 4. CI 워크플로

`.github/workflows/<file>.yml`에서 `pull_request: branches: [main]`을 트리거로 두고,
job 이름을 `<check>`와 같게 한다.

## 5. 확인

```bash
gh api repos/<repo>/branches/main/protection \
  -q '{checks:.required_status_checks.contexts, admins:.enforce_admins.enabled}'
gh api repos/<repo> -q '{allow_auto_merge, delete_branch_on_merge}'
```
