# 레이어와 설계

## 패키지와 책임

```text
<base-package>
├── ui/<feature>                    # MVC controller, 요청/응답 DTO, view 변환
├── app/<feature>                   # use case 구현, 조율, port, 트랜잭션
├── domain/<feature>                # 모델, 불변식, 비즈니스 규칙, domain port
└── infra/<adapter>/<feature>       # persistence, HTTP, cache 등의 port 구현
```

| 레이어 | 허용하는 의존 | 담당 책임 |
| --- | --- | --- |
| ui | app의 use case·결과 계약 | HTTP 입력 검증, 응답과 SSR 모델 조립 |
| app | domain, app/domain port | 유스케이스 순서, 권한·정합성 조율, 작업 경계 |
| domain | domain의 타입·계약 | 프레임워크와 무관한 비즈니스 규칙 |
| infra | app/domain port·타입, 외부 라이브러리 | 기술 예외·데이터 변환, 저장·외부 연결 |

- 흐름은 `ui → app → domain`이며 ui에서 repository를 직접 호출하지 않는다.
- app/domain에서 구체 infra 클래스를 import하지 않는다. 필요한 외부 계약은 port로 둔다.
- infra는 그 port를 구현하며 wiring은 해당 infra 기능 아래 설정에서 처리한다.
- controller에 비즈니스 규칙, adapter에 use case 조율을 넣지 않는다.
- HTTP 상태·ProblemDetail·ModelAndView는 ui 책임이다.
  app/domain의 오류 코드와 ui의 HTTP 상태 매핑을 구분한다.
- JPA entity와 domain 모델의 분리는 현재 요구에 필요한 경우만 한다.
  이미 분리한 모델을 임의로 합치거나 JPA 타입을 HTTP 응답에 노출하지 않는다.
- `common/util/config` 보조 패키지는 소유 layer/feature 안에 둔다.
  의미 있는 비즈니스 규칙은 해당 feature에 유지한다.
- Servlet 기반 Spring MVC를 사용한다. WebFlux나 reactive 타입을 무단 도입하지 않는다.

## 설계 판단 순서

1. KISS: 현재 요구를 만족하는 가장 단순하고 읽히는 구체 구현을 선택한다.
2. YAGNI: 미래 가능성만으로 interface/factory/strategy와 추가 계층을 만들지 않는다.
3. SOLID: 실제 책임·변동 경계를 작게 유지하고 계약을 지키는 구현을 만든다.
4. DRY: 같은 비즈니스 의미가 반복될 때 통합하고 우연한 코드 유사성은 유지한다.

- SRP: 클래스·함수에 하나의 변경 이유를 둔다.
- OCP: 확인된 변동 경계에만 확장점을 둔다.
- LSP: port 구현은 결과·오류·부수효과 계약을 깨지 않는다.
- ISP: 범용 거대 interface보다 use case에 필요한 작은 계약을 사용한다.
- DIP: 외부 기술을 app/domain의 port 뒤로 둔다.
- port는 실제 기술 경계에 만들며 모든 클래스에 interface를 짝지어 만들지 않는다.

## 크기 지침

- 파일/모듈은 100줄 이하로 유지한다. 호스트가 허용하면 import 제외 기준을 따른다.
  새 스킬 문서는 import 예외 없이 모든 줄을 포함해 100줄 이하로 작성한다.
- 함수/메서드는 가능한 16줄 이하로 유지한다.
- 초과하면 검증·변환·분기·I/O 중 다른 책임이 섞였는지 확인한다.
- 이름으로 의도를 드러낼 수 있는 private method나 작은 객체로 분리한다.
- 무의미한 추출이나 줄 압축을 피한다. 호스트가 예외를 허용할 때만 근거를 기록한다.
