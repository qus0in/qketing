# SSR 템플릿과 progressive enhancement

## 기본 HTML 계약

- Thymeleaf 등 호스트의 SSR 도구와 기존 layout/fragment 구조를 재사용한다.
- HTML만으로 navigation·form 제출·기본 내용을 사용할 수 있게 한다.
- 이동은 href가 있는 a, 동작은 button, 입력은 label을 연결한 form control로 표현한다.
  form 안의 비제출 button에는 type="button"을 명시한다.
- 클릭 가능한 div나 JS로만 생성되는 기본 navigation을 만들지 않는다.
- JS 초기화가 성공한 뒤 강화 기능을 활성화한다.
  기본 내용을 CSS로 숨겨 놓고 JS 실패 시 영구히 접근 불가능하게 만들지 않는다.

## template 경계

- page shell은 layout, 재사용 UI는 fragment, 고유 내용은 page에 둔다.
- CSS·JS는 호스트 asset 구조에 배치하고 인라인 코드·inline event handler를 줄인다.
- 페이지 문구·title·설명은 메시지/설정 리소스로 관리하며 레포 고유 문자열을 복제하지 않는다.
- SSR 데이터는 escaped text·안전한 attribute·최소 data-* 계약으로 전달한다.
  Thymeleaf에서는 th:text를 기본으로 쓰고 th:utext에 사용자 값을 넣지 않는다.
- JS에서 사용자 값은 textContent로 갱신한다. innerHTML 문자열 조립으로 삽입하지 않는다.
- JS 객체 전달이 필요하면 도구의 안전한 serialization을 사용한다.
  문자열 이어 붙이기로 script 내부 데이터·selector·URL을 생성하지 않는다.
- SSR 조각 삽입은 호스트 도구의 검증된 경로를 따르고 신뢰되지 않은 HTML을 실행하지 않는다.
- CSRF·인증·validation은 서버 계약을 유지한다.
  JS validation은 편의 기능이며 서버 검증을 대체하지 않는다.

## 이벤트와 부분 화면

- 모듈 script 또는 호스트의 defer 초기화 방식으로 DOM 준비 뒤 초기화한다.
- querySelector/querySelectorAll로 명시적 data-* hook을 조회한다.
  스타일 class를 JS 계약으로 과도하게 공유하지 않는다.
- 동적 SSR fragment 교체는 안정적인 부모의 이벤트 위임으로 처리한다.
  부모까지 교체한다면 기존 listener를 해제하고 새 부모에 한 번만 등록한다.
- fetch/부분 갱신 실패 시 일반 form/navigation 또는 재시도 경로를 제공한다.
  사용자에게 중복 제출을 요구하거나 이미 처리된 mutation을 무조건 다시 실행하지 않는다.
- 로딩·성공·오류 상태를 텍스트로 알리고 필요한 경우 aria-live를 사용한다.
  focus·스크롤·기존 입력 값이 불필요하게 사라지지 않게 한다.
- aria-expanded/aria-controls와 실제 제어 대상의 표시 상태를 함께 갱신한다.
  SSR과 JS의 상태 계약을 중복된 전역 상태로 숨기지 않는다.

## 확인 예시

입력: 필터 form의 GET 제출을 부분 목록 갱신으로 강화한다.
출력: JS가 없으면 서버가 전체 페이지를 렌더링하고, JS가 있으면 같은 서버 계약으로
목록을 갱신한다. URL/뒤로가기·키보드·오류 복구도 사용자가 예측할 수 있게 유지한다.
