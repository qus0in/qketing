---
name: spring-boot-conventions
description: Spring Boot MVC의 ui/app/domain/infra 레이어, 트랜잭션, 예외, DTO와 설정 컨벤션을 적용하고 검토한다. Spring MVC 구현·리팩터링·코드 리뷰에서 사용한다. "레이어 의존 방향", "트랜잭션 경계", "ProblemDetail", "record DTO", "ConfigurationProperties" 같은 요청에 사용.
metadata:
  version: "1.1"
---

# Spring Boot 컨벤션

Spring MVC 애플리케이션을 작은 책임과 명시적인 경계로 구현한다.
패키지·버전·빌드 도구·설정값은 호스트 레포에서 확인하며 임의로 고정하지 않는다.

## 절차

1. 호스트 규칙과 기존 코드·빌드 설정을 읽고 MVC 스택과 지원 기능을 확인한다.
   이미 정한 아키텍처와 충돌하면 먼저 충돌을 보고하고 무단 전환하지 않는다.
2. 변경할 기능과 실제 요구를 정리하고 KISS/YAGNI를 먼저 적용한다.
   [레이어와 설계](references/layers.md)에 따라 의존 방향과 책임을 정한다.
3. 영속 조회·변경·외부 I/O를 구분하고 app 구현체에 트랜잭션을 선언한다.
   [트랜잭션](references/transactions.md)의 readOnly·프록시·이벤트 규칙을 확인한다.
4. 오류를 명시적 타입으로 정의하고 API와 SSR에서 각각 변환한다.
   [예외 처리](references/errors.md)의 guard clause와 응답 노출 규칙을 따른다.
5. DTO/VO는 record를 우선하고 생성자 주입과 타입 안전 설정을 적용한다.
   [DTO·설정·상수](references/data-config.md)에 따라 변환과 값의 소유자를 정한다.
   [데이터 접근과 로깅](references/data-access-logging.md)의 캐시·schema·정렬·로그 규칙을 적용한다.
6. 파일 100줄 이하·메서드 가능한 16줄 이하로 책임을 점검한다.
   [검증과 문제 해결](references/review.md)에 따라 관련 동작을 확인하고 결과를 보고한다.

## 핵심 기준

- `ui → app → domain`, `infra → app/domain의 port` 방향을 유지한다.
- domain은 HTTP·Spring MVC·트랜잭션·구체 infra 구현에 의존하지 않는다.
- DB 조회 use case는 `@Transactional(readOnly = true)`, 변경은 `@Transactional`이다.
- unchecked custom exception을 사용하고 API는 ProblemDetail, SSR은 오류 view로 처리한다.
- `@Data`와 field injection을 피하고 필요한 Lombok 기능만 선택한다.
- 별도 Mapper 계층을 만들지 않고 책임에 맞는 `from/of/toXxx`로 변환한다.
- `common/util/config`를 최상위 레이어나 무관한 코드의 보관함으로 만들지 않는다.
- 캐시 TTL과 원본 조회를 명시하고 정합성의 최종 판정은 영속 원본에서 수행한다.
- 운영 schema는 migration으로 관리하고 사용자 정렬 입력은 allow-list로 제한한다.
- 로그는 structured JSON으로 출력하고 request/correlation id로 요청을 연결한다.

## 입출력 예시

입력: `<base-package>` 아래 `<feature>`의 조회·변경 API와 SSR 화면 구현 요청.
출력: `ui/<feature>`의 controller/DTO, `app/<feature>`의 use case,
`domain/<feature>`의 규칙, `infra/persistence/<feature>`의 adapter와 관련 검증 결과.
기존 경계·이름을 재사용하고 필요한 파일만 추가한다.

## 엣지 케이스

- 캐시·외부 저장소만 쓰는 연산에 불필요한 DB 트랜잭션을 열지 않는다.
- record나 ProblemDetail 미지원 버전이면 호스트의 호환 구현을 따르고 차이를 보고한다.
- 줄 수를 줄이기 위해 한 줄에 코드를 압축하거나 의미 없는 메서드를 추출하지 않는다.
- 회고에서 재사용할 해결책은 관련 reference에 반영한다.
  레포 고유 결정은 호스트 문서에 기록하되 현재 작업의 허용 범위를 따른다.
