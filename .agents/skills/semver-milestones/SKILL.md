---
name: semver-milestones
description: GitHub milestone으로 SemVer 로드맵을 운영하고 SNAPSHOT 개발 버전을 release tag와 GitHub Release로 승격한다. 버전 계획·milestone 생성과 이슈 연결·릴리스 준비에서 사용한다. "SemVer", "milestone", "SNAPSHOT", "버전 승격", "release tag" 같은 요청에 사용.
compatibility: 명령 예시는 git, 인증된 gh CLI, bash를 사용하며 빌드 도구는 호스트 설정을 따른다.
metadata:
  version: "1.0"
---

# SemVer와 milestone 운영

계획·빌드·릴리스의 역할을 구분하고 검증된 커밋만 릴리스한다.
소유자·레포·브랜치·이슈 번호·버전·registry는 호스트에서 확인한다.

## 절차

1. 호스트 규칙, 로드맵의 원본 이슈/문서, 빌드 버전 원본, 릴리스 CI를 확인한다.
   기존 milestone·tag·Release를 조회하고 중복 생성이나 무단 버전 변경을 피한다.
2. [버전 정책](references/version-policy.md)에 따라 응집된 목표와 완료 기준을 정한다.
   로드맵 원본에 범위·의존·진척을 모으고 세부 설계는 관련 이슈에 남긴다.
3. [milestone 명령](references/milestone-commands.md)으로 목표를 조회·생성하고
   실제 구현 이슈를 연결한다. 계획 완료와 릴리스 완료를 구분한다.
4. [빌드 버전](references/build-version.md)에 따라 개발 버전을 단일 원본에서 관리한다.
   자동 버전 계산 플러그인은 실제 요구가 없다면 추가하지 않는다.
5. [승격 체크리스트](references/promotion.md)의 검증을 마친 뒤 호스트가 허용한
   절차로 release 버전·tag·이미지·GitHub Release를 확정한다.
6. 결과와 남은 작업을 보고하고 다음 개발 버전과 milestone을 정리한다.
   [문제 해결과 회고](references/troubleshooting.md)에 재사용할 해결책을 반영한다.

## 경계와 연결

- milestone은 계획된 통합 목표, tag는 검증된 릴리스 커밋, Release는 배포 기록이다.
- milestone 존재·종료만으로 릴리스가 완료됐다고 판단하지 않는다.
- 이슈 작성·라벨·PR·브랜치 보호 절차는 설치된 `github-workflow` 스킬을 따른다.
  없는 레포에서는 호스트의 동등한 절차를 따른다. 이 스킬에 그 규칙을 복제하지 않는다.
- 커밋·push·tag 발행·Release 게시의 실행 권한은 사용자 요청과 호스트 규칙을 따른다.
  명령 예시를 읽었다는 이유로 해당 작업이 허가된 것으로 간주하지 않는다.
- 작업을 10분 이내 단위로 나누고 완료 후 진척과 다음 필요한 작업을 보고한다.

## 입출력 예시

입력: `<owner>/<repo>`의 `<version> — <scope>` milestone 승격 준비.
출력: 목표와 연결 이슈 현황, 빌드 버전·tag 검증 결과, 이미지 version/SHA/digest,
Release 기록과 다음 개발 버전. 허용되지 않은 외부 변경은 실행하지 않는다.

## 검증 도구

[scripts/verify-release-version.sh](scripts/verify-release-version.sh)는
`vX.Y.Z`와 빌드 버전 `X.Y.Z`의 형식·일치를 검사한다.
일반 정식 버전만 허용하며 prerelease·build metadata 정책은 호스트에서 별도로 정한다.

## 엣지 케이스

- patch를 통합 milestone으로 추적해도 이미 닫은 목표의 범위를 조용히 변경하지 않는다.
- 하나의 이슈는 하나의 milestone에 연결된다. 여러 단계 구현은 작업 이슈를 분리한다.
- 이미 게시된 tag·이미지를 덮어쓰지 않고 수정 릴리스는 새 버전으로 만든다.
