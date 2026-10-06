# JavaScript

## 변수와 문법

- var를 사용하지 않는다. binding 재할당이 없으면 const를 사용한다.
- let은 loop counter나 실제 상태 재할당처럼 필요한 위치에만 좁은 scope로 둔다.
- const가 객체 내부까지 불변으로 만들지는 않는다. 공유 가변 상태를 숨기지 않는다.
- ES6+ 모듈 import/export로 기능을 나누고 전역 변수와 inline event handler를 피한다.
- 콜백에는 화살표 함수를 우선한다. 동적 this가 필요한 함수는 일반 함수를 사용한다.
  이벤트 처리에서는 this 대신 event.currentTarget 등 명시적인 값을 우선한다.
- 구조분해·템플릿 리터럴·spread를 읽기 쉬운 곳에 사용하고 불필요한 복사를 피한다.
- optional chaining과 nullish coalescing으로 선택적인 값만 처리한다.
  필수 DOM/설정 누락을 조용히 숨기지 않는다. 0/false가 유효하면 || 대신 ??를 검토한다.
- 초기화 전 필수 요소를 검증하고 실패 시 SSR 기본 동작을 보존한다.
- 함수/메서드는 가능한 16줄 이하로 검증·변환·DOM 갱신 책임을 나눈다.

## DOM 조회

- document/Element의 querySelector 또는 querySelectorAll만 조회 API로 사용한다.
- getElementById, getElementsByClassName, getElementsByTagName,
  getElementsByName, document.forms 같은 우회 조회를 사용하지 않는다.
- 사용자 입력을 selector 문자열에 직접 삽입하지 않는다.
  고정 selector로 조회하고 dataset 값을 비교한다. 동적 selector가 필요하면 안전하게 escape한다.
- querySelector는 null일 수 있다. querySelectorAll은 static NodeList이므로
  동적 삽입 이후 갱신이 필요하면 다시 조회한다.
- contains/matches는 이미 받은 노드의 관계/조건 판별에 사용한다.
  closest 등 별도 조회 API를 추가해 querySelector 규칙을 우회하지 않는다.

## 이벤트 위임

- 반복·동적 항목은 공통 부모에 listener 하나를 두고 허용한 action만 처리한다.
- 이벤트가 bubble되는지 확인한다. focus는 focusin 등을 검토하고
  비버블링 이벤트는 capture 또는 실제 필요한 개별 listener를 사용한다.
- 중첩 아이콘/텍스트를 클릭해도 버튼이 동작하도록 target 포함 관계를 확인한다.
  위임 컨테이너 밖의 대상과 다른 중첩 컨테이너의 action을 처리하지 않는다.
- 이벤트마다 고정 selector로 현재 버튼 목록을 조회하면 동적 삽입에도 적용된다.
  대규모 목록은 측정 후 처리 범위를 줄이며 새 조회 API로 규칙을 우회하지 않는다.

```javascript
const panel = document.querySelector('[data-panel]');
const handleClick = (event) => {
  const { target } = event;
  if (!(target instanceof Element)) return;
  const buttons = panel.querySelectorAll('[data-action="toggle"]');
  const button = Array.from(buttons).find((item) => item.contains(target));
  if (!button) return;
  const expanded = button.getAttribute('aria-expanded') === 'true';
  button.setAttribute('aria-expanded', String(!expanded));
};
if (panel) panel.addEventListener('click', handleClick);
```

예시는 대상 판별만 보여준다. 실제 toggle은 제어 대상의 hidden 상태도 함께 갱신한다.
SSR에서는 기본 내용을 표시하고 초기화 성공 후 button과 접기 기능을 활성화한다.

- preventDefault는 강화 동작을 처리할 수 있을 때만 사용한다.
  SSR form/navigation fallback을 불필요하게 차단하지 않는다.
- 부분 화면 교체에서 listener를 중복 등록하지 않는다.
  안정적인 부모에 위임하거나 같은 함수 참조/AbortController로 lifecycle 종료 시 해제한다.
- 비동기 실패에서 명확한 사용자 메시지와 복구 흐름을 제공한다.
  try-catch로 정상 분기를 만들거나 실패를 삼키지 않는다.
