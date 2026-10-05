---
name: skill-authoring
description: Agent Skills 표준(agentskills.io)에 맞춰 스킬을 새로 작성하거나 기존 스킬을 업데이트한다. SKILL.md 작성, frontmatter(name/description) 규칙 검증, references/scripts/assets 분리, 작업 후 회고 내용을 스킬에 반영할 때 사용한다. "스킬 만들어", "스킬 업데이트", "SKILL.md", "회고 반영" 같은 요청에 사용.
metadata:
  version: "1.2"
---

# Skill Authoring

Agent Skills 표준에 맞는 스킬을 작성/갱신하는 절차.
상세 스펙은 [references/spec.md](references/spec.md), 템플릿은 [assets/SKILL.template.md](assets/SKILL.template.md).

## 0. 먼저 확인할 것

1. 호스트 레포의 규칙 파일(`AGENTS.md`, `CLAUDE.md` 등)을 읽고 다음을 파악한다.
   - 스킬 위치 (예: `.agents/skills/`, `.claude/skills/`)
   - 파일당 최대 줄 수 등 추가 제약
   - 레포 전용 내용을 기록할 위치 (예: `docs/`)
2. 같은 목적의 스킬이 이미 있는지 확인한다. 있으면 새로 만들지 말고 업데이트한다.
   ```bash
   grep -l "^name:" <skills-dir>/*/SKILL.md
   ```

## 1. 무엇을 스킬에 넣을지 결정

| 내용 | 위치 |
| - | - |
| 다른 레포에서도 재사용 가능한 절차·지식·트러블슈팅 | 스킬 |
| 특정 레포의 경로, 설정값, 의사결정 기록 | 레포 문서(`docs/` 등) |

- 스킬 본문에 특정 레포 이름·절대경로·계정 정보를 하드코딩하지 않는다.
- 레포마다 달라지는 값은 "호스트 레포 규칙을 따른다"로 쓰거나 스크립트 인자/환경변수로 받는다.
- 도구가 공식 스킬이나 `--help`를 제공하면 원문을 복제하지 말고 요약한 뒤 원본을 가리킨다.
  확인한 도구 버전은 `metadata`(예: `<tool>-version-checked`)에 남기고, 공식 스킬과 이름이 겹치지 않게 짓는다.

## 2. 작성

1. 디렉터리 이름 = `name`. 소문자·숫자·하이픈만, 64자 이하, `--`·앞뒤 하이픈 금지.
2. `description`은 **무엇을 하는지 + 언제 쓰는지 + 트리거 키워드**를 1024자 이내로 쓴다.
   에이전트는 시작 시 name/description만 보고 활성화 여부를 판단한다.
3. 본문은 단계별 절차 → 입출력 예시 → 엣지 케이스 순으로 짧게 쓴다.
4. 길어지면 분리한다 (progressive disclosure).
   - `references/`: 필요할 때만 읽는 상세 문서 (주제별 작은 파일)
   - `scripts/`: 반복 실행되는 결정적 작업. 의존성과 사용법을 파일 상단에 명시
   - `assets/`: 템플릿, 스키마, 데이터
5. 파일 참조는 스킬 루트 기준 상대경로, SKILL.md에서 한 단계 깊이까지만.

## 3. 업데이트

- 기존 지시와 충돌하는 내용은 덧붙이지 말고 **교체**한다. 낡은 내용은 삭제한다.
- 트러블슈팅은 `증상 → 원인 → 해결` 한 줄 형식으로 해당 스킬의 references에 추가한다.
- 변경 성격에 따라 `metadata.version`을 올린다 (지시 변경: minor, 오탈자: 생략 가능).
- 파일이 줄 수 제한에 가까워지면 주제별로 `references/`에 분리한다.

## 4. 검증

```bash
bash <this-skill>/scripts/validate.sh <skill-dir> [max-lines]
```

- frontmatter 필수 필드, name 규칙/디렉터리 일치, description·compatibility 길이,
  파일별 줄 수(기본 500, 레포 규칙이 있으면 그 값), 상대경로 링크 존재 여부를 검사한다.
- `skills-ref`가 설치돼 있으면 `skills-ref validate <skill-dir>`도 실행한다.

## 5. 작업 후 회고

작업을 마치면 다음을 스스로 점검하고 반영한다.

1. 이번 작업에서 반복될 만한 절차/실수/해결책이 있었나? → 관련 스킬 업데이트 또는 신규 스킬
2. 이 레포에만 해당하는 사실(경로, 결정, 설정)이 생겼나? → 레포 문서에 기록
3. 반영 후 2·4단계 규칙으로 다시 검증한다.

## 엣지 케이스

- description에 YAML 특수문자(`:` 뒤 공백, `#`)가 있으면 따옴표로 감싼다.
- 심볼릭 링크로 여러 에이전트 디렉터리가 같은 스킬 폴더를 공유할 수 있다. 원본 경로에서 편집한다.
  링크는 상대경로로 만든다 (`ln -s ../.agents/skills .claude/skills`). 절대경로는 클론·CI에서 깨진다.
- `allowed-tools`는 실험적 필드라 에이전트마다 지원이 다르다. 꼭 필요할 때만 쓴다.
