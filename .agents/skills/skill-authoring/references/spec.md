# Agent Skills 스펙 요약

출처: https://agentskills.io/specification

## 디렉터리 구조

```
skill-name/
├── SKILL.md      # 필수: frontmatter + 지시
├── scripts/      # 선택: 실행 코드
├── references/   # 선택: 필요 시 읽는 문서
└── assets/       # 선택: 템플릿, 리소스
```

## Frontmatter

| 필드 | 필수 | 제약 |
| - | - | - |
| `name` | O | 1-64자, `a-z0-9-`만, 앞뒤 하이픈·`--` 금지, 상위 디렉터리명과 일치 |
| `description` | O | 1-1024자, 무엇을 하는지 + 언제 쓰는지 + 키워드 |
| `license` | X | 라이선스 이름 또는 번들된 라이선스 파일명 |
| `compatibility` | X | 1-500자, 환경 요구사항(제품, 패키지, 네트워크). 대부분 불필요 |
| `metadata` | X | string → string 맵. 키 이름은 충돌 없게 |
| `allowed-tools` | X | 공백 구분 사전 승인 도구 목록 (실험적) |

### description 예시

- 좋음: `Extracts text and tables from PDF files, fills PDF forms, and merges multiple PDFs. Use when working with PDF documents or when the user mentions PDFs, forms, or document extraction.`
- 나쁨: `Helps with PDFs.`

### allowed-tools 예시

```yaml
allowed-tools: Bash(git:*) Bash(jq:*) Read
```

## Progressive disclosure

1. Metadata (~100 토큰): 시작 시 모든 스킬의 name/description 로드
2. Instructions (< 5000 토큰 권장): 활성화 시 SKILL.md 본문 전체 로드
3. Resources (필요 시): scripts/references/assets 파일

- SKILL.md는 500줄 미만 권장. 상세 내용은 별도 파일로.
- reference 파일은 주제별로 작게 유지 (작을수록 컨텍스트 절약).

## 파일 참조

- 스킬 루트 기준 상대경로: `[가이드](references/REFERENCE.md)`, `scripts/extract.py`
- SKILL.md에서 한 단계 깊이까지만. 참조의 참조 체인 금지.

## scripts 작성 원칙

- 자체 완결적이거나 의존성을 명확히 문서화
- 도움이 되는 에러 메시지
- 엣지 케이스를 우아하게 처리

## 공식 검증 도구

```bash
skills-ref validate ./my-skill
```

https://github.com/agentskills/agentskills/tree/main/skills-ref
