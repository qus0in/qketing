# 오케스트레이션 구성 (qketing)

방법은 `.agents/skills/herdr-orchestration` 스킬을 따른다. 여기에는 이 레포의 현재 구성과 운영 기록만 남긴다.

## 현재 구성 (2026-10-06 재배정 3회 후 실측, workspace `wW`)

| 탭 라벨 | tab | pane | 에이전트 | 모델·effort(화면 표기) | 티어·역할 |
| - | - | - | - | - | - |
| orchestrator | wW:t1 | wW:p1 | Codex | GPT-6 (세션 지시 기준, effort 미확인) | 분해·배정·빌드 검증·통합·git·최종 리뷰 |
| worker1 | wW:t6 | wW:p6 | OpenCode | MiMo-V2.6-Flash · OpenCode Go (effort 표시 없음) | 하: 요약·정리·단순 수정 |
| worker2 | wW:t5 | wW:p5 | OpenCode | Muse Spark 1.3 Contributor · OpenCode Go (effort 표시 없음) | 하: 요약·정리·단순 수정 |
| worker3 | wW:t3 | wW:p3 | OpenCode | GPT-6 Luna · OpenCode Go · max | 상: 주 구현, worker 산출물 교차 리뷰 |
| worker4 | wW:t4 | wW:p4 | OpenCode | LongCat 2.5 Preview Free · OpenCode Go · high | 중: 명확한 구현·테스트·문서 |

- 근거: 2026-10-06 orchestrator 화면 실측 (사용자 요청)
- 워커 4개 모두 OpenCode, 이름 없음 → 지목은 탭 라벨(herdr-call이 현재 workspace 우선 해석, 다른 workspace wX에도 worker1·2 라벨 있음)
- 티어: 상 1개(worker3) = 주 구현, 중 1개(worker4) = 명확한 구현·테스트·문서, 하 2개(worker1, worker2) = 요약·정리·단순 수정
- 리뷰: 최종은 orchestrator, worker 산출물 교차 리뷰는 worker3(상)
- 호출은 `herdr-call.sh`로 탭 라벨(worker1~4) 또는 tab id 사용, 이름 의존 금지
- 이름은 에이전트가 재시작되면 해제된다. 세션을 시작할 때 `discover-workers.sh`로 확인하고 다시 붙인다.
- 모델이 바뀌면 이 표를 갱신한다.
- 2026-10-06 사용자 지시: orchestrator는 직접 하기 전에 worker에게 먼저 위임하는 것이 기본값이다.

## worker 호출

- 호출은 `.agents/skills/herdr-orchestration/scripts/herdr-call.sh`로만 한다 (`status`/`resolve`/`send`/`wait`/`read`).
- 에이전트 이름에 의존하지 않고 tab 라벨 또는 tab/pane id를 쓴다. 이름은 재시작 시 유실된다 (wW:p5 반복 유실).
- tab과 pane 번호가 다를 수 있으니 `resolve`로 확인한다.

| 탭 라벨 | 이름 (참고용) | tab/pane |
| - | - | - |
| `orchestrator` | orchestrator | wW:t1/p1 |
| `worker1` | (이름 없음) | wW:t6/p6 |
| `worker2` | (이름 없음) | wW:t5/p5 |
| `worker3` | (이름 없음) | wW:t3/p3 |
| `worker4` | (이름 없음) | wW:t4/p4 |

## 이 레포 규칙과의 연결 (AGENTS.md)

- 작업 단위는 10분 이내. 넘으면 worker에게 분산하고 Orchestrator는 붙잡지 않는다.
- 작업이 끝나면 진척도와 이후 작업을 브리핑한다.
- 커밋은 사용자가 요청할 때만, Orchestrator만 한다. worker는 git 쓰기 금지.
- 모든 파일 100줄 이하. 위임 프롬프트의 제약에도 이 규칙을 넣는다.

## 외부 호출

- hermes 등은 `herdr agent prompt orchestrator "[from:hermes reply:issue] ..." --wait`로 요청한다.
- 결과는 마지막 줄 `RESULT: ...`와 GitHub 이슈(`plan`/`work`/`report`)로 돌려준다.

## 운영 기록

