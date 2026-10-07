# Realtime 운영 관측 (0.5.0)

설계는 `docs/realtime.md`, 키·TTL 계약은 `docs/valkey.md`. 이 문서는 운영에서 실제로 보는 지점만 다룬다.

## 1. Valkey 키·채널

| 구분 | 패턴 | 관측 명령 예시 | 보는 것 |
|---|---|---|---|
| active 집합 | `qticket:queue:{<pid>}:active` | `ZCARD` · `ZSCORE <member>` | 동시 활성 수(capacity 100 대비), member별 만료 시각 |
| waiting 집합 | `qticket:queue:{<pid>}:waiting` | `ZCARD` · `ZRANGE 0 -1 WITHSCORES` · `PTTL` | 대기 인원, enqueue 시각(score), 집합 남은 TTL |
| session 개인 | `qticket:queue:{<pid>}:active:<memberId>` | `PTTL` | 세션 잔여 시간(기본 5m, 재 admit 미연장) |
| hold | `qticket:hold:{<pid>}:<seatId>` | `PTTL` · `EXISTS` | 좌석 hold 잔여(키 없음/`<=0` = 미보유) |
| cache | `qticket:cache:performance:<pid>:*` | `SCAN MATCH` | 공연 read cache 잔여(기본 5m) |
| seat 채널 | `qticket:realtime:{<pid>}` (PSUB `qticket:realtime:*`) | `PUBSUB NUMPAT` · publish→수신 probe · 앱 로그 | PSUB 패턴 구독 존재와 실제 fan-out 수신 |
| queue 채널 | `qticket:queue-events:{<pid>}` (PSUB `qticket:queue-events:*`) | `PUBSUB NUMPAT` · publish→수신 probe · 앱 로그 | PSUB 패턴 구독 존재와 admission 전이 수신 |

- `PUBSUB CHANNELS`는 패턴 구독(PSUB)을 보여주지 못한다. PSUB 확인은 `PUBSUB NUMPAT`(서버 전체 패턴 수), 실제 publish→수신 probe, 앱 로그(`Ignoring ...` 없음)로 한다.

## 2. 설정값 위치 (코드 상수 금지)

- `qticket.valkey.queue.capacity` 100 · `qticket.valkey.session.ttl` 5m · `qticket.valkey.hold.ttl` 5m
- `qticket.cache.performance-ttl` 5m
- `qticket.realtime.queue-stream.heartbeat-interval` 15s · `refresh-interval` 3s · `timeout` 30m
- `qticket.realtime.seat-websocket.send-time-limit` 5s · `buffer-size-limit` 262144 · `pending-limit` 256
- Pub/Sub 실행기(`ValkeyRealtimeConfiguration` **코드 상수** → 환경 재정의 불가): message core 2 / max 8 / queue 512(포화 drop+warn) · subscription core 1 / max 2 / queue 16(AbortPolicy)
- seat-websocket 신규 props **확정**: `send-hold-limit` **7s**(실제 Tomcat native `BLOCKING_SEND_TIMEOUT`에 Long으로 설정) · `resync-interval` **15s**(per-connection CAS 중복 방지).

## 3. 엔드포인트·로그 관측 지점

- HTTP는 `GET /actuator/health`만 노출(기본값, metrics 미노출) → 수치는 위 키/채널 명령으로 본다.
- fan-out decode·채널 불일치: `QueueRealtimeListener`·`ValkeySeatRealtimeListener`의 `Ignoring ...` 계열 warn 6종씩
- WS: `Seat WebSocket frame send failed`, `Seat snapshot failed performanceId=`
- SSE: `Queue stream broadcast task failed`, `Queue snapshot refresh failed performanceId=`, `Queue realtime bridge failed performanceId=`

## 4. 만료·유실 시나리오 (먼저 확인할 것)

