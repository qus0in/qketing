# 예외와 분기

## 오류 계약

- 비즈니스/애플리케이션 오류는 RuntimeException 기반 custom unchecked exception으로 둔다.
- 계층은 얕게 유지하고 오류 코드를 enum/VO 등 명시적 타입으로 관리한다.
- 오류 정의에는 안전한 사용자 메시지를 두고 ui의 중앙 매핑에서 HTTP 상태를 정한다.
  domain에 HTTP 타입을 넣거나 controller/service마다 코드·메시지를 흩뿌리지 않는다.
- checked exception은 외부/IO 계약상 필요한 경계에 한정한다.
  infra에서 원인을 보존하며 의미 있는 app/domain 오류로 변환한다.

## API와 SSR 분리

| 요청 | 처리 위치 | 출력 |
| --- | --- | --- |
| API | 범위를 지정한 `@RestControllerAdvice` | ProblemDetail과 적절한 HTTP 상태 |
| SSR | 범위를 지정한 `@ControllerAdvice` 또는 MVC handler | 사용자용 오류 view와 HTTP 상태 |

- controller 패키지·marker annotation 등으로 두 advice의 적용 범위를 겹치지 않게 한다.
  `Accept` 헤더만으로 API/SSR 계약을 추측하지 말고 endpoint 책임을 기준으로 둔다.
- 같은 오류 코드를 공유하고 JSON 구성과 HTML 렌더링 책임은 나눈다.
- API는 ProblemDetail의 `type/title/status/detail/instance`를 필요에 맞게 구성한다.
  `status`와 실제 HTTP 상태를 일치시키고 안전한 `code`·request id를 확장 값으로 둔다.
- 프레임워크 입력 검증·역직렬화 오류도 안전한 4xx 응답으로 일관되게 처리한다.
- SSR은 403/404/500 및 generic 오류 view를 제공하고 공통 layout을 재사용한다.
  오류 화면을 렌더링하더라도 정상 200 응답으로 숨기지 않는다.
- controller 밖에서 발생하는 `/error`와 보안 필터 오류는 호스트의 처리 경로를 확인한다.
  advice만으로 모든 요청 오류를 처리한다고 가정하지 않는다.
- 예상하지 못한 예외는 공통 500으로 처리하고 사용자에게 안전한 request id만 제공한다.
- stack trace·SQL·token·개인정보·원본 예외 메시지는 응답에 노출하지 않는다.
  서버 로그에서도 민감 값을 제거하고 원인을 추적할 정보를 보존한다.

## 흐름과 catch

- 정상 분기는 guard clause/early return으로 표현하고 `else if` 연쇄를 줄인다.
- 누락이나 충돌이 실제 오류 계약일 때만 예외를 던진다.
  예상 가능한 정상 선택 흐름을 try-catch로 구현하지 않는다.
- try-catch는 예외 변환, 원인·context 보강, 명확히 복구 가능한 fallback에만 사용한다.
- catch 후 무시하거나 범용 Exception을 무조건 삼키지 않는다.
  fallback이 영속 불변식을 깨뜨리거나 rollback 실패를 숨기지 않게 한다.

```java
public void rename(String name) {
    if (name == null || name.isBlank()) {
        throw new InvalidNameException();
    }
    this.name = name;
}
```

## 공식 근거

ProblemDetail과 MVC 오류 처리 지원은 호스트 버전에 맞는
[Spring 오류 응답 문서](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)를 확인한다.
