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

## 변경 이력

- 2026-10-06: 최초 작성 (architecture.md에서 분리, #40)