1. waiting 집합 TTL 만료: 마지막 **WAITING 경로 admit**(대기 발생 또는 대기 중 재시도) 후 session.ttl 동안 접근 없음 → waiting 전체 소실. capacity 미달인 ACTIVE 확정 admit만으로는 waiting TTL이 갱신되지 않는다. `ZCARD waiting`가 0인데 클라이언트는 대기 중으로 아는 상태가 이 경로다(자세한 차이는 `docs/valkey.md`).
2. session 만료: 개인 `PTTL` 0 → active에서 제거, capacity가 허용하는 다음 admit에서 재확정. 재 admit이 TTL을 연장하지 않으므로 장시간 활성은 재확정마다 새로 부여된다.
3. hold 만료: 좌석 snapshot이 즉시 `held=false, holdTtlMillis=0`으로 반영한다. 원장은 PostgreSQL이며 Valkey는 source of truth가 아니다.

## 5. 테스트 실행과 결과 기록

재현 가능한 일반 명령(JDK 17 + Docker daemon 필요). 실행 시 결과 기록 표에 남긴다.

```bash
./gradlew build --no-daemon                                          # 전체 (compile + test)
./gradlew test --no-daemon --tests "kr.noco.qticket.realtime.integration.*"
./gradlew test --no-daemon --tests "*SnapshotReadAdapterIntegrationTest"
```

- 공유 인프라 `kr.noco.qticket.realtime.support.SharedRealtimeInfrastructure`: PG 1세트 + Valkey 1세트.
- `close()`는 Valkey → PG 순서로 시도하되, `valkey.stop()`이 예외여도 **finally로 `postgres.stop()`과 instance 해제를 시도한다**(`InfraStopGuard.stop`). 첫 예외는 원본 그대로 던지고 PG 예외까지 나면 suppressed로 붙인다. 실제로 멈추지 못한 컨테이너(정지 자체가 실패한 경우)만 Testcontainers Ryuk 또는 수동 삭제로 회수한다.

### 결과 기록 (최종: T47-56, 2026-10-07 11:18 KST)

| 실행 시각(KST) | 명령 | exit | tests/failures/errors/skipped | 근거 |
|---|---|---|---|---|
| 11:18 | targeted(realtime) | 0 | 31/0/0/0 (23 suites, 2-node 11 전부 pass) | T47-56 |
| 11:18 | `./gradlew build --no-daemon` | 0 | 169/0/0/0 (72 suites) | T47-56 |
| 11:18 | `./gradlew e2eTest` (로컬) | 0 | 5/0/0/0 | T47-56 |
| 11:18 | 홈/404 스크린샷 | - | 2장 | `build/reports/e2e/*.png` |
| (이전 기준) 10:24-10:26 | T47-39: integration 5/5 · build 153/0/0/0 (60 suites) · e2e 5/0/0/0 | 0 | 0 fail | 이전 gate |

- **이전 기준(T47-39)**: full build exit 0, 60 suites·153 tests·0 fail (이번 최종은 위 표 169 / 31 / 5).
- **TCP 실측**: `CLIENT KILL` **killed=4 → 자동 재연결 후 새 JSON event 수신(ports 51622/51629)**, 수동 start 없음.
- **실패 이력(수정 후 회복)**: T47-52 부팅 11 fail → `@ConstructorBinding`(canonical) / T47-54 raw `CLIENT KILL` decode 1 fail → typed helper / T47-56 cast·import 컴파일 시도 실패 후 fix → 최종 pass(시도 기록은 T47-56 report, 시도별 로그 덮어쓰기 주의).
- watchdog/resync 근거는 unit(fake)·설정 적용 실측까지이며 실제 slow socket 실측은 §6-1의 known limit으로 분리한다.
- **이전 회차(T47-37)**: integration 5건 중 1건 failure(`java.lang.IllegalStateException: SSE status 403`, `SseStream.open`) → 같은 코드·같은 `localhost` 주소로 단건(10:24)·전체(10:25) 재실행 시 통과. **원인은 미확정·미재현이며 해결로 단정하지 않는다.** `SseStream`은 non-200 body를 `readNBytes(4096)`으로 **최대 4096바이트** 보존하도록 보정했고 `localhost` 주소는 유지했다.

