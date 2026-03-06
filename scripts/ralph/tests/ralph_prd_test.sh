#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
source "$REPO_ROOT/scripts/ralph/lib/ralph_prd.sh"

assert_eq() {
  local expected="$1"
  local actual="$2"
  local message="$3"

  if [[ "$expected" != "$actual" ]]; then
    echo "ASSERT_EQ failed: $message"
    echo "  expected: $expected"
    echo "  actual:   $actual"
    exit 1
  fi
}

assert_contains() {
  local needle="$1"
  local haystack="$2"
  local message="$3"

  if ! grep -Fq -- "$needle" <<<"$haystack"; then
    echo "ASSERT_CONTAINS failed: $message"
    echo "  missing: $needle"
    exit 1
  fi
}

assert_fails() {
  local message="$1"
  shift

  if "$@" >/dev/null 2>&1; then
    echo "ASSERT_FAILS failed: $message"
    exit 1
  fi
}

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
PRD_FILE="$TMP_DIR/prd.json"

cat > "$PRD_FILE" <<'JSON'
{
  "project": "MoneyManager",
  "branchName": "ralph/test-statuses",
  "description": "Test story state helpers",
  "userStories": [
    {
      "id": "US-001",
      "title": "Todo story",
      "description": "Story still pending",
      "acceptanceCriteria": ["One"],
      "priority": 2,
      "status": "todo",
      "passes": false,
      "notes": ""
    },
    {
      "id": "US-002",
      "title": "Implemented story",
      "description": "Story implemented but not passed",
      "acceptanceCriteria": ["Two"],
      "priority": 1,
      "status": "implemented",
      "passes": false,
      "notes": ""
    },
    {
      "id": "US-003",
      "title": "Legacy passed story",
      "description": "Legacy story with passes only",
      "acceptanceCriteria": ["Three"],
      "priority": 3,
      "passes": true,
      "notes": ""
    }
  ]
}
JSON

unset RALPH_REMOTE_RUN_MODE
unset RALPH_REMOTE_DEFER_CI_UNTIL_READY
assert_eq "ci-first" "$(ralph_resolve_remote_run_mode)" "default remote-run mode should be ci-first"

RALPH_REMOTE_RUN_MODE="deferred"
assert_eq "deferred" "$(ralph_resolve_remote_run_mode)" "explicit mode should win"

RALPH_REMOTE_RUN_MODE="ci-first"
RALPH_REMOTE_DEFER_CI_UNTIL_READY="true"
assert_eq "ci-first" "$(ralph_resolve_remote_run_mode)" "explicit mode should override legacy fallback"

unset RALPH_REMOTE_RUN_MODE
assert_eq "deferred" "$(ralph_resolve_remote_run_mode)" "legacy defer=true should map to deferred"

RALPH_REMOTE_DEFER_CI_UNTIL_READY="false"
assert_eq "ci-first" "$(ralph_resolve_remote_run_mode)" "legacy defer=false should map to ci-first"

RALPH_REMOTE_DEFER_CI_UNTIL_READY="maybe"
assert_fails "invalid legacy remote-run mode should fail" ralph_resolve_remote_run_mode
unset RALPH_REMOTE_DEFER_CI_UNTIL_READY

assert_eq "1" "$(ralph_count_todo_stories "$PRD_FILE")" "todo count"
assert_eq "1" "$(ralph_count_implemented_stories "$PRD_FILE")" "implemented count"
assert_eq "1" "$(ralph_count_passed_stories "$PRD_FILE")" "passed count"
assert_eq "2" "$(ralph_count_remaining_stories "$PRD_FILE")" "remaining count"
assert_eq "implemented" "$(ralph_story_status "$PRD_FILE" "US-002")" "status lookup should read explicit status"
assert_eq "passed" "$(ralph_story_status "$PRD_FILE" "US-003")" "status lookup should infer legacy passes=true"

NEXT_TODO="$(ralph_next_todo_story_json "$PRD_FILE")"
assert_eq "US-001" "$(jq -r '.id' <<<"$NEXT_TODO")" "next todo story should ignore implemented/passed stories"

SUMMARY="$(ralph_build_story_summary "$PRD_FILE")"
assert_contains "- [ ] US-001: Todo story" "$SUMMARY" "summary should render todo stories"
assert_contains "- [~] US-002: Implemented story" "$SUMMARY" "summary should render implemented stories"
assert_contains "- [x] US-003: Legacy passed story" "$SUMMARY" "summary should render passed stories"

ralph_set_story_status_in_file "$PRD_FILE" "US-001" "implemented"
assert_eq "implemented" "$(ralph_story_status "$PRD_FILE" "US-001")" "status writer should update story status"
assert_eq "false" "$(jq -r --arg story_id 'US-001' '.userStories[] | select(.id == $story_id) | .passes' "$PRD_FILE")" "implemented stories should keep passes=false"

ralph_promote_implemented_to_passed_in_file "$PRD_FILE"
assert_eq "0" "$(ralph_count_implemented_stories "$PRD_FILE")" "promotion should clear implemented stories"
assert_eq "3" "$(ralph_count_passed_stories "$PRD_FILE")" "promotion should mark implemented stories as passed"

assert_fails "ralph.sh should not run git commit --amend anymore" rg -n 'git commit --amend --no-edit' "$REPO_ROOT/scripts/ralph/ralph.sh"
assert_fails "ralph.sh should not run git reset --hard anymore" rg -n 'git reset --hard ' "$REPO_ROOT/scripts/ralph/ralph.sh"

echo "ralph_prd_test.sh: PASS"