- 2026-10-06 역할 재확인: Codex `wW:p1`을 `orchestrator`로 rename하고 응답에서 확인. `pane current --current`는 OpenCode `wW:p5`를 반환하여 agent list의 종류·cwd·대화 제목으로 본인을 식별했다. worker 모델 표는 이번에 재검증하지 않았다.
- 2026-10-05 dry-run (#6), 읽기 전용 작업 3개를 동시에 위임
  - codex-1 (리뷰, 상): 실제 버그 2건을 찾음 (pane 이동 후 환경변수가 예전 값으로 남음, `LC_ALL=C`에서 유니코드 깨짐). 샌드박스가 herdr 소켓을 막아 BLOCKED로 보고
  - opencode-1 (정합성 점검, 중): 1분 21초. 문서 모순 7건 중 유효 4건 (스킬 목록 누락, 커밋 규칙 충돌 등)
  - opencode-2 (요약, 하): 42초. 정확하게 요약. effort는 확인 불가
  - 결론: 티어 판단이 맞았다. 하 티어도 사실 조회는 정확하다
- 2026-10-06 실전 배분 (#18, 스킬 2개·문서 2개). 사용자 확인 서열: Sol 6.1 > DeepSeek > MiMo Flash
  - codex-1: 스킬 작성 2건 + 리뷰 반영, 각 3분 안팎으로 validate 통과. 설계가 걸린 작성은 codex에 몰아준다
  - opencode-1: roadmap 문서(17초), 스킬 리뷰에서 유효 누락 4건. 리뷰어로 쓸 만하다
  - opencode-2: 이슈 8개 사실 추출(2분 24초), 원문과 일치
  - 문제: OpenCode가 새 session을 만들 때마다 이름이 해제되어 `agent wait`이 즉시 실패함. 긴 보고는 화면에서 잘림
- 2026-10-06 0.1.0 Bootstrap (#22), codex-2 구현 · codex-1 서브 헤드 리뷰 · OpenCode 병렬
  - codex-2(Luna): 골격·오류 처리 구현. 샌드박스라 빌드 불가 → orchestrator가 빌드하며 BOM 누락 1건 수정
  - codex-1(Sol): 리뷰 2회, 실제 결함(CI 줄 수 검사에 wrapper 걸림, 405 Allow 누락, advice 순서) 지적. 서브 헤드로 적합
  - opencode-1: 템플릿·색상 수정, 범위 밖 404 JSON 문제를 먼저 발견. opencode-2: Dockerfile 한 번에 통과
  - 충돌: opencode-1이 메시지에 접두사를 넣자 codex-2 테스트 실패 → 공유 파일 의미까지 계약에 적기
- 2026-10-06 0.1.0 릴리스 (#23 #26, tag v0.1.0). macOS uptime 50일 TCP 버그로 재부팅 → herdr 재시작, codex pane 부재
  - opencode-1이 재시작 후 `/tmp` 권한 요청으로 blocked → 0.2.0 이슈(#27 #28)는 orchestrator가 직접 작성
  - hermes(Discord) 요청은 사람 지시로 처리. 승인 요청은 핵심 3개 + 표/mermaid
- 2026-10-06 0.2.0 착수 (#27): opencode-2가 migration을 실제 PG로 자가 검증, codex-2 entity·QueryDSL allow-list, Sol 검수
- 2026-10-06 codex-2 이름 3회 유실, 이름으로 보낸 T27-9 유실 → herdr-call 도입 (Sol 작성, orchestrator 실소켓 검증에서 `--` 미지원 버그 발견·수정)
- 2026-10-06 0.3.0 16 최종 리뷰는 agy-1이 대행(승인)
- 2026-10-06 worker1: agy-1(Sonnet 5.5 medium) → OpenCode Space Bunny Free max로 교체, 진행 중이던 T40-7 유실 후 재배정
- 2026-10-06 재배정: codex(Sol·Luna)·agy(Sonnet) 퇴장 → 워커 4개 OpenCode. T40-9(seat hold)는 Luna→Muse Spark로 이어짐
- 2026-10-06 0.4.0 (#40): worker 4개 모두 OpenCode. 재배정 중 worker1의 T40-7 유실 → 재배정, worker2(Luna→Muse Spark) T40-9 seat hold 정상 완료
- worker3이 다른 보고서의 'T40-10 제안'을 따라 범위 밖 hold·확정 연동 구현 → #11 설계와 일치해 유지, 대신 adapter 수준 DB 경합 테스트 추가
- 동시 Gradle 실행으로 test-results 충돌, worker1 130줄 파일 → 재지시. 다른 workspace(wX)와 탭 라벨 중복 → herdr-call이 현재 workspace 우선 해석
- 최종 리뷰(orchestrator): Valkey 키 외부 식별자 검증 추가, session 키 문서 정정
- 2026-10-06 재배정(3회): worker1 Space Bunny→MiMo Flash, worker3 DeepSeek→GPT-6 Luna max, worker4 MiMo→LongCat high
