# 검증·문제 해결·회고

## 변경 검토

1. host 규칙과 허용 범위를 확인하고 패키지·버전·운영 값을 임의로 고정하지 않았는지 본다.
2. import와 호출을 확인해 ui의 repository 직접 접근과 app/domain의 infra 의존을 찾는다.
3. app 구현체의 조회 readOnly·변경 경계, 여러 repository의 원자성,
   self-invocation·외부 I/O·예외 삼킴을 확인한다.
4. API/SSR advice 범위, HTTP 상태, 안전한 오류 코드와 메시지 노출을 확인한다.
5. record DTO·생성자 주입·설정 소유자·상수 중복·변환 방향을 점검한다.
6. 캐시 TTL·miss 원본 조회·정합성 판정과 운영 schema migration/validate를 점검한다.
7. 사용자 정렬 allow-list, structured JSON, request/correlation id의 전파·정리를 점검한다.
8. 파일 100줄·메서드 가능한 16줄을 점검하고 책임을 분리한 이유를 설명한다.

## 의미 있는 동작 검증

- 호스트의 빌드·관련 테스트를 실행하고 변경한 동작에 필요한 검증만 추가한다.
- 쓰기 실패에서 여러 repository 변경이 모두 rollback되는지 실제 app bean 호출로 확인한다.
  테스트 자체 트랜잭션이 누락된 서비스 경계를 숨기지 않도록 한다.
- 동시성·잠금·constraint 요구는 실제 사용하는 DB의 통합 검증으로 확인한다.
  대체 인메모리 DB 결과만으로 운영 DB의 동작을 보장하지 않는다.
- API는 ProblemDetail·HTTP 상태·오류 코드와 민감정보 비노출을 확인한다.
- SSR은 오류 view·HTTP 상태와 API advice가 개입하지 않는지 확인한다.
- 설정은 누락·잘못된 값의 검증과 환경별 override가 작동하는지 확인한다.
- 캐시 만료·miss에서 원본 조회, 정합성 판정에서 캐시 배제를 확인한다.
- 운영 설정의 validate와 실제 DB migration 적용을 확인한다.
- 미등록 정렬 키·방향·악의적 fragment가 쿼리 생성 전에 거부되는지 확인한다.
- 로그가 JSON으로 파싱되고 요청 id가 응답·로그에서 일치하며 다음 요청에 누출되지 않는지 확인한다.
- 문서만 변경했다면 링크·줄 수·frontmatter를 검증한다.
  구현을 그대로 복제한 테스트나 무관한 반복 테스트를 만들지 않는다.

## 트러블슈팅

- write가 반영되지 않음 → readOnly 기본값을 상속함 → 변경 메서드에 명시적 override.
- rollback이 적용되지 않음 → self-invocation/직접 생성 → 프록시를 통한 app bean 호출.
- 일부 변경만 남음 → repository별 암묵적 경계 → app use case에 원자 작업 경계 선언.
- 실패 뒤 commit됨 → 예외를 catch하고 정상 반환 → unchecked 변환 또는 명시적 복구 정책.
- SSR에 JSON이 표시됨 → advice 범위 겹침 → controller 책임별 적용 범위 분리.
- 렌더링 중 lazy 오류 → entity가 UI까지 전달됨 → app 경계 내 결과 DTO 구성.
- rollback 후 이벤트가 남음 → commit 전 외부 발행 → commit 이후 발행·재시도 설계.
- commit 후 이벤트가 유실됨 → DB와 발행이 비원자적 → 전달 요구에 맞는 outbox/재조정 검토.
- 문서 줄 수 초과 → 여러 책임을 한 파일에 기록 → 주제별 reference로 분리.
- 오래된 캐시로 최종 결정 → 캐시를 원장으로 취급 → TTL·원본 조회와 영속 최종 판정 분리.
- 운영 schema가 자동 변경됨 → ddl-auto update 사용 → validate와 migration 단일 관리 적용.
- 정렬 입력이 쿼리 표현식으로 실행됨 → 입력 path 직접 사용 → 고정 allow-list 매핑.
- 요청 id가 다음 요청에 남음 → MDC 정리 누락 → finally 정리와 비동기 context 정책 확인.
- starter 버전을 못 찾음(`Could not find ...starter-x:.`) → BOM 미적용 → dependency-management plugin 또는 platform BOM 추가.
- 오류 화면에 escape된 `<title …>` 마크업이 출력됨 → layout fragment 파라미터명이 model 속성과 같아 가려짐
  → 파라미터명을 구분(`titleTag`)하고, 테스트는 문구 포함이 아니라 escape 마크업 부재까지 검증한다.
- 브라우저 404가 JSON으로 나옴 → controller 밖 오류(no handler, 405)는 패키지 한정 advice가 못 잡음
  → 최고 순위 advice에서 `produces`로 HTML/JSON 분기. ProblemDetail 자동 handler(order 0)보다 앞서야 한다.
- 브랜드 prefix가 API title에도 붙음 → 메시지 하나를 HTML `<title>`과 API에 공용 → 메시지는 순수 문구, prefix는 템플릿에서.

## 회고와 보고

- 반복 가능한 해결책만 이 스킬에 반영하고 레포 이름·버전·이슈 번호는 넣지 않는다.
- 이슈의 환경 결정과 범용 설계 원칙을 구분해 placeholder와 host 규칙으로 일반화한다.
- 줄 수 검증만으로 내용의 정확성을 보장하지 않는다.
  의존 방향·트랜잭션·오류 응답에 관한 빠진 의미를 별도로 검토한다.
- 데이터 경계 리뷰에는 캐시·schema·정렬·요청 추적도 포함해 트랜잭션 선언 밖의 누락을 찾는다.
- 레포 고유 사실은 허용된 host 문서에 기록한다. 범위 밖이면 수정하지 않고 보고한다.
- 결과에는 변경 내용, 검증 결과, 남은 위험·다음 필요한 작업과 진척 정도를 포함한다.
