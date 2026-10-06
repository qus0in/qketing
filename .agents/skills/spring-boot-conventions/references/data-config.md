# DTO·설정·상수

## 타입과 변환

- DTO/VO는 호스트 Java 버전이 지원하면 record를 우선한다.
- record의 불변성은 얕다. List/Map 등 가변 입력은 필요한 경우 방어적 복사한다.
- JPA entity는 record 대신 일반 class를 사용하며 영속 계약에 맞는 생성자를 둔다.
- UI 요청/응답 DTO와 app 결과·domain 모델을 구분한다.
  request binding·HTTP validation annotation은 ui에 둔다.
- 별도 Mapper 계층/MapStruct를 추가하지 않는다.
  `from(...)`, `of(...)`, `toXxx()`를 데이터 변환 책임이 있는 계층에 둔다.
- 바깥 계층 타입을 안쪽 계층의 팩토리 인자로 받아 의존 방향을 뒤집지 않는다.
  예: domain이 UI DTO를 받는 대신 ui/app에서 domain 생성에 필요한 값을 전달한다.
- 변환 메서드는 변환만 수행하고 repository 조회·외부 호출을 숨기지 않는다.
- JPA entity를 응답에 직접 반환하지 않고 필요한 DTO를 app 경계 안에서 구성한다.
  view 렌더링 중 lazy loading이나 우발적 추가 조회를 방지한다.

```java
public record ItemResponse(long id, String name) {
    public static ItemResponse from(ItemResult result) {
        return new ItemResponse(result.id(), result.name());
    }
}
```

## Lombok과 주입

- field injection 대신 생성자 주입을 사용하고 의존 필드는 final로 둔다.
- Lombok이 이미 있으면 `@RequiredArgsConstructor`를 검토한다.
  없으면 명시적인 생성자를 쓰며 이를 위해 의존성을 추가하지 않는다.
- `@Data`를 피하고 필요한 `@Getter`, 제한적 `@Setter`, `@Builder`만 선택한다.
- entity의 equals/hashCode/toString은 식별자·lazy 관계·순환 참조를 고려한다.
  자동 생성으로 영속 계약이나 민감정보 노출을 만들지 않는다.
- 목적에 맞는 생성자 annotation만 사용하고 불필요한 전체 setter를 만들지 않는다.
- 로깅에는 필요 시 `@Slf4j`를 사용하며 민감 값은 기록하지 않는다.

## 설정값과 상수의 소유자

| 값의 성격 | 관리 위치 |
| --- | --- |
| 환경·운영에 따라 변경되는 값 | application 설정/profile/env + ConfigurationProperties |
| 독립적인 단일 설정값 | 필요한 경우에만 Value |
| 코드 불변 규칙·상수 | 소유 feature/layer의 static final 또는 enum/VO |
| UI label·title·설명 | 호스트의 메시지·UI 설정 리소스 |

- TTL·capacity·feature flag·외부 endpoint·모델명은 변경 가능한 설정이다.
- 관련 값은 `@ConfigurationProperties(prefix = "<feature-prefix>")`로 묶는다.
  Duration 등 의미 있는 타입과 `@Validated`/제약으로 잘못된 설정을 시작 시 검출한다.
- 바인딩 타입은 record를 검토하고 호스트 버전에 맞는 scan 또는 등록 방식을 사용한다.
- 환경별 profile과 환경변수로 덮어쓰며 시크릿을 코드·기본 설정에 넣지 않는다.
- 설정 bean은 해당 adapter/feature가 소유하고 domain에는 필요한 값만 전달한다.
- 전역 Constants 클래스나 common/util에 무관한 값을 쌓지 않는다.
- 숫자·문자열 magic value를 controller/service/template에 반복하지 않는다.
  실제 불변 값까지 설정으로 만들며 복잡도를 늘리지 않는다.
