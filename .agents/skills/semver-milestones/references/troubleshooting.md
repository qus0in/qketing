# 문제 해결과 회고

- milestone 중복 → 첫 페이지만 조회함 → `--paginate`와 `state=all`로 생성 전 확인.
- 조회가 POST로 실행됨 → gh api에 `-f`를 사용함 → 조회는 `--method GET` 명시.
- milestone 404 → issue 번호나 id를 사용함 → 응답의 milestone `number`와 대상 레포 확인.
- 연결 실패 → title 일부만 전달함 → 전체 title을 조회해 `--milestone`에 전달.
- 이슈가 이전 목표에서 사라짐 → 단일 milestone 연결을 덮어씀 → 단계별 작업 이슈 분리.
- tag와 버전 불일치 → SNAPSHOT 또는 CI override 잔존 → 실제 빌드 버전까지 검증.
- 같은 버전의 다른 산출물 → tag 커밋과 checkout 불일치 → HEAD·tag SHA와 digest 검증.
- tag 조회 실패 → shallow checkout에 ref 누락 → 호스트 CI에서 해당 tag ref 확보.
- Release만 존재 → 자동 tag 생성에 의존함 → 기존 검증 tag와 `--verify-tag` 사용.
- Release 게시 실패 → 권한 또는 부분 실패 → 원격 tag·이미지·Release 상태부터 재조회.
- 이미지 태그 불일치 → 두 번 빌드함 → 동일 빌드/digest에 version과 SHA tag 부여.
- milestone 종료 후 미완료 발견 → 이슈 수만 완료 기준으로 삼음 → 검증 기준·연기 기록 확인.
- 버전 순서 역전 → 문자열 비교 사용 → major/minor/patch를 숫자로 비교.

## 부분 실패와 불변성

- 이미 성공한 tag/이미지/Release 게시를 재시도로 덮어쓰지 않는다.
- 같은 커밋·버전·digest의 미완료 단계만 호스트 권한 안에서 재개한다.
- 잘못 게시한 릴리스는 영향을 기록하고 새 patch 등 새 버전으로 수정한다.
- 배포 rollback은 검증된 이전 digest를 사용하며 새 tag를 기존 커밋으로 강제 이동하지 않는다.

## 회고

- 계획 버전·빌드 버전·tag·이미지·Release를 서로 다른 원본 역할로 검토한다.
- 정책 문서와 명령을 분리해 레포별 roadmap이나 CI 구현을 복제하지 않는다.
- 재사용할 오류 원인·해결은 이 reference에 추가하고 모든 파일 100줄 이하를 검증한다.
- 레포 고유 값은 허용된 host 문서에만 기록한다. 범위 밖이면 수정하지 않고 보고한다.
- 완료 보고에는 변경 파일, 실행한 검증, 미완료 단계와 다음 작업을 포함한다.
