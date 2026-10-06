#!/usr/bin/env bash
# restore 내부 함수. 상위 script가 common과 manifest를 준비한다.
set -euo pipefail
workspace() {
  local id label cwd response
  id="$(jq -r .workspace_id <<<"$row")"; label="$(jq -c .label <<<"$row")"
  target_ws="$(lookup workspaces "$id" "$label" '')"
  root_tab=''
  if [ -n "$target_ws" ]; then return; fi
  cwd="$(jq -r --arg w "$id" '[.panes[]|select(.workspace_id==$w)][0].cwd' "$manifest")"
  printf '%s workspace label=%s cwd=%s\n' "$mode" "$label" "$cwd"
  target_ws="pending:$id"
  if [ "$apply" = 1 ]; then
    local args=(workspace create --cwd "$cwd" --no-focus)
    [ "$label" = null ] || args+=(--label "$(jq -r .label <<<"$row")")
    response="$(query "${args[@]}")"
    target_ws="$(jq -er '.result.workspace.workspace_id' <<<"$response")"
    refresh
    root_tab="$(jq -er --arg w "$target_ws" '[.tabs[]|select(.workspace_id==$w)] |
      if length==1 then .[0].tab_id else error("new workspace root tab unclear") end' <<<"$live")"
  fi
}
tab() {
  local id label cwd
  id="$(jq -r .tab_id <<<"$row")"; label="$(jq -c .label <<<"$row")"
  target_tab="$(lookup tabs "$id" "$label" "$target_ws")"
  if [ -n "$target_tab" ]; then
    [ "$target_tab" != "$root_tab" ] || root_tab=''
    return
  fi
  cwd="$(jq -r --arg t "$id" '[.panes[]|select(.tab_id==$t)][0].cwd' "$manifest")"
  if [ -n "$root_tab" ]; then
    target_tab="$root_tab"; root_tab=''
    [ "$label" = null ] || change "root tab label=$label" tab rename "$target_tab" "$(jq -r .label <<<"$row")"
    return
  fi
  printf '%s tab workspace=%s label=%s cwd=%s\n' "$mode" "$target_ws" "$label" "$cwd"
  target_tab="pending:$id"
  if [ "$apply" = 1 ]; then
    local args=(tab create --workspace "$target_ws" --cwd "$cwd" --no-focus)
    [ "$label" = null ] || args+=(--label "$(jq -r .label <<<"$row")")
    target_tab="$(query "${args[@]}" | jq -er '.result.tab.tab_id')"
    refresh
  fi
}
pane() {
  local id index cwd anchor
  id="$(jq -r .pane_id <<<"$row")"; index="$(jq -r .index <<<"$row")"
  cwd="$(jq -r .cwd <<<"$row")"
  target_pane="$(jq -r --arg t "$target_tab" --arg p "$id" --argjson i "$index" '
    [.panes[]|select(.tab_id==$t)] as $rows |
    ([$rows[]|select(.pane_id==$p)][0] // $rows[$i]).pane_id // empty' <<<"$live")"
  if [ -z "$target_pane" ]; then
    printf '%s pane tab=%s index=%s cwd=%s (right split)\n' "$mode" "$target_tab" "$index" "$cwd"
    target_pane="pending:$id"
    if [ "$apply" = 1 ]; then
      anchor="$(jq -er --arg t "$target_tab" '[.panes[]|select(.tab_id==$t)][-1].pane_id' <<<"$live")"
      target_pane="$(query pane split --pane "$anchor" --direction right --cwd "$cwd" --no-focus |
        jq -er '.result.pane.pane_id')"
      refresh
    fi
  else
    local actual
    actual="$(jq -r --arg p "$target_pane" '.panes[]|select(.pane_id==$p)|.cwd' <<<"$live")"
    if [ "$actual" != "$cwd" ]; then
      printf 'skip pane %s cwd conflict: %s != %s\n' "$target_pane" "$actual" "$cwd"
      target_pane=''; return
    fi
  fi
  case "|$claimed|" in *"|$target_pane|"*) fail "pane binding 중복: $target_pane" ;; esac
  claimed="${claimed:+$claimed|}$target_pane"
}
