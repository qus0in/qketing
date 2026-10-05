# 스킬 운영 (qketing)

## 위치

- 원본: `.agents/skills/<name>/`
- `.claude/skills`는 `.agents/skills`를 가리키는 심볼릭 링크다. 편집은 원본 경로에서 한다.

## 레포 규칙 적용

- 모든 파일 100줄 이하 → 검증 시 `max-lines`를 100으로 지정한다.
- 레포 전용 내용(경로, 결정, 설정)은 스킬이 아니라 `docs/`에 기록한다.

```bash
bash .agents/skills/skill-authoring/scripts/validate.sh .agents/skills/<name> 100
```

## 스킬 목록

| 스킬 | 용도 |
| - | - |
| `skill-authoring` | Agent Skills 표준에 맞춰 스킬 작성/업데이트·검증, 작업 후 회고 반영 |
| `github-workflow` | 라벨·이슈(plan/work/report)·dev→main PR·자동 머지 운영 규칙 |
| `herdr-usage` | Herdr 사람 안내(설치·개념·키보드·설정)와 pane 안 에이전트 제어 요약 |

CI가 모든 스킬을 자동 검증한다 (`docs/github.md` 참고).

## 변경 이력

- 2026-10-05: `skill-authoring` 스킬 추가 (agentskills.io 스펙 기반, validate.sh 포함)
- 2026-10-05: `github-workflow` 스킬 추가, `.claude/skills` 링크를 상대경로로 변경
- 2026-10-05: `herdr-usage` 스킬 추가, skill-authoring에 공식 스킬 요약 원칙 추가 (v1.2)
