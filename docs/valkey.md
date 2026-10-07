# Valkey 구조 (qketing, 0.4.0)

범용 규칙은 spring-boot-conventions 스킬, 결정은 #40 #41

| 영역 | 키 형태 (공통 prefix `qticket`) | Hash tag |
|---|---|---|
| cache | `qticket:cache:performance:<performanceId>:<resource>:<id>` | 없음. 독립 read cache key |
| queue | `qticket:queue:{<performanceId>}:active` / `:waiting` | `{performanceId}`. admission Lua가 active·waiting·session 3키를 함께 접근 |
| session | `qticket:queue:{<performanceId>}:active:<memberId>` | `{performanceId}`. queue Lua가 3키를 함께 다루므로 같은 slot에서 원자 실행 — cluster CROSSSLOT 방지 |
| hold | `qticket:hold:{<performanceId>}:<seatId>` | `{performanceId}`. queue admission Lua가 함께 접근 |

- 키 이름은 `qticket:<영역>:<리소스>:<식별자>` 순서로 두고 tenant/user 입력 문자열을 키 경로에 직접 합치지 않는다. `{performanceId}` hash tag는 #11의 performance 단위 queue/seat Lua가 Valkey Cluster에서 같은 slot으로 원자 실행되게 하려는 제안이다(현재 standalone Testcontainers 구성에서는 slot 분배가 적용되지 않는다). cache는 multi-key Lua 경로가 아니므로 tag를 강제하지 않는다
- 외부 식별자(memberId·holderId)는 `[A-Za-z0-9._-]{1,64}`로 검증한 뒤 키에 사용한다
- 설정값은 `@ConfigurationProperties` `qticket.valkey`에 둔다(개발 기본: queue.capacity 100, session.ttl 5m, hold.ttl 5m, 코드 상수 금지, #41 결정 2A). 테스트는 공유 Testcontainers Valkey `@ServiceConnection(name = "redis")`를 쓴다(#41 결정 1A). Valkey는 source of truth가 아니라 TTL 만료 시 DB에서 다시 읽는다

## TTL 정책

| 대상 | 부여 방식 | 기본값 | 만료 판정 |
|---|---|---|---|
| session `qticket:queue:{pid}:active:<memberId>` | ACTIVE 확정 시 `SET PX` | `qticket.valkey.session.ttl` 5m | 개인키 만료 즉시 |
| active ZSET score | 확정 시 `score = 확정시각 + session.ttl` | 동일 | `ZREMRANGEBYSCORE -inf now`(모든 admit의 첫 행, 즉시) + 세션 키 소실분 `reconcileActive()`(capacity 초과 시에만) |
| waiting ZSET | WAITING 경로 admit마다 `PEXPIRE waitingKey session.ttl`(집합 전체) | 동일 | 아래 "개별 score vs 집합 PEXPIRE" |
| hold `qticket:hold:{pid}:<seatId>` | `SET NX PX` | `qticket.valkey.hold.ttl` 5m | 키 만료 → snapshot에서 `held=false, holdTtlMillis=0` |
| cache | 명시 TTL | `qticket.cache.performance-ttl` 5m | miss 시 DB 재조회 |

### waiting: 개별 score 만료와 집합 PEXPIRE의 차이

- **개별 score 만료(논리 만료)**: waiting score는 enqueue 시각(`nowMillis`)이고, `ZREMRANGEBYSCORE waiting -inf (now - session.ttl)`가 **자기 enqueue로부터 session.ttl이 지난 member만** 제거한다.
  - 실행 조건이 **WAITING 경로의 admit에서만**이다(capacity 미달로 ACTIVE 확정되는 admit은 스크립트가 즉시 return해 waiting 정리를 건너뛴다 → `queue-admission.lua:48-57`). 정리 시점은 늦은 정리이며, 대기 발생이 멈추면 만료 판정이 나도 key에는 남는다.
  - score는 `ZADD NX`라 중복 admit에도 갱신되지 않는다 → 자신의 체류 시간은 첫 시도 시각 기준이며, 다른 member의 admit이 이를 연장하지 않는다.
- **집합 PEXPIRE(물리 만료)**: 같은 WAITING 경로 admit이 `PEXPIRE waiting session.ttl`로 **waiting 키 전체** TTL을 매번 갱신한다.
  - 마지막 **WAITING 경로 admit**으로부터 session.ttl 동안 접근이 없으면 waiting ZSET이 통째로 만료 → 남은 대기자 정보가 한 번에 소실된다(재 admit 시점에 다시 등록). capacity 미달 admit만으로는 갱신되지 않는다.
  - WAITING 경로 admit이 이어지는 동안에는 집합 TTL이 갱신되므로 개별 정리는 오직 score 방식이 담당한다.
- 정리: 집합 TTL은 "무접속 통째 소실" 경로, score 만료는 "WAITING admit 시에만 실행되는 늦은 정리"이고 session.ttl이 둘 다의 기준값이다.

### session TTL 정책

- ACTIVE 확정 시에만 `SET PX session.ttl`과 active score를 부여한다. **세션 생존 중인 재 admit은 TTL을 연장하지 않는다**(`PTTL>0` 조기 반환 → 잔여 시간 유지).
- 세션 키가 만료되면 active에서도 제거되고, capacity가 허용하는 다음 admit에서 재확정되며 새 TTL을 받는다.
- 기본값은 `application.yml`의 `qticket.valkey.*`(capacity 100, session 5m, hold 5m)와 `qticket.cache.performance-ttl` 5m이며 코드 상수 금지(#41 결정 2A)다. 운영 조정은 환경 재정의로만 한다.

운영 관측 지점·명령·로그·실행 담당은 `docs/realtime-operations.md`에 있다.

## 변경 이력

- 2026-10-06: 최초 작성 (architecture.md에서 분리, #40)
- 2026-10-07: TTL 정책(waiting 개별 score vs 집합 PEXPIRE, session 미연장) 추가 (T47-28)
- 2026-10-07: waiting 정리·PEXPIRE는 WAITING 경로 admit에서만 실행됨을 정정 (T47-33 리뷰)
