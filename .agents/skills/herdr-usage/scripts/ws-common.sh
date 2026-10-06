#!/usr/bin/env bash
# 저장/복구 공용: JSON 응답 검사와 현재 서버 전체 inventory. bash, jq, herdr 필요.
set -euo pipefail
# 저장/복구 경로의 단일 정의. 다른 도구의 schema 파일과 공유하지 않는다.
manifest_default="${HERDR_MANIFEST:-$HOME/.config/herdr/workspace-layout.json}"
fail() { printf 'workspace-manifest: %s\n' "$*" >&2; exit 1; }
[ "${HERDR_ENV:-}" = 1 ] || fail 'HERDR_ENV=1인 pane에서 실행해야 함'
command -v jq >/dev/null || fail 'jq 필요'
command -v herdr >/dev/null || fail 'herdr 필요'
query() {
  local response code=0
  response="$(herdr "$@")" || code=$?
  [ "$code" -eq 0 ] || return "$code"
  jq -e '.error == null and (.result | type == "object")' >/dev/null <<<"$response" \
    || { printf '%s\n' "$response" >&2; return 1; }
  printf '%s\n' "$response"
}
refresh() {
  local w ws ts ps
  ws="$(query workspace list)"; ts='[]'; ps='[]'
  while IFS= read -r w; do
    tabs="$(query tab list --workspace "$w")"
    panes="$(query pane list --workspace "$w")"
    ts="$(jq -cn --argjson a "$ts" --argjson b "$tabs" '$a + $b.result.tabs')"
    ps="$(jq -cn --argjson a "$ps" --argjson b "$panes" '$a + $b.result.panes')"
  done < <(jq -er '.result.workspaces[].workspace_id' <<<"$ws")
  agents="$(query agent list)"
  live="$(jq -cn --argjson w "$ws" --argjson t "$ts" --argjson p "$ps" \
    --argjson a "$agents" '{workspaces:$w.result.workspaces,tabs:$t,panes:$p,agents:$a.result.agents}')"
  jq -e 'all(.[]; type == "array")' >/dev/null <<<"$live" || fail 'inventory 배열 누락'
}
# ID와 라벨을 함께 확인; ID가 바뀌면 유일한 라벨만 허용한다.
lookup() {
  jq -er --arg key "$1" --arg id "$2" --argjson label "$3" --arg parent "$4" '
    [.[$key][] | select($key == "workspaces" or .workspace_id == $parent)] as $rows |
    (if $key == "workspaces" then "workspace_id" else "tab_id" end) as $field |
    [$rows[] | select(.[$field] == $id and .label == $label)] as $exact |
    (if ($exact|length)>0 then $exact else [$rows[] | select(.label == $label)] end) |
    if length > 1 then error("ambiguous label/id")
    elif length == 0 then "" else .[0][$field] end' <<<"$live"
}
change() {
  printf '%s %s\n' "$mode" "$1"
  shift
  if [ "$apply" = 1 ]; then query "$@" >/dev/null; refresh; fi
}
