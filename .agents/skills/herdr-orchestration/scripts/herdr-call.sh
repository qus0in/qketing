#!/usr/bin/env bash
# 사용법: herdr-call.sh status | resolve <id> | send <id> <메시지파일|->
#         herdr-call.sh wait <id> <ms> | read <id> <줄수>
# 의존성: bash, jq, herdr CLI, HERDR_ENV=1. tab/pane ID 사용, 이름은 진단용.
set -euo pipefail
fail() { printf 'herdr-call: %s\n' "$*" >&2; exit 1; }
usage() { fail "usage: $0 status|resolve <id>|send <id> <file|->|wait <id> <ms>|read <id> <lines>"; }
[ "${HERDR_ENV:-}" = 1 ] || fail 'HERDR_ENV=1인 pane에서 실행해야 함'
command -v jq >/dev/null || fail 'jq 필요'
command -v herdr >/dev/null || fail 'herdr 필요'
# CLI exit code와 JSON error 모두 확인. 실패 시 자동 재전송하지 않는다.
query() {
  local result code=0
  result="$(herdr "$@")" || code=$?
  [ "$code" -eq 0 ] || { printf '%s\n' "$result" >&2; return "$code"; }
  jq -e 'type == "object" and .error == null and (.result | type == "object")' \
    >/dev/null <<<"$result" || { printf '%s\n' "$result" >&2; return 1; }
  printf '%s\n' "$result"
}
load() {
  panes="$(query pane list)"
  tabs="$(query tab list)"
  agents="$(query agent list)"
  jq -e '.result.panes | type == "array"' >/dev/null <<<"$panes"
  jq -e '.result.tabs | type == "array"' >/dev/null <<<"$tabs"
  jq -e '.result.agents | type == "array"' >/dev/null <<<"$agents"
}
# 현재 workspace: pane current 조회 우선, 실패 시 HERDR_PANE_ID 앞부분(wW:p4 → wW).
current_workspace() {
  local json ws=''
  if json="$(query pane current)"; then ws="$(jq -r '.result.pane.workspace_id // empty' <<<"$json")"; fi
  if [ -z "$ws" ]; then ws="${HERDR_PANE_ID:-}"; ws="${ws%%:*}"; fi
  printf '%s' "$ws"
}
resolve() {
  local matches count
  [ -n "$1" ] || fail '빈 식별자'
  matches="$(jq -cn --arg target "$1" --arg ws "${current_ws:-}" --argjson p "$panes" \
    --argjson t "$tabs" --argjson a "$agents" '
    $p.result.panes as $panes | $t.result.tabs as $tabs |
    [$panes[] | select(.pane_id == $target)] as $direct |
    [$tabs[] | select(.tab_id == $target)] as $tab |
    [$a.result.agents[] | select(.name == $target)] as $name |
    [$tabs[] | select(.label == $target)] as $label |
    if ($direct|length) > 0 then $direct | map(.pane_id)
    elif ($tab|length) > 0 then
      if ($tab|length) != 1 then error("duplicate tab id")
      else [$panes[] | select(.tab_id == $target) | .pane_id] end
    elif ($name|length) > 0 then
      if ($name|length) != 1 then error("duplicate agent name")
      else [$panes[] | select(.pane_id == $name[0].pane_id) | .pane_id] end
    elif ($label|length) > 0 then
      ([$label[] | select($ws != "" and .workspace_id == $ws)]) as $local |
      if ($local|length) == 1 then [$panes[] | select(.tab_id == $local[0].tab_id) | .pane_id]
      elif ($local|length) > 1 then error("duplicate tab label")
      elif ($label|length) == 1 then [$panes[] | select(.tab_id == $label[0].tab_id) | .pane_id]
      else error("duplicate tab label")
      end
    else [] end')" || fail "식별자 해석 실패: $1"
  count="$(jq 'length' <<<"$matches")"
  [ "$count" -gt 0 ] || fail "대상 없음: $1 (status로 확인)"
  [ "$count" -eq 1 ] || fail "대상 중복: $1 ($count panes; pane ID 사용)"
  jq -er '.[0] | select(type == "string" and length > 0)' <<<"$matches"
}
[ $# -gt 0 ] || usage
action="$1"; shift
case "$action:$#" in
  status:0|resolve:1|send:2|wait:2|read:2) ;;
  *) usage ;;
esac
if [ "$action" = wait ] || [ "$action" = read ]; then
  [[ "$2" =~ ^[1-9][0-9]*$ ]] || fail 'ms/줄수는 양의 정수여야 함'
fi
load
current_ws="$(current_workspace)"
case "$action" in
  status)
    jq -cn --argjson p "$panes" --argjson t "$tabs" --argjson a "$agents" '
      $p.result.panes[] as $pane |
      [$t.result.tabs[] | select(.tab_id == $pane.tab_id)][0] as $tab |
      [$a.result.agents[] | select(.pane_id == $pane.pane_id)][0] as $agent |
      {pane_id: $pane.pane_id, tab_id: $pane.tab_id, label: $tab.label,
       name: $agent.name, kind: $pane.agent, status: $pane.agent_status}' ;;
  resolve) resolve "$1" ;;
  send)
    pane="$(resolve "$1")"
    if [ "$2" = - ]; then message="$(cat)"; else message="$(cat -- "$2")"; fi
    [[ "$message" =~ [^[:space:]] ]] || fail '빈 메시지는 전달하지 않음'
    # herdr는 -- 구분자를 지원하지 않아 대시로 시작하는 메시지를 옵션으로 해석한다.
    [[ "$message" != -* ]] || fail '메시지가 -로 시작함: herdr 옵션 오인을 막기 위해 일반 문구로 시작해야 함'
    query agent prompt "$pane" "$message" ;;
  wait)
    pane="$(resolve "$1")"
    query agent wait "$pane" --timeout "$2" ;;
  read)
    pane="$(resolve "$1")"
    herdr agent read "$pane" --source recent-unwrapped --lines "$2" ;;
esac
