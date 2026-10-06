# CSS layout과 spacing

## layout 선택

- 한 방향의 정렬·분배·wrap은 flex를 우선한다.
- 행·열을 함께 맞춰야 하는 2차원 구조는 grid를 사용한다.
- gap으로 항목 사이 간격을 표현하고 중복 margin으로 구조를 복잡하게 만들지 않는다.
- 콘텐츠의 의미·DOM 순서와 시각 순서를 맞추고 order로 키보드 순서를 뒤틀지 않는다.
- 좁은 화면과 긴 콘텐츠를 고려해 wrap·min-width·overflow를 검토한다.
  레이아웃을 맞추기 위해 텍스트·클릭 영역을 과도하게 축소하지 않는다.
- float는 사용하지 않는다. 정렬은 flex/grid로 해결한다.

## position 정책과 제한된 예외

- 기본 문서 흐름을 사용하고 불필요한 position 선언을 생략한다.
  필요한 선언은 sticky/fixed만 기본 허용한다.
- sticky는 scroll container·inset을 확인하고 fixed는 콘텐츠를 가리지 않게 한다.
  overlay의 focus 이동·닫기·원래 focus 복구는 기능 요구에 맞게 처리한다.
- absolute/relative는 일반 정렬·간격·열 구성에 사용하지 않는다.
- 예외 후보는 접근성용 visually-hidden/skip link, anchor에 붙는 장식·badge,
  문서 흐름 밖에 있어야 하는 overlay 등 flex/grid로 같은 의미를 표현하기 어려운 경우이다.
- 예외는 호스트가 허용하는 경우에만 최소 selector에 적용한다.
  이유·대체안이 불가능한 근거·영향 범위를 코드 주석 또는 허용된 기록에 남긴다.
- containing block이 필요한 relative도 예외에 포함해 검토한다.
  예외를 전체 page/container의 관행으로 확대하지 않는다.
- 예외를 쓰기 전 기본 flow·sticky/fixed와 의미 있는 HTML로 해결할 수 있는지 확인한다.

## 8px spacing scale

- 기준 scale은 0, 8, 16, 24, 32, 40, 48px처럼 8px 배수이다.
  음수 margin이 실제 필요하면 역시 8px 배수이며 근거를 남긴다.
- 적용 대상: gap/row-gap/column-gap, margin·padding(방향별 속성 포함),
  고정 레이아웃 width/height·min/max 크기는 scale token을 사용한다.
  border-radius의 기존 scale token 규칙은 유지한다.
- 비대상: border-width(방향별 두께·border shorthand의 두께 포함), outline 두께,
  font-size·line-height·letter-spacing 등 타이포는 8px 배수를 강제하지 않는다.
- border 두께는 1px·2px 등 디자인 판단을 허용한다. 구분선·상태 강조·대비 등 선택 근거를
  코드 주석이나 리뷰에 남긴다. 얇은 border 사용에 별도 spacing 예외 승인을 요구하지 않는다.
- outline 두께는 focus 식별과 대비를 기준으로 정한다. border·outline의 두께 선택이
  주변 margin/padding이나 버튼·클릭 영역의 spacing 규칙을 면제하지 않는다.
- auto·비율·fr·%·콘텐츠 기반 크기는 반응형 관계를 표현할 때 사용한다.
  calc/clamp 안의 고정 spacing/크기 항은 scale을 따르고 임의 pixel 값을 숨기지 않는다.
- 고정 root font 크기를 가정한 rem spacing은 사용하지 않는다.
  호스트 token이 8px 기준임을 보장하면 기존 단위 체계를 재사용한다.

```css
:root {
  --space-1: 8px;
  --space-2: 16px;
  --space-3: 24px;
  --space-6: 48px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding: var(--space-3);
}
```

## 타이포와 접근성

- font-size·line-height·letter-spacing은 가독성을 위해 spacing scale에서 제외한다.
  글자의 margin/padding·버튼 크기까지 타이포 예외로 확대하지 않는다.
- 본문은 16px 이상을 기준으로 호스트 시인성 요구를 따르고 충분한 line-height를 둔다.
- 키보드 focus·명도 대비·터치 영역을 유지하고 상태를 색상만으로 전달하지 않는다.
- 축소 화면·긴 번역·200% zoom에서 fixed/sticky가 콘텐츠와 CTA를 가리지 않는지 확인한다.
