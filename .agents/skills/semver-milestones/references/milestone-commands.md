# GitHub milestone 명령

gh 인증과 대상 레포를 확인한다. 아래 placeholder를 실제 값으로 바꾼다.
조회부터 실행하고 생성·연결·종료는 허용된 작업일 때만 실행한다.

```bash
milestone_repo='<owner>/<repo>'
milestone_title='<version> — <scope>'
milestone_issue='<issue-number>'
```

## 조회와 생성

전체 페이지·open/closed를 확인해 제목이 같은 목표를 중복 생성하지 않는다.
`-f` 사용 시 조회에는 `--method GET`을 명시한다.

```bash
gh api --method GET --paginate "repos/$milestone_repo/milestones" \
  -f state=all -f per_page=100 \
  --jq '.[] | {number,title,state,open_issues,closed_issues}'
```

생성 전 범위·완료 기준을 설명 파일에 작성한다. 응답의 `number`를 보존한다.
due date는 호스트가 실제 기한을 정한 경우만 `due_on`에 UTC ISO 8601로 추가한다.

```bash
gh api --method POST "repos/$milestone_repo/milestones" \
  -f title="$milestone_title" -F description=@'<description-file>' \
  -f state=open --jq '{number,title,html_url}'
milestone_number='<returned-number>'
gh api "repos/$milestone_repo/milestones/$milestone_number"
```

`milestone_number`는 milestone 번호다. issue 번호나 REST 응답의 `id`와 혼동하지 않는다.

## 이슈 연결과 확인

```bash
gh issue edit "$milestone_issue" --repo "$milestone_repo" \
  --milestone "$milestone_title"
gh issue view "$milestone_issue" --repo "$milestone_repo" \
  --json number,title,state,milestone
gh api --method GET --paginate "repos/$milestone_repo/issues" \
  -f milestone="$milestone_number" -f state=all -f per_page=100 \
  --jq '.[] | select(.pull_request == null) | {number,title,state}'
```

- 제목은 숫자뿐 아니라 전체 milestone title과 일치시킨다.
- issues REST 응답에는 PR도 포함될 수 있어 위 조회는 PR을 제외한다.
- 기존 연결이 있다면 변경 사유를 확인한다. 연결은 기존 milestone을 대체한다.
- 이슈 생성·라벨·진척 기록 방식은 호스트의 github-workflow 절차에 연결한다.

## 종료

완료 기준과 미해결 이슈를 확인한다. 연기한 일은 이유와 새 목표를 명시해 이동한다.
열린 이슈 수가 0이라는 사실만으로 릴리스 성공을 선언하지 않는다.

```bash
gh api --method PATCH "repos/$milestone_repo/milestones/$milestone_number" \
  -f state=closed --jq '{number,title,state,open_issues,closed_issues}'
```

재개가 필요한 경우 같은 endpoint에 `state=open`을 사용하고 사유를 기록한다.
재시도 전 상태를 조회해 성공한 POST를 다시 실행하지 않는다.

명령 계약은 [GitHub milestone API](https://docs.github.com/en/rest/issues/milestones)와
[gh issue edit](https://cli.github.com/manual/gh_issue_edit)를 확인한다.
