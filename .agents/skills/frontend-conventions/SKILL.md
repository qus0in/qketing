---
name: frontend-conventions
description: JavaScript·CSS·SSR 템플릿의 작성과 리뷰에 일관된 프런트엔드 규칙을 적용한다. 브라우저 코드 구현·리팩터링·코드 리뷰에서 사용한다. "const 우선", "querySelector", "이벤트 위임", "flex/grid", "8px spacing", "progressive enhancement" 같은 요청에 사용.
metadata:
  version: "1.0"
---

# 프런트엔드 컨벤션

의미 있는 HTML과 작은 브라우저 코드를 우선하고 SSR 기본 동작을 점진적으로 강화한다.
레포 이름·패키지·제품 버전·asset 경로는 고정하지 않고 호스트 규칙을 확인한다.

## 절차

1. 호스트 규칙·지원 브라우저·기존 SSR/asset 구조를 확인한다.
   의존성·빌드 도구·프레임워크를 현재 요구 없이 추가하지 않는다.
2. [JavaScript](references/javascript.md)에 따라 const·ES6+·제한된 DOM 조회를 사용한다.
   이벤트는 필요한 공통 부모에 위임하고 초기화·해제 책임을 명시한다.
3. [CSS](references/css.md)에 따라 flex 우선, 2차원 grid, 8px spacing을 적용한다.
   position과 타이포 예외를 일반 layout 규칙과 구분한다.
4. [SSR과 점진적 강화](references/ssr.md)에 따라 HTML의 기본 navigation/form을 유지한다.
   template escaping과 접근성을 보존하고 JS 실패에도 기본 동작이 남게 한다.
5. [리뷰 체크리스트](references/review.md)의 검색과 동작 검증을 수행한다.
   파일 100줄 이하·메서드 가능한 16줄 이하를 확인하고 결과·남은 작업을 보고한다.

## 핵심 규칙

- JS에서 var를 금지한다. 재할당하지 않는 binding은 const, 필요한 재할당만 let이다.
- 모듈·화살표·구조분해·템플릿 리터럴·optional chaining 등 ES6+ 문법을 우선한다.
- DOM 조회는 querySelector/querySelectorAll만 사용한다.
  getElementById/getElementsBy* 등 다른 조회 API를 사용하지 않는다.
- layout은 flex 우선, 행·열을 함께 다루는 2차원 구조는 grid이다.
- position 선언은 sticky/fixed만 기본 허용하고 absolute/relative는 제한된 예외로 다룬다.
- float를 금지하고 gap·margin·padding·border·고정 크기는 8px 배수 scale을 사용한다.
- SSR이 기본 기능을 제공하고 JS는 그 기능을 강화한다.

## 입출력 예시

입력: SSR 목록에 필터 form과 동적으로 추가되는 항목의 버튼 구현 요청.
출력: 의미 있는 form/링크, 공통 부모의 위임 이벤트, const 기반 모듈,
flex/grid와 spacing token을 사용한 CSS, JS 비활성·키보드 검증 결과.

## 엣지 케이스

- 브라우저 지원 범위를 넘어선 문법은 호스트의 기존 변환 수단이나 호환 구현을 따른다.
  호환성만을 이유로 var나 금지한 DOM API를 다시 도입하지 않는다.
- 외부/vendor 코드는 직접 작성한 코드와 구분해 검토한다. 무단으로 복제·수정하지 않는다.
- 규칙 충돌이나 필요한 예외는 근거·영향·범위를 명시하고 호스트 정책을 따른다.
- 회고의 범용 해결책은 reference에 반영하며 레포 고유 사실은 허용된 호스트 문서에 기록한다.
  현재 작업 범위 밖 문서는 수정하지 않는다.
