# 리뷰 체크리스트와 검색

## 검토 순서

1. 호스트 지원 범위·SSR/asset 구조·현재 작업 범위를 확인한다.
2. var 없음, const 우선, let 재할당 근거, ES6+ 모듈·문법과 함수 책임을 검토한다.
3. DOM 조회 제한, null 처리, 동적 항목 위임과 listener 중복/해제를 확인한다.
4. flex/grid 선택, float 없음, position 허용·예외 근거와 8px scale을 점검한다.
5. 타이포 예외가 spacing에 번지지 않았는지, template escaping과 서버 계약을 확인한다.
6. JS 비활성·초기화 실패·부분 갱신·키보드·좁은 화면·zoom·긴 콘텐츠를 확인한다.
7. 파일 100줄·메서드 가능한 16줄을 확인하고 결과·제한·다음 필요한 작업을 보고한다.

## 검색 패턴

아래 placeholder를 직접 작성한 JS/CSS/template 경로로 바꾼다.
vendor·minified·생성 파일은 호스트 기준으로 제외하고 결과의 문맥을 읽는다.
rg를 우선하며 없는 환경에서는 grep -RnE로 같은 후보 패턴을 검색한다.

```bash
frontend_root='<frontend-source-dir>'
rg -n '\bvar\b|\blet\b' "$frontend_root" -g '*.js' -g '*.mjs' -g '*.html'
rg -n 'getElementById|getElementsBy[A-Za-z]*|document\.(forms|images|links)|\.closest\(' \
  "$frontend_root" -g '*.js' -g '*.mjs' -g '*.html'
rg -n 'addEventListener|removeEventListener|on(click|submit|change)\s*=' "$frontend_root"
rg -n 'position\s*:\s*(absolute|relative)|float\s*:' "$frontend_root"
rg -n '(gap|margin|padding|border|width|height)[A-Za-z-]*\s*:|[0-9]+(\.[0-9]+)?px' \
  "$frontend_root" -g '*.css' -g '*.html'
rg -n 'innerHTML|outerHTML|insertAdjacentHTML|th:utext|preventDefault' "$frontend_root"
```

grep가 필요한 환경의 예시:

```bash
grep -RnE 'getElementById|getElementsBy[A-Za-z]*|position:[[:space:]]*(absolute|relative)' \
  '<frontend-source-dir>'
```

- \b 지원 등 검색 엔진 차이를 고려한다. 주석·문자열·CSS var()도 후보에 포함될 수 있다.
- ES6+를 쓴다는 사실만으로 let이 필요하지는 않다. 실제 재할당을 확인한다.
- querySelector의 동적 문자열과 HTML 쓰기의 안전성을 별도로 확인한다.
- pixel 검색만으로 scale을 자동 판정하지 않는다. token·shorthand·calc를 펼쳐 확인한다.
- 모든 CSS 크기를 강제하지 않고 타이포·비율·content size의 허용 범위를 구분한다.
- position/float 검색에는 주석·vendor도 포함된다. 예외 이유와 국소 범위를 확인한다.
- 검색 결과가 없다는 이유만으로 합격하지 않는다. 동적 코드·template 생성·다른 확장자도 확인한다.

## 트러블슈팅

- 아이콘 클릭이 무반응 → target만 판정 → 허용 버튼의 contains로 대상 식별.
- 동적 항목이 동작하지 않음 → 초기 NodeList만 사용 → 안정적인 부모에 위임하고 이벤트 때 재조회.
- 클릭이 이중 실행됨 → fragment마다 재등록 → 부모의 lifecycle과 해제 명시.
- JS 실패 시 내용이 사라짐 → 초기 CSS로 숨김 → SSR 내용을 먼저 제공하고 성공 후 강화.
- 좁은 화면/zoom에서 CTA가 가려짐 → fixed 영역이 본문을 덮음 → flow·여백·scroll 재검토.
- root font 변경 시 spacing이 깨짐 → rem 환산 전제 불일치 → 8px 기준 token에 통일.
- grep 후보가 과다함 → vendor/문자열도 검색 → 자작 코드와 후보 문맥 구분.

## 회고와 검증

- 금지 API와 이벤트 위임을 함께 지키려면 대상 판별과 DOM 조회를 구분한다.
- layout·타이포 예외를 명시하고 규칙이 접근성을 해치지 않는지 확인한다.
- 재사용할 해결책은 이 reference, 레포 고유 값은 허용된 호스트 문서에 기록한다.
- 문서만 변경했다면 frontmatter·링크·100줄 제한을 검증한다.
  동작 미검증이나 빌드/네트워크 제약은 결과에 명시하고 무단 설치로 우회하지 않는다.
