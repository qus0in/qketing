# 데이터 접근과 로깅

## 캐시와 원본

- 영속 원본이 있고 반복 조회 비용을 줄일 필요가 있는 읽기 데이터를 캐시한다.
- 캐시 항목별 TTL을 명시한다. 환경·운영에 따라 변하는 TTL은 타입 안전 설정으로 관리한다.
- cache miss·만료 시 원본 저장소를 조회하고 성공한 조회 결과를 캐시에 채운다.
  캐시 장애 시에도 원본을 조회할 수 있는지 호스트의 장애 정책에 맞게 정한다.
- 변경 후 무효화·갱신 정책을 명시하고 DB rollback 전에 캐시를 확정하지 않는다.
- 예약 확정·잔여 수량·권한 등 정합성이 필요한 값은 cache source of truth로 쓰지 않는다.
  캐시 값이나 TTL 만료 이벤트만으로 최종 결정을 내리지 않고 영속 원본에서 검증한다.
- 캐시·Pub/Sub는 영속 원장의 대체물이 아니다. 최종 불변식은 app의 짧은 영속 경계에서 지킨다.

## 영속 schema

- JPA 사용 시 운영에서 `spring.jpa.hibernate.ddl-auto=validate`를 적용한다.
  schema를 자동 변경하는 `update/create/create-drop`은 운영에 사용하지 않는다.
- 운영 schema 변경은 호스트의 migration 도구로만 관리한다.
  특정 도구·DB 제품·버전을 강제하지 않고 버전 관리된 migration을 검토·실행한다.
- 테이블·인덱스·제약·확장 등 변경을 migration에 기록하고 수동 DDL과 ORM 자동 생성을 혼용하지 않는다.
- migration은 schema 검증보다 먼저 완료한다. validate는 migration 실행을 대신하지 않는다.
- ORM 검증이 모든 인덱스·제약·확장을 확인한다고 가정하지 않는다.
  실제 DB에서 migration 적용·호환성·필요한 schema 요소를 별도로 확인한다.

## 사용자 입력과 동적 정렬

- QueryDSL 등 쿼리 도구와 무관하게 사용자 입력 기반 동적 정렬은 allow-list만 허용한다.
- 공개 정렬 키를 서버가 미리 정의한 필드/타입 안전 표현식으로 매핑한다.
  예: `<sort-key>`를 대응하는 고정 필드 표현식으로 변환한다.
- 사용자 문자열을 컬럼명·경로·함수·SQL/JPQL fragment로 직접 사용하거나 이어 붙이지 않는다.
  임의 path/template 또는 unsafe sort로 전달하지 않는다.
- 정렬 방향도 허용한 ASC/DESC enum으로 변환하고 미등록 키·방향은 입력 오류로 처리한다.
- 값 parameter binding만으로 식별자·orderBy injection이 방지된다고 판단하지 않는다.
  노출하지 않을 필드는 유효한 entity 속성이어도 정렬을 허용하지 않는다.

## structured JSON과 요청 추적

- 운영 로그는 structured JSON으로 출력하고 수집기가 필드로 파싱할 수 있게 한다.
  문자열로 JSON을 직접 조립하지 않고 호스트의 지원 로깅 설정/encoder를 사용한다.
- timestamp·level·logger·message와 request/correlation id를 명시적 필드로 기록한다.
  기존 tracing이 있으면 trace/span id와 호스트의 상관관계 규칙을 재사용한다.
- MVC 요청 진입점에서 id를 생성하거나 검증된 id를 받아 로그 context에 설정한다.
  외부 입력 id는 길이·형식을 검증하고 임의 헤더 값을 그대로 신뢰하지 않는다.
- 오류 응답의 안전한 request id와 서버 로그 id를 일치시키고 외부 호출에도 필요한 context를 전달한다.
- MDC 등 thread-local context는 요청 종료 시 finally에서 정리한다.
  비동기 실행·스레드 전환 시 자동 전파를 가정하지 말고 전달·정리 정책을 확인한다.
- token·개인정보·원문 요청/응답을 로그에 남기지 않고 필요한 값만 마스킹·선별한다.
  JSON escaping과 로그 수집 필드 보존을 확인하고 로그 제품·schema 버전은 고정하지 않는다.

## 공식 근거

호스트 버전의 [Spring Boot 로깅](https://docs.spring.io/spring-boot/reference/features/logging.html)과
[Spring Data JPA 쿼리](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html) 지원을 확인한다.
