#!/usr/bin/env bash
# restore 내부 agent 복구: 기존 다른 kind를 재시작하거나 shell 입력으로 강제하지 않는다.
set -euo pipefail
agent() {
  local kind name existing current_kind current_name owner
  kind="$(jq -r '.kind // empty' <<<"$row")"
  name="$(jq -r '.name // empty' <<<"$row")"
  [ -n "$kind" ] || return 0
  if [ -z "$name" ]; then printf 'skip agent %s: 저장된 이름 없음\n' "$target_pane"; return; fi
  existing="$(jq -c --arg p "$target_pane" '[.agents[]|select(.pane_id==$p)][0] // {}' <<<"$live")"
  current_kind="$(jq -r '.agent // empty' <<<"$existing")"
  current_name="$(jq -r '.name // empty' <<<"$existing")"
  owner="$(jq -r --arg n "$name" --arg p "$target_pane" '.agents[] |
    select(.name==$n and .pane_id!=$p) | .pane_id' <<<"$live")"
  [ -z "$owner" ] || { printf 'skip name conflict: %s @ %s\n' "$name" "$owner"; return; }
  if [ -n "$current_kind" ] && [ "$current_kind" != "$kind" ]; then
    printf 'skip kind conflict: %s (%s != %s)\n' "$target_pane" "$current_kind" "$kind"; return
  fi
  if [ -z "$current_kind" ]; then
    change "agent start $name kind=$kind pane=$target_pane" agent start "$name" --kind "$kind" --pane "$target_pane"
  elif [ "$current_name" != "$name" ]; then
    change "agent rename $target_pane -> $name" agent rename "$target_pane" "$name"
  fi
}
