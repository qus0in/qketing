#!/usr/bin/env bash
# 현재 herdr workspace에서 호출 pane을 제외한 에이전트 pane(worker)을 찾아 JSON lines로 출력한다.
# 사용법: discover-workers.sh [workspace_id]   (기본: 호출 pane의 현재 workspace)
# 출력: {"pane_id","tab_id","kind","name","status","model_hint"}
#   model_hint: 화면 하단의 모델/effort 표시줄 (휴리스틱이므로 확신이 없으면 직접 화면을 읽어 확인)
# 의존성: herdr CLI, jq. HERDR_ENV=1 필요. 읽기 전용이며 입력은 보내지 않는다.
set -euo pipefail

[ "${HERDR_ENV:-}" = 1 ] || { echo "ERROR: herdr pane 안이 아님 (HERDR_ENV!=1)" >&2; exit 1; }
command -v jq >/dev/null || { echo "ERROR: jq 필요" >&2; exit 1; }
# pane이 다른 workspace로 이동하면 HERDR_* 환경변수는 이전 ID로 남는다 → 서버에 현재 위치를 묻는다.
cur="$(herdr pane current --current)"
self="$(jq -r '.result.pane.pane_id' <<<"$cur")"
ws="${1:-$(jq -r '.result.pane.workspace_id' <<<"$cur")}"

# 알려진 모델 계열 키워드 + effort 표기. 새 모델이 나오면 여기에 추가한다.
MODEL_RE='(GPT|gpt-|o[0-9]-|Claude|claude-|Opus|Sonnet|Haiku|Fable|DeepSeek|MiMo|Gemini|Qwen|Kimi|GLM|Grok|Llama|Mistral|Codestral|Devstral)'

model_hint() {
  # pane read는 JSON이 아니라 일반 텍스트를 반환한다.
  herdr pane read "$1" --source visible 2>/dev/null \
    | grep -E "$MODEL_RE" | tail -n1 \
    | sed -E 's/(┃|╹|▀|│)//g; s/^[[:space:]]+//; s/[[:space:]]+$//; s/[[:space:]]{2,}/ /g' || true
  # 박스 문자는 대괄호 클래스 대신 alternation으로 지운다 (LC_ALL=C에서 멀티바이트 문자가 깨지지 않게).
}

# 에이전트 이름은 pane list가 아니라 agent list에만 있다.
names="$(herdr agent list | jq -c '[.result.agents[] | {(.pane_id): .name}] | add // {}')"
herdr pane list --workspace "$ws" \
  | jq -c --arg self "$self" --argjson names "$names" '.result.panes[]
      | select(.agent != null and .pane_id != $self)
      | {pane_id, tab_id, kind: .agent, name: $names[.pane_id], status: .agent_status}' \
  | while IFS= read -r row; do
      pid="$(jq -r .pane_id <<<"$row")"
      jq -c --arg m "$(model_hint "$pid")" '. + {model_hint: $m}' <<<"$row"
    done
