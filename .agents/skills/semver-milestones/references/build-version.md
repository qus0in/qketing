# 빌드 버전과 tag 일치 검증

## 단일 원본

- Gradle 예시는 `gradle.properties`의 `version=<version>-SNAPSHOT`을 원본으로 둔다.
  정식 승격 시 SNAPSHOT을 제거하며 build script에 같은 버전을 중복 하드코딩하지 않는다.
- Gradle의 실제 `project.version`이 속성을 사용하는지 확인한다.
  환경변수·CI 인자·플러그인·서브프로젝트 override로 달라지는 값도 점검한다.
- Maven은 유효 project.version, Node는 package 버전처럼 호스트 빌드 도구의
  버전 원본과 실제 해석 결과를 따른다. 특정 파일 이름을 모든 도구에 강제하지 않는다.
- multi-module의 단일 제품 버전과 독립 모듈 버전을 먼저 구분한다.
  독립 릴리스라면 대상 모듈·tag naming·이미지 매핑을 호스트 정책으로 정의한다.

## Gradle 예시 스니펫

CI는 tag가 가리키는 커밋을 checkout한 뒤 실행한다.
아래는 루트 제품 버전이 단일 `gradle.properties`에 정의된 경우의 예시다.
다른 도구나 복합 Gradle 빌드는 실제 버전을 출력하는 호스트 명령으로 대체한다.

```bash
set -euo pipefail
release_tag='<actual-release-tag>'
build_version=$(awk -F= '
  /^[[:space:]]*version[[:space:]]*=/ {
    value=$0; sub(/^[^=]*=/,"",value)
    gsub(/^[[:space:]]+|[[:space:]]+$/,"",value)
    count++; version=value
  }
  END { if (count != 1) exit 1; print version }
' gradle.properties)
resolved_version=$(./gradlew -q properties | awk '
  /^version: / { count++; value=substr($0,10) }
  END { if (count != 1) exit 1; print value }
')
test "$build_version" = "$resolved_version"
bash '<skill-dir>/scripts/verify-release-version.sh' \
  "$release_tag" "$resolved_version"
test "$(git rev-parse "refs/tags/$release_tag^{commit}")" = \
  "$(git rev-parse HEAD)"
```

- tag는 실제 CI 이벤트/입력에서 받고 브랜치 이름을 tag로 오인하지 않는다.
- checkout이 해당 tag ref를 포함하는지 확인한다. shallow clone이면 호스트 CI의
  checkout/fetch 설정에서 필요한 tag와 이력을 확보한다.
- 버전만 같아도 다른 커밋이면 실패한다. tag 생성 전 검증은 버전 검사만 실행하고,
  생성 후·CI에서는 위 커밋 일치까지 검증한다.
- 형식 불일치·SNAPSHOT·누락·중복·해석 차이가 있으면 게시 전에 실패시킨다.
- 이 스킬의 검증 스크립트는 일반 `X.Y.Z` 릴리스만 지원한다.
  SemVer prerelease/build metadata까지 지원하는 범용 파서라고 간주하지 않는다.
