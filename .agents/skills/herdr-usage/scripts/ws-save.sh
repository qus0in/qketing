#!/usr/bin/env bash
# 사용법: ws-save.sh [매니페스트 경로]. bash, jq, herdr, HERDR_ENV=1 필요.
# 기본 경로: HERDR_MANIFEST 또는 ~/.config/herdr/workspace-layout.json
set -euo pipefail
[ $# -le 1 ] || { echo 'usage: ws-save.sh [manifest]' >&2; exit 2; }
base="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$base/ws-common.sh"
out="${1:-$manifest_default}"
refresh
manifest="$(jq --arg saved_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '
  . as $all | {version:1,saved_at:$saved_at,
    workspaces:[.workspaces[] | {workspace_id,label}],
    tabs:[.tabs[] | {tab_id,workspace_id,label}],
    panes:[.tabs[] as $tab |
      [$all.panes[] | select(.tab_id == $tab.tab_id)] | to_entries[] |
      .key as $index | .value as $pane |
      [$all.agents[] | select(.pane_id == $pane.pane_id)] as $agent |
      if ($agent|length)>1 then error("duplicate pane agent") else
      {pane_id:$pane.pane_id,tab_id:$pane.tab_id,workspace_id:$pane.workspace_id,
       index:$index,cwd:$pane.cwd,kind:($agent[0].agent // $pane.agent),name:$agent[0].name} end]}' <<<"$live")"
mkdir -p "$(dirname "$out")"
tmp="$(mktemp "${out}.tmp.XXXXXX")"
trap 'rm -f "$tmp"' EXIT
chmod 600 "$tmp"
printf '%s\n' "$manifest" > "$tmp"
mv "$tmp" "$out"
printf 'saved: %s\n' "$out"
