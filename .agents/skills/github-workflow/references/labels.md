# 라벨 규칙

| 라벨 | 대상 | 작성자 | 언제 |
| - | - | - | - |
| `memo` | 이슈 | 사람만 | 사람이 직접 남기는 메모·요청. 에이전트는 읽기만 |
| `plan` | 이슈 | 에이전트 | 업무 요청을 받아 작업 계획을 세울 때 |
| `work` | 이슈/댓글 | 에이전트 | 작업 진척, 결정, 막힌 점을 기록할 때 |
| `report` | 이슈 | 에이전트 | 사용자가 현황·상황 보고를 요청했을 때 |
| `handle` | 이슈 | 에이전트 → 사람이 처리 | 설정값 입력, 외부 처리(인증·결제·외부 서비스) 등 사람의 조치가 필요할 때 |
| `feature` | PR | 에이전트 | 기능 추가 |
| `fix` | PR | 에이전트 | 버그 수정 |
| `docs` | PR | 에이전트 | 문서·스킬만 변경 |

## 생성 명령

```bash
while IFS='|' read -r n c d; do
  gh label create "$n" --color "$c" --description "$d" --force
done <<'EOF'
memo|0e044d|사람이 직접 작성한 이슈
plan|1d76db|업무 요청을 받아 에이전트가 작성한 작업 계획
work|0e8a16|에이전트의 작업 진척 기록
feature|a2eeef|PR: 기능 추가
fix|d73a4a|PR: 버그 수정
docs|0075ca|PR: 문서/스킬 변경
report|fbca04|사용자 요청에 따른 현황·상황 보고
handle|b60205|사람의 조치 필요: 설정값(시크릿·환경변수·계정 설정) 입력, 외부 처리(인증·결제·외부 서비스 작업)
EOF
```

기본 라벨(bug, enhancement 등)을 지우려면:

```bash
gh label list --json name -q '.[].name' | grep -vxE 'memo|plan|work|feature|fix|docs|report|handle' \
  | while IFS= read -r l; do gh label delete "$l" --yes; done
```

라벨을 삭제하면 해당 라벨이 붙은 이슈·PR에서도 떨어진다.
