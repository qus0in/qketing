# 오케스트레이션 구성 (qketing)

방법은 `.agents/skills/herdr-orchestration` 스킬을 따른다. 여기에는 이 레포의 현재 구성과 운영 기록만 남긴다.

## 현재 구성 (2026-10-06 확인, workspace `wW`)

| 이름 | pane | 에이전트 | 모델 · effort | 티어 | 주 업무 |
| - | - | - | - | - | - |
| `orchestrator` | wW:p1 | Claude Code | Opus 5.5 | - | 분해, 배정, 검증, git, 보고 |
| `codex-1` | wW:p2 | Codex 0.160 | GPT-6.1-Sol · medium | 상+ (서브 헤드) | 코드 리뷰, 설계 판단, 하위 작업 분해·검수 |
| `codex-2` | wW:p5 | Codex | GPT-6-Luna · high | 상 | 설계가 걸린 구현, 디버깅 |
| `opencode-1` | wW:p3 | OpenCode 1.18 | DeepSeek V4.1 Flash · max | 중 | 명세가 분명한 구현, 테스트, 문서 정합성 점검 |
| `opencode-2` | wW:p4 | OpenCode 1.18 | MiMo-V2.6-Flash Free (effort 표시 없음) | 하 | 요약, 사실 조회, 단순 수정 |

- 이름은 에이전트가 재시작되면 해제된다. 세션을 시작할 때 `discover-workers.sh`로 확인하고 다시 붙인다.
- 모델이 바뀌면 이 표를 갱신한다.
- 2026-10-06 사용자 지시: Sol(codex-1)은 직접 구현보다 리뷰·서브 헤드로 쓰고, 주 구현은 Luna(codex-2)가 맡는다.
  서브 헤드는 맡은 영역의 하위 작업을 나누고 결과를 검수하지만, git과 최종 머지 판단은 orchestrator가 한다

## 이 레포 규칙과의 연결 (AGENTS.md)

- 작업 단위는 10분 이내. 넘으면 worker에게 분산하고 Orchestrator는 붙잡지 않는다.
- 작업이 끝나면 진척도와 이후 작업을 브리핑한다.
- 커밋은 사용자가 요청할 때만, Orchestrator만 한다. worker는 git 쓰기 금지.
- 모든 파일 100줄 이하. 위임 프롬프트의 제약에도 이 규칙을 넣는다.

## 외부 호출

- hermes 등은 `herdr agent prompt orchestrator "[from:hermes reply:issue] ..." --wait`로 요청한다.
- 결과는 마지막 줄 `RESULT: ...`와 GitHub 이슈(`plan`/`work`/`report`)로 돌려준다.

## 운영 기록

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
