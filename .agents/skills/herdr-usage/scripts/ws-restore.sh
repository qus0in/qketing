#!/usr/bin/env bash
# 사용법: ws-restore.sh [--apply] [매니페스트 경로]. 기본은 dry-run, bash/jq/herdr 필요.
# 경로: HERDR_MANIFEST 또는 ~/.config/herdr/workspace-layout.json. 기존 pane 종료 금지.
set -euo pipefail
base="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$base/ws-common.sh"
apply=0; mode=would
if [ "${1:-}" = --apply ]; then apply=1; mode=apply; shift; fi
[ $# -le 1 ] || fail 'usage: ws-restore.sh [--apply] [manifest]'
manifest="${1:-$manifest_default}"
[ -f "$manifest" ] || fail "매니페스트 없음: $manifest"
# mutation 전 전체 schema/바인딩 검증. 구버전/중복/null cwd는 추정하지 않는다.
jq -e '
  . as $m |
  def text: type=="string" and length>0;
  def valid_label: .==null or type=="string";
  .version==1 and (.workspaces|type=="array") and (.tabs|type=="array") and (.panes|type=="array") and
  all(.workspaces[]; (.workspace_id|text) and (.label|valid_label)) and
  all(.tabs[]; (.tab_id|text) and (.workspace_id|text) and (.label|valid_label)) and
  all(.panes[]; (.pane_id|text) and (.tab_id|text) and (.workspace_id|text) and
    (.cwd|text) and (.index|type=="number" and .>=0 and .==floor) and
    (.kind==null or (.kind|text)) and (.name==null or (.name|test("^[a-z][a-z0-9_-]{0,31}$")))) and
  ([.workspaces[].workspace_id]|length== (unique|length)) and
  ([.tabs[].tab_id]|length== (unique|length)) and
  ([.panes[].pane_id]|length== (unique|length)) and
  ([.panes[]|select(.name!=null)|.name]|length== (unique|length)) and
  all(.tabs[]; . as $t | any($m.workspaces[]; .workspace_id==$t.workspace_id)) and
  all(.panes[]; . as $p | any($m.tabs[]; .tab_id==$p.tab_id and .workspace_id==$p.workspace_id)) and
  all(.workspaces[]; . as $w | any($m.tabs[]; .workspace_id==$w.workspace_id)) and
  all(.tabs[]; . as $t | [$m.panes[]|select(.tab_id==$t.tab_id)|.index] | length>0 and sort==[range(0;length)])
  ' "$manifest" >/dev/null || fail '매니페스트 schema/바인딩 오류'
source "$base/ws-topology.sh"
source "$base/ws-agents.sh"
refresh
claimed=''
while IFS= read -r row; do
  saved_ws="$(jq -r .workspace_id <<<"$row")"
  workspace
  while IFS= read -r row; do
    saved_tab="$(jq -r .tab_id <<<"$row")"
    tab
    while IFS= read -r row; do
      pane
      [ -n "$target_pane" ] || continue
      agent
    done < <(jq -c --arg t "$saved_tab" '[.panes[]|select(.tab_id==$t)]|sort_by(.index)[]' "$manifest")
  done < <(jq -c --arg w "$saved_ws" '.tabs[]|select(.workspace_id==$w)' "$manifest")
done < <(jq -c '.workspaces[]' "$manifest")
printf 'ok: comparison complete (%s); skip 항목은 복구되지 않음\n' "$mode"