## 6. 운영 잔여 점검 (수정 반영 상태 · known limit · 미검증)

1. **WS 전송 한도·주기 복구 (수정 반영 상태 + known limit)**: 한도는 3겹 — decorator `sendTimeLimit` 5s(경합 send의 버퍼 flush 검사) / **Tomcat native `BLOCKING_SEND_TIMEOUT` = `send-hold-limit` 7s(실제 BasicRemote write 상한, `SeatWebSocketNativeTuning`이 Long으로 설정)** / **watchdog 7s raw close(감시 스레드와 분리된 별도 close pool `seat-ws-close-` 1/2/64)**. **주기 snapshot 복구는 `resync-interval` 15s로 동작한다**: per-connection `CAS`가 in-flight를 선점해 중복을 막고, 정상·실패·거부(resync executor 거부 포함) 모든 경로에서 `resyncCompleted()`로 reset하며, `stop()`은 sweep 정지 → 열린 연결 raw close → resync/close 실행기 종료 순서다. **known limit: 실제 느린 소켓이 7s에 중단되는지는 미측정**(unit fake raw close·property 왕복·binder 부팅 실측과 구분).
2. **WS 주기 heartbeat 없음**: 서버가 주기적으로 보내는 WS 프레임은 없다. 클라이언트 `PING`에 대한 `PONG`만 존재한다(SSE의 `heartbeat` 15s와 혼동 금지).
3. **포화는 두 경로가 다르다 (복구 방식 구분)**:
   - **message executor 포화 (core 2 / max 8 / queue 512)**: `RealtimeRejectedExecutionHandler`가 이벤트를 drop하고 `Realtime Pub/Sub dispatch saturated` warn만 남긴다. **WS 연결 자동 close는 없다** → watchdog의 **15s 주기 resync가 자동으로 authoritative `SNAPSHOT`을 보내 재연결 없이 복구**한다(`SeatAutoResyncTest`: 1s 설정으로 검증, 기본 15s). 명시적 `SNAPSHOT` 재조회·재연결은 **추가 선택지**다.
   - **decorator `bufferSizeLimit` 초과(OverflowStrategy.TERMINATE) / snapshot 전 `pendingLimit`(256) 초과**: **연결을 종료**한다 → 재연결로 복구.
4. **자동 재구독은 검증 완료 / subscription 포화 후 회복·튜닝은 별도 미측정**: TCP `CLIENT KILL`(4 연결) 절단 후 **자동 재구독은 실제 검증 완료**(killed=4 → 재연결 후 새 JSON event 수신). 별도로 subscription executor(core 1 / max 2 / queue 16)는 `ThreadPoolExecutor.AbortPolicy`로 **거부를 명시적 예외로 올리며, 거부(포화) 후 recovery 동작과 실부하 튜닝(큐 깊이·거절율)은 미측정**이다(message executor는 drop, 위 3항과 구분).
5. **SSE 403 원인 미확정 유지**: T47-37 1건은 재실행 통과(transient). 최종 재현 시도(T47-56: 동일 localhost, ACTIVE/ABSENT 동시 2 stream + reconnect, 10 round 30 opens)에서 **비200 0건 → 미재현** → 원인 미확정·미해결로 유지한다.
6. **SSE 느린 클라이언트 backpressure·실부하 튜닝 미측정**: `SseConnectionManager`의 `emit`/`registerWithSnapshot`은 `synchronized(connection)` 안에서 `SseEmitter.send`를 호출하므로, 느린 구독자에서 send가 블로킹되면 단일 스레드 `QueueStreamBroadcaster`가 전체 전달이 지연될 수 있다(blocker 결함이 아닌 known limit, 실부하 측정 미수행).
