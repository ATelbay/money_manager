#!/bin/bash
set -uo pipefail

RALPH_EFFECTIVE_STATUS_JQ='(.status // (if .passes == true then "passed" else "todo" end))'

ralph_resolve_remote_run_mode() {
  local explicit_mode="${RALPH_REMOTE_RUN_MODE:-}"
  local legacy_defer="${RALPH_REMOTE_DEFER_CI_UNTIL_READY:-}"
  local legacy_normalized=""

  if [[ -n "$explicit_mode" ]]; then
    case "$explicit_mode" in
      ci-first|deferred)
        printf '%s\n' "$explicit_mode"
        return 0
        ;;
      *)
        echo "Error: Invalid RALPH_REMOTE_RUN_MODE='$explicit_mode'. Expected 'ci-first' or 'deferred'."
        return 1
        ;;
    esac
  fi

  if [[ -n "$legacy_defer" ]]; then
    legacy_normalized=$(printf '%s' "$legacy_defer" | tr '[:upper:]' '[:lower:]')
    case "$legacy_normalized" in
      true)
        printf 'deferred\n'
        return 0
        ;;
      false)
        printf 'ci-first\n'
        return 0
        ;;
      *)
        echo "Error: Invalid RALPH_REMOTE_DEFER_CI_UNTIL_READY='$legacy_defer'. Expected 'true' or 'false'."
        return 1
        ;;
    esac
  fi

  printf 'ci-first\n'
}

ralph_story_status() {
  local file="$1"
  local story_id="$2"

  jq -r --arg story_id "$story_id" '
    .userStories[]?
    | select(.id == $story_id)
    | (.status // (if .passes == true then "passed" else "todo" end))
  ' "$file" 2>/dev/null | head -n 1
}

ralph_count_stories_by_status() {
  local file="$1"
  local status="$2"

  jq -r --arg status "$status" '
    [
      .userStories[]?
      | select((.status // (if .passes == true then "passed" else "todo" end)) == $status)
    ] | length
  ' "$file" 2>/dev/null || echo "0"
}

ralph_count_todo_stories() {
  local file="$1"
  ralph_count_stories_by_status "$file" "todo"
}

ralph_count_implemented_stories() {
  local file="$1"
  ralph_count_stories_by_status "$file" "implemented"
}

ralph_count_passed_stories() {
  local file="$1"
  ralph_count_stories_by_status "$file" "passed"
}

ralph_count_remaining_stories() {
  local file="$1"

  jq -r '
    [
      .userStories[]?
      | select((.status // (if .passes == true then "passed" else "todo" end)) != "passed")
    ] | length
  ' "$file" 2>/dev/null || echo "0"
}

ralph_next_todo_story_json() {
  local file="$1"

  jq -c '
    (
      .userStories
      | to_entries
      | map(select((.value.status // (if .value.passes == true then "passed" else "todo" end)) == "todo"))
      | sort_by(.value.priority, .key)
      | .[0].value
    ) // {}
  ' "$file" 2>/dev/null || echo "{}"
}

ralph_build_story_summary() {
  local file="$1"

  jq -r '
    .userStories[]?
    | (.status // (if .passes == true then "passed" else "todo" end)) as $status
    | "- [" + (
        if $status == "passed" then "x"
        elif $status == "implemented" then "~"
        else " "
        end
      ) + "] " + .id + ": " + .title
  ' "$file" 2>/dev/null || true
}

ralph_set_story_status_in_file() {
  local file="$1"
  local story_id="$2"
  local status="$3"
  local tmp_file

  tmp_file=$(mktemp)

  if ! jq --arg story_id "$story_id" --arg status "$status" '
    .userStories |= map(
      if .id == $story_id then
        .status = $status
        | .passes = ($status == "passed")
      else
        .
      end
    )
  ' "$file" > "$tmp_file"; then
    rm -f "$tmp_file"
    return 1
  fi

  mv "$tmp_file" "$file"
}

ralph_promote_implemented_to_passed_in_file() {
  local file="$1"
  local tmp_file

  tmp_file=$(mktemp)

  if ! jq '
    .userStories |= map(
      if (.status // (if .passes == true then "passed" else "todo" end)) == "implemented" then
        .status = "passed"
        | .passes = true
      else
        .
      end
    )
  ' "$file" > "$tmp_file"; then
    rm -f "$tmp_file"
    return 1
  fi

  mv "$tmp_file" "$file"
}
