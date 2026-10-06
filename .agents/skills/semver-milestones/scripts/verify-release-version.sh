#!/usr/bin/env bash
# 정식 release tag와 실제 빌드 버전의 형식·일치를 검증한다.
# 사용법: bash verify-release-version.sh <vX.Y.Z> <build-version>
# 의존성: bash. 파일·네트워크·git 상태를 변경하지 않는다.
# SNAPSHOT, prerelease, build metadata는 이 스크립트의 지원 범위 밖이다.
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo '사용법: verify-release-version.sh <vX.Y.Z> <build-version>' >&2
  exit 2
fi

release_tag="$1"
build_version="$2"
numeric_part='(0|[1-9][0-9]*)'
release_pattern="^v${numeric_part}\.${numeric_part}\.${numeric_part}$"

if [[ ! "$release_tag" =~ $release_pattern ]]; then
  echo "오류: 일반 정식 tag vX.Y.Z 형식이 아님: $release_tag" >&2
  exit 1
fi

if [ "${release_tag#v}" != "$build_version" ]; then
  echo "오류: tag와 빌드 버전 불일치: $release_tag / $build_version" >&2
  exit 1
fi

echo "PASS: $release_tag / $build_version"
