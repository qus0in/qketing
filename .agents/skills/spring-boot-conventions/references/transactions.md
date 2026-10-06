# 트랜잭션 경계

## 선언 위치와 기본값

- app의 use case/application service **구현체의 public 메서드**를 진입점으로 둔다.
- 영속 조회는 `@Transactional(readOnly = true)`를 명시한다.
- 영속 변경은 `@Transactional`을 명시하고 여러 repository 작업을 원자적으로 묶는다.
- 혼합 서비스는 메서드별 선언을 우선한다.
  조회 중심 서비스에만 클래스 readOnly 기본값과 변경 메서드 override를 검토한다.
- domain·port interface·controller에는 트랜잭션 정책을 두지 않는다.
- repository는 persistence adapter이다. 기본 repository 트랜잭션으로
  여러 호출을 묶는 application 경계를 대신하지 않는다.

## 작은 예시

아래는 import·port 구현을 생략한 app 구현체 예시다. 메서드별 경계를 보여준다.

```java
@Service
@RequiredArgsConstructor
public class ItemUseCase {
    private final ItemPort items;

    @Transactional(readOnly = true)
    public ItemResult find(long id) {
        return ItemResult.from(items.require(id));
    }

    @Transactional
    public void rename(long id, String name) {
        var item = items.require(id);
        item.rename(name);
        items.save(item);
    }
}
```

## 정합성과 외부 I/O

- readOnly는 의도·최적화 힌트이며 쓰기 방지나 격리·동시성 보장이 아니다.
- 트랜잭션은 영속 불변식을 지키는 최소 구간으로 유지한다.
  외부 HTTP/인증/AI 호출, 사용자 대기, 장시간 스트림을 포함하지 않는다.
- 캐시·큐·임시 보류만 처리할 때는 DB 트랜잭션을 열지 않는다.
- DB와 외부 API·Pub/Sub를 하나의 로컬 트랜잭션으로 원자 처리한다고 가정하지 않는다.
- 이벤트 발행은 DB commit 이후 처리한다. 실패 시 재시도·멱등성·재조정을 설계한다.
  전달 보장이 실제 요구라면 outbox 등을 검토하고 단순 commit 후 발행과 구분한다.
- propagation/isolation·잠금 정책은 실제 동시성 요구를 근거로 선택한다.
  readOnly나 단순 트랜잭션 선언만으로 경쟁 갱신을 해결했다고 간주하지 않는다.

## 프록시와 rollback

- 기본 프록시 모드에서 self-invocation은 호출 대상 메서드의 새 정책을 적용하지 않는다.
  필요한 경계는 별도 app bean의 public 메서드를 외부에서 호출하도록 분리한다.
- 직접 생성한 객체, private 메서드, 초기화 중 호출에 선언만 붙여 효과를 기대하지 않는다.
- RuntimeException 기반 custom exception은 기본 rollback 대상이다.
  호스트의 rollback 정책 override 유무도 확인한다.
- checked exception은 기본 정책에서 rollback되지 않으므로 필요한 경우만
  구체 예외를 `rollbackFor`로 지정한다.
- 트랜잭션 안에서 실패를 catch하고 정상 반환하면 rollback 의도가 사라질 수 있다.
  예외를 의미 있는 unchecked 타입으로 변환하거나 명시적인 복구 정책을 둔다.

## 공식 근거

프록시·선언·기본 rollback 동작은 호스트가 사용하는 버전의
[Spring 트랜잭션 문서](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)를 확인한다.
