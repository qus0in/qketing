# 승격 체크리스트

## 게시 전 준비

- [ ] 로드맵 목표와 이슈 범위가 합의됐고 필수 완료 기준을 검증했다.
- [ ] 연기·제외한 작업은 이유와 다음 목표를 기록했다.
- [ ] 호스트의 테스트·CI·보안·호환성·migration·배포/rollback 관련 검증을 통과했다.
- [ ] 변경 내용·검증·알려진 제한·migration·이미지 정보를 릴리스 노트 파일에 작성했다.
- [ ] 단일 버전 원본에서 SNAPSHOT을 제거하고 실제 빌드 버전 일치를 확인했다.
- [ ] 호스트의 허용 절차로 변경을 반영했고 릴리스 커밋이 원격에 존재한다.
- [ ] 깨끗한 릴리스 커밋에 대해 tag 이름·버전·기존 tag/Release 충돌을 확인했다.
- [ ] 호스트가 main E2E를 요구하면 버전 PR 머지 후 실행의 headSha가 release_commit과 같은지 확인했다. 버전 승격 전 성공으로 대체하지 않고 artifact도 검토한다. 확인 불가/실패 시 tag를 만들지 않는다.

커밋·push·PR의 방법은 github-workflow에 맡긴다.
승인이나 권한이 필요한 단계는 호스트 규칙에 따라 정지하고 준비 결과를 보고한다.

## tag에서 Release까지

아래는 실행 권한이 있는 경우의 예시다. CI가 수행하는 단계를 로컬에서 중복 실행하지 않는다.
`release_commit`은 검증·반영된 정확한 커밋 SHA이며 브랜치의 이동하는 HEAD로 대체하지 않는다.

```bash
release_repo='<owner>/<repo>'
release_version='<version>'
release_tag="v$release_version"
release_commit='<verified-commit-sha>'
git tag -a "$release_tag" "$release_commit" -m "Release $release_version"
git push origin "refs/tags/$release_tag"
```

- [ ] tag CI가 해당 커밋을 checkout하고 버전·커밋 일치 검증을 통과했다.
- [ ] 정식 버전으로 빌드한 산출물과 이미지 검증이 통과했다.
- [ ] 다음 이미지 태그 두 개가 동일 digest를 가리킨다.
- [ ] tag·커밋 SHA·digest와 산출물 검증 결과를 릴리스 기록에 남겼다.

```bash
release_image='<registry>/<namespace>/<image>'
release_sha=$(git rev-parse HEAD)
docker build -t "$release_image:$release_version" \
  -t "$release_image:sha-$release_sha" .
docker push "$release_image:$release_version"
docker push "$release_image:sha-$release_sha"
gh release create "$release_tag" --repo "$release_repo" --verify-tag \
  --title "$release_tag" --notes-file '<release-notes-file>'
gh release view "$release_tag" --repo "$release_repo" \
  --json tagName,isDraft,isPrerelease,url
```

이미지 예시는 tag 커밋을 checkout하고 HEAD 일치 검증을 통과한 환경에서 실행한다.
이미지 내부 산출물의 버전도 확인하고 재빌드 대신 검증된 digest 승격이 가능하면 사용한다.
Release에 파일을 첨부할 때는 검증된 산출물 경로를 create 명령의 인자로 전달한다.
`--verify-tag`는 원격 tag 존재를 요구하며 tag를 기본 브랜치에서 자동 생성하지 않게 한다.
릴리스가 사용자에게 전달됐다는 판정은 호스트의 게시·배포 완료 기준을 따른다.

## 종료와 다음 개발

- [ ] 게시/배포 검증 결과·Release 링크를 로드맵 원본에 기록했다.
- [ ] 목표의 완료 기준에 따라 milestone을 종료했다.
- [ ] 다음 minor 또는 patch 목표를 선택하고 `<next-version>-SNAPSHOT`으로 변경했다.
- [ ] 다음 개발 버전 변경은 release tag 뒤의 새 커밋으로 처리했다.

명령의 동작은 [gh release create](https://cli.github.com/manual/gh_release_create)를 확인한다.
