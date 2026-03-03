#!/bin/bash
# Ralph Wiggum - Long-running AI agent loop
# Usage: ./ralph.sh [--tool amp|claude|gemini|codex] [--review-tool amp|claude|gemini|codex] [--codex] [--remote-run] [--pr] [--download] [max_iterations]
#   --tool           AI engine for coding iterations (default: amp)
#   --review-tool    AI engine for code review step (default: same as --tool)
#   --codex          shortcut: codex codes, claude reviews (--tool codex --review-tool claude)
#   --remote-run     CI-first mode: no local heavy checks; Ralph shell handles per-story push + draft PR + required CI checks
#   --pr             push branch, create PR, code review, wait CI, trigger QA Build
#   --download       after QA Build: download APK locally (use on Mac; skip on VM — Garlic handles it)

# Parse arguments
TOOL="amp"  # Default to amp for backwards compatibility
REVIEW_TOOL=""  # Empty = use same as TOOL
MAX_ITERATIONS=10
CREATE_PR=false
DOWNLOAD_APK=false
REMOTE_RUN=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --tool)
      TOOL="$2"
      shift 2
      ;;
    --tool=*)
      TOOL="${1#*=}"
      shift
      ;;
    --review-tool)
      REVIEW_TOOL="$2"
      shift 2
      ;;
    --review-tool=*)
      REVIEW_TOOL="${1#*=}"
      shift
      ;;
    --codex)
      TOOL="codex"
      REVIEW_TOOL="claude"
      shift
      ;;
    --remote-run)
      REMOTE_RUN=true
      CREATE_PR=true
      shift
      ;;
    --pr)
      CREATE_PR=true
      shift
      ;;
    --download)
      DOWNLOAD_APK=true
      shift
      ;;
    *)
      # Assume it's max_iterations if it's a number
      if [[ "$1" =~ ^[0-9]+$ ]]; then
        MAX_ITERATIONS="$1"
      fi
      shift
      ;;
  esac
done

VALID_TOOLS="amp claude gemini codex"

# Validate tool choice
if [[ ! " $VALID_TOOLS " =~ " $TOOL " ]]; then
  echo "Error: Invalid tool '$TOOL'. Must be one of: $VALID_TOOLS"
  exit 1
fi

# Review tool defaults to same as main tool
if [[ -z "$REVIEW_TOOL" ]]; then
  REVIEW_TOOL="$TOOL"
fi

if [[ ! " $VALID_TOOLS " =~ " $REVIEW_TOOL " ]]; then
  echo "Error: Invalid review tool '$REVIEW_TOOL'. Must be one of: $VALID_TOOLS"
  exit 1
fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRD_FILE="$SCRIPT_DIR/prd.json"
TARGET_BASE_BRANCH="${RALPH_PR_BASE:-main}"
GIT_DIR="$(git rev-parse --git-dir 2>/dev/null || echo ".git")"

# Avoid interactive git credential prompts in automation
export GIT_TERMINAL_PROMPT=0
PROGRESS_FILE="$SCRIPT_DIR/progress.txt"
ARCHIVE_DIR="$SCRIPT_DIR/archive"
LAST_BRANCH_FILE="$SCRIPT_DIR/.last-branch"
REMOTE_STATE_FILE="$GIT_DIR/ralph-remote-run-state.json"
PR_URL=""

# Archive previous run if branch changed
if [ -f "$PRD_FILE" ] && [ -f "$LAST_BRANCH_FILE" ]; then
  CURRENT_BRANCH=$(jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null || echo "")
  LAST_BRANCH=$(cat "$LAST_BRANCH_FILE" 2>/dev/null || echo "")
  
  if [ -n "$CURRENT_BRANCH" ] && [ -n "$LAST_BRANCH" ] && [ "$CURRENT_BRANCH" != "$LAST_BRANCH" ]; then
    # Archive the previous run
    DATE=$(date +%Y-%m-%d)
    # Strip "ralph/" prefix from branch name for folder
    FOLDER_NAME=$(echo "$LAST_BRANCH" | sed 's|^ralph/||')
    ARCHIVE_FOLDER="$ARCHIVE_DIR/$DATE-$FOLDER_NAME"
    
    echo "Archiving previous run: $LAST_BRANCH"
    mkdir -p "$ARCHIVE_FOLDER"
    [ -f "$PRD_FILE" ] && cp "$PRD_FILE" "$ARCHIVE_FOLDER/"
    [ -f "$PROGRESS_FILE" ] && cp "$PROGRESS_FILE" "$ARCHIVE_FOLDER/"
    echo "   Archived to: $ARCHIVE_FOLDER"
    
    # Reset progress file for new run
    echo "# Ralph Progress Log" > "$PROGRESS_FILE"
    echo "Started: $(date)" >> "$PROGRESS_FILE"
    echo "---" >> "$PROGRESS_FILE"
  fi
fi

# Track current branch
if [ -f "$PRD_FILE" ]; then
  CURRENT_BRANCH=$(jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null || echo "")
  if [ -n "$CURRENT_BRANCH" ]; then
    echo "$CURRENT_BRANCH" > "$LAST_BRANCH_FILE"
  fi
fi

# Initialize progress file if it doesn't exist
if [ ! -f "$PROGRESS_FILE" ]; then
  echo "# Ralph Progress Log" > "$PROGRESS_FILE"
  echo "Started: $(date)" >> "$PROGRESS_FILE"
  echo "---" >> "$PROGRESS_FILE"
fi

# Validate prd.json exists, schema basics, and branch
if [ ! -f "$PRD_FILE" ]; then
  echo "Error: scripts/ralph/prd.json not found. Create it before running Ralph."
  exit 1
fi

if ! jq -e '.userStories and (.userStories | type == "array") and (.userStories | length > 0)' "$PRD_FILE" > /dev/null 2>&1; then
  echo "Error: scripts/ralph/prd.json is invalid: .userStories must be a non-empty array."
  exit 1
fi

BRANCH_NAME=$(jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null)
if [[ -z "$BRANCH_NAME" ]]; then
  echo "Error: scripts/ralph/prd.json is invalid: .branchName is required."
  exit 1
fi

RUN_DESCRIPTION=$(jq -r '.description // "Ralph auto-PR"' "$PRD_FILE" 2>/dev/null)

count_pending_stories() {
  jq -r '[.userStories[] | select(.passes != true)] | length' "$PRD_FILE" 2>/dev/null || echo "999"
}

next_pending_story_json() {
  jq -c '(.userStories | to_entries | map(select(.value.passes != true)) | sort_by(.value.priority, .key) | .[0].value) // {}' "$PRD_FILE" 2>/dev/null
}

build_story_summary() {
  jq -r '.userStories[] | "- [" + (if .passes == true then "x" else " " end) + "] " + .id + ": " + .title' "$PRD_FILE" 2>/dev/null
}

filtered_git_status() {
  git status --porcelain --untracked-files=all || true
}

working_tree_has_user_changes() {
  [[ -n "$(filtered_git_status)" ]]
}

ensure_gh_ready() {
  if ! command -v gh >/dev/null 2>&1; then
    echo "Error: GitHub CLI (gh) is required for PR/CI operations."
    exit 1
  fi
  if ! gh auth status >/dev/null 2>&1; then
    echo "Error: gh CLI is not authenticated. Run: gh auth login"
    exit 1
  fi
}

require_active_branch() {
  local active_branch
  active_branch=$(git branch --show-current 2>/dev/null || echo "")
  if [[ "$active_branch" != "$BRANCH_NAME" ]]; then
    echo "ERROR: Expected active branch '$BRANCH_NAME' but found '${active_branch:-detached}'."
    echo "The agent should check out the PRD branch before continuing."
    exit 1
  fi
}

push_branch() {
  local branch="$1"
  echo "Pushing branch: $branch"
  if ! git push -u origin "$branch"; then
    echo "ERROR: git push failed (non-interactive mode). Check gh auth / git credentials."
    exit 1
  fi
}

branch_has_unpushed_commits() {
  local upstream ahead_count
  upstream=$(git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>/dev/null || echo "")
  if [[ -z "$upstream" ]]; then
    return 0
  fi

  ahead_count=$(git rev-list --count "$upstream..HEAD" 2>/dev/null || echo "0")
  [[ "$ahead_count" -gt 0 ]]
}

push_branch_if_ahead() {
  local branch="$1"
  if branch_has_unpushed_commits; then
    echo "Local branch has unpushed commits. Syncing branch before exit..."
    push_branch "$branch"
    return 0
  fi
  return 1
}

get_pr_snapshot() {
  gh pr view "$BRANCH_NAME" --json mergeable,mergeStateStatus,statusCheckRollup,url 2>/dev/null || echo '{}'
}

extract_failed_checks() {
  local snapshot="$1"
  local failed_checks
  failed_checks=$(jq -r '
    .statusCheckRollup[]?
    | select(((.status // "COMPLETED") | ascii_upcase) == "COMPLETED")
    | (.conclusion // "" | ascii_upcase) as $conclusion
    | select($conclusion != "" and $conclusion != "SUCCESS" and $conclusion != "SKIPPED" and $conclusion != "NEUTRAL")
    | "- \(.name // "check") [\($conclusion)] :: \(.detailsUrl // "")"
  ' <<<"$snapshot")
  [[ -z "$failed_checks" ]] && failed_checks="- none"
  printf '%s\n' "$failed_checks"
}

clear_remote_repair_state() {
  rm -f "$REMOTE_STATE_FILE"
}

validate_remote_story_iteration() {
  local start_head="$1"
  local story_id="$2"
  local repair_mode="${3:-false}"
  local commit_range head_subject other_story_commits

  if [[ -n "$start_head" ]]; then
    commit_range="$start_head..HEAD"
  else
    commit_range="HEAD"
  fi

  other_story_commits=$(git log --format=%s "$commit_range" 2>/dev/null | grep -E '^feat: \[[^]]+\] - ' | grep -vF "feat: [$story_id] - " || true)
  if [[ -n "$other_story_commits" ]]; then
    echo "ERROR: --remote-run iteration targeted a different story while $story_id is active."
    printf '%s\n' "$other_story_commits"
    exit 1
  fi

  head_subject=$(git log -1 --format=%s HEAD 2>/dev/null || echo "")
  if [[ "$repair_mode" != "true" && ! "$head_subject" =~ ^feat:\ \[$story_id\]\ -\  ]]; then
    echo "ERROR: Expected latest commit to target $story_id in --remote-run."
    echo "Latest commit: ${head_subject:-<none>}"
    exit 1
  fi
}

mark_story_passed_locally() {
  local story_id="$1"
  local tmp_file
  tmp_file=$(mktemp)

  if ! jq --arg story_id "$story_id" '(.userStories[] | select(.id == $story_id) | .passes) = true' "$PRD_FILE" > "$tmp_file"; then
    rm -f "$tmp_file"
    echo "ERROR: Failed to mark $story_id as passed in prd.json."
    exit 1
  fi

  mv "$tmp_file" "$PRD_FILE"
  git add "$PRD_FILE"
  if ! git diff --cached --quiet; then
    if ! git commit -m "chore(ralph): mark $story_id passed after CI"; then
      echo "ERROR: Failed to commit CI pass marker for $story_id."
      exit 1
    fi
  fi
}

commit_review_feedback_if_needed() {
  if ! working_tree_has_user_changes; then
    return 1
  fi

  echo "Review produced local changes. Auto-committing review feedback..."
  git add -A
  if git diff --cached --quiet; then
    return 1
  fi

  if ! git commit -m "fix(review): apply Ralph review feedback"; then
    echo "ERROR: Failed to commit review feedback."
    exit 1
  fi

  return 0
}

write_remote_repair_state() {
  local story_id="$1"
  local story_title="$2"
  local commit_sha="$3"
  local snapshot="$4"
  local failed_checks="$5"
  local attempts="$6"

  jq -n \
    --arg story_id "$story_id" \
    --arg story_title "$story_title" \
    --arg last_commit "$commit_sha" \
    --arg failed_checks "$failed_checks" \
    --argjson attempts "$attempts" \
    --argjson pr_snapshot "$snapshot" \
    '{
      mode: "repair",
      storyId: $story_id,
      storyTitle: $story_title,
      lastCommit: $last_commit,
      failedChecks: $failed_checks,
      attempts: $attempts,
      prSnapshot: $pr_snapshot
    }' > "$REMOTE_STATE_FILE"
}

REMOTE_STORY_ID=""
REMOTE_STORY_TITLE=""
REMOTE_REPAIR_MODE=false
REMOTE_REPAIR_ATTEMPTS=0
REMOTE_REPAIR_LAST_COMMIT=""
REMOTE_REPAIR_FAILED_CHECKS="- none"
REMOTE_REPAIR_PR_SNAPSHOT="{}"

load_remote_story_context() {
  REMOTE_STORY_ID=""
  REMOTE_STORY_TITLE=""
  REMOTE_REPAIR_MODE=false
  REMOTE_REPAIR_ATTEMPTS=0
  REMOTE_REPAIR_LAST_COMMIT=""
  REMOTE_REPAIR_FAILED_CHECKS="- none"
  REMOTE_REPAIR_PR_SNAPSHOT="{}"

  if [[ -f "$REMOTE_STATE_FILE" ]]; then
    REMOTE_REPAIR_MODE=true
    REMOTE_STORY_ID=$(jq -r '.storyId // empty' "$REMOTE_STATE_FILE" 2>/dev/null)
    REMOTE_STORY_TITLE=$(jq -r '.storyTitle // empty' "$REMOTE_STATE_FILE" 2>/dev/null)
    REMOTE_REPAIR_ATTEMPTS=$(jq -r '.attempts // 0' "$REMOTE_STATE_FILE" 2>/dev/null)
    REMOTE_REPAIR_LAST_COMMIT=$(jq -r '.lastCommit // empty' "$REMOTE_STATE_FILE" 2>/dev/null)
    REMOTE_REPAIR_FAILED_CHECKS=$(jq -r '.failedChecks // "- none"' "$REMOTE_STATE_FILE" 2>/dev/null)
    REMOTE_REPAIR_PR_SNAPSHOT=$(jq -c '.prSnapshot // {}' "$REMOTE_STATE_FILE" 2>/dev/null || echo '{}')
    if [[ -z "$REMOTE_STORY_ID" ]]; then
      echo "ERROR: Remote repair state is invalid (missing storyId). Delete $REMOTE_STATE_FILE and rerun Ralph."
      exit 1
    fi
  else
    local pending_story_json
    pending_story_json=$(next_pending_story_json)
    REMOTE_STORY_ID=$(jq -r '.id // empty' <<<"$pending_story_json")
    REMOTE_STORY_TITLE=$(jq -r '.title // empty' <<<"$pending_story_json")
  fi
}

ensure_pr_exists() {
  local branch="$1"
  local description="$2"
  local story_summary="$3"
  local draft="${4:-false}"
  local pr_body
  pr_body="$(cat <<EOF
## Summary
$description

## Story Status
$story_summary

## Progress Log
<details>
<summary>Click to expand</summary>

$(cat "$PROGRESS_FILE")

</details>
EOF
)"

  local existing_pr
  existing_pr=$(gh pr view "$branch" --json url -q '.url' 2>/dev/null || echo "")
  if [[ -n "$existing_pr" && "$existing_pr" != "null" ]]; then
    PR_URL="$existing_pr"
    if ! gh pr edit "$branch" --title "$description" --body "$pr_body" >/dev/null 2>&1; then
      echo "WARN: Failed to refresh PR title/body; continuing with existing PR."
    fi
    if [[ "$draft" != "true" ]]; then
      gh pr ready "$branch" >/dev/null 2>&1 || true
    fi
    echo "PR already exists: $PR_URL"
    return 0
  fi

  local -a create_args=(
    --base "$TARGET_BASE_BRANCH"
    --head "$branch"
    --title "$description"
    --body "$pr_body"
  )
  if [[ "$draft" == "true" ]]; then
    create_args=(--draft "${create_args[@]}")
  fi

  if ! PR_URL=$(gh pr create "${create_args[@]}" 2>&1); then
    echo "ERROR: Failed to create PR."
    echo "$PR_URL"
    exit 1
  fi
  echo "PR created: $PR_URL"
}

wait_required_checks() {
  local branch="$1"
  local pr_url="${2:-}"
  local snapshot
  local effective_pr_url="$pr_url"
  CHECKS_LAST_SNAPSHOT="{}"
  CHECKS_LAST_FAILURE_SUMMARY="- none"

  echo ""
  echo "Waiting for required CI checks..."
  # gh CLI (Mar 2026) supports --watch/--required; --fail-fast is not available
  if gh pr checks "$branch" --watch --required 2>&1; then
    echo "CI passed!"
    return 0
  fi

  if ! snapshot=$(gh pr view "$branch" --json url,statusCheckRollup 2>/dev/null); then
    echo "ERROR: gh pr checks failed and Ralph could not read PR status afterwards."
    echo "Treating this as a GitHub CLI/API issue, not a code failure."
    return 2
  fi

  CHECKS_LAST_SNAPSHOT="$snapshot"
  CHECKS_LAST_FAILURE_SUMMARY=$(extract_failed_checks "$snapshot")
  if [[ "$CHECKS_LAST_FAILURE_SUMMARY" == "- none" ]]; then
    echo "ERROR: gh pr checks failed but no blocking failed checks were found in PR status."
    echo "Treating this as a GitHub CLI/API issue, not a code failure."
    return 2
  fi

  if [[ -z "$effective_pr_url" ]]; then
    effective_pr_url=$(jq -r '.url // empty' <<<"$snapshot")
  fi

  if [[ -n "$effective_pr_url" ]]; then
    echo "CI failed. Check PR for details: $effective_pr_url"
  else
    echo "CI failed."
  fi
  return 1
}

latest_workflow_run_id() {
  local workflow_name="$1"
  local branch="$2"
  gh run list --branch "$branch" --workflow "$workflow_name" --limit 1 --json databaseId -q '.[0].databaseId' 2>/dev/null || true
}

wait_for_new_workflow_run_id() {
  local workflow_name="$1"
  local branch="$2"
  local previous_run_id="${3:-}"
  local run_id=""

  for _ in $(seq 1 12); do
    sleep 5
    run_id=$(latest_workflow_run_id "$workflow_name" "$branch")
    if [[ -n "$run_id" && "$run_id" != "null" && "$run_id" != "$previous_run_id" ]]; then
      printf '%s\n' "$run_id"
      return 0
    fi
  done

  return 1
}

sync_remote_run_iteration() {
  local start_head="$1"
  local start_pending="$2"
  local story_id="$3"
  local story_title="$4"
  local end_head end_pending
  local snapshot failed_checks attempts wait_status

  REMOTE_CI_PASSED=false
  REMOTE_CI_FAILED=false

  require_active_branch

  if working_tree_has_user_changes; then
    echo "ERROR: --remote-run iteration left uncommitted changes."
    echo "Remote CI mode requires the agent to commit the completed story before returning."
    filtered_git_status
    exit 1
  fi

  end_head=$(git rev-parse HEAD 2>/dev/null || echo "")
  end_pending=$(count_pending_stories)

  if [[ "$end_head" == "$start_head" && "$end_pending" == "$start_pending" ]]; then
    echo "No committed progress detected in this iteration; skipping PR/CI sync."
    return 0
  fi

  validate_remote_story_iteration "$start_head" "$story_id" "$REMOTE_REPAIR_MODE"
  push_branch "$BRANCH_NAME"
  ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" true
  wait_required_checks "$BRANCH_NAME" "$PR_URL"
  wait_status=$?
  if [[ "$wait_status" -eq 2 ]]; then
    echo "ERROR: Unable to determine CI state due to GitHub CLI/API failure."
    exit 1
  fi
  if [[ "$wait_status" -ne 0 ]]; then
    snapshot="${CHECKS_LAST_SNAPSHOT:-$(get_pr_snapshot)}"
    failed_checks="${CHECKS_LAST_FAILURE_SUMMARY:-$(extract_failed_checks "$snapshot")}"
    attempts=$((REMOTE_REPAIR_ATTEMPTS + 1))
    write_remote_repair_state "$story_id" "$story_title" "$end_head" "$snapshot" "$failed_checks" "$attempts"
    REMOTE_CI_FAILED=true
    echo "CI failed for $story_id. Entering repair mode (attempt $attempts)."
    return 0
  fi

  clear_remote_repair_state
  mark_story_passed_locally "$story_id"
  REMOTE_CI_PASSED=true
  echo "CI passed for $story_id. Marked as passed locally."
}

INITIAL_PENDING_STORIES=$(count_pending_stories)
if [[ "$REMOTE_RUN" != "true" && -f "$REMOTE_STATE_FILE" ]]; then
  clear_remote_repair_state
fi
if [[ "$INITIAL_PENDING_STORIES" == "0" && -f "$REMOTE_STATE_FILE" ]]; then
  clear_remote_repair_state
fi

if [[ "$INITIAL_PENDING_STORIES" == "0" ]]; then
  if [[ "$CREATE_PR" == "false" && "$DOWNLOAD_APK" == "false" ]]; then
    echo "Nothing to do: all stories in prd.json have passes: true."
    exit 0
  fi
  echo "All stories already pass — skipping iterations, proceeding to PR/download flow."
fi

if [[ "$REVIEW_TOOL" != "$TOOL" ]]; then
  echo "Starting Ralph - Tool: $TOOL - Review: $REVIEW_TOOL - Max iterations: $MAX_ITERATIONS"
else
  echo "Starting Ralph - Tool: $TOOL - Max iterations: $MAX_ITERATIONS"
fi

if [[ "$CREATE_PR" == "true" || "$DOWNLOAD_APK" == "true" ]]; then
  ensure_gh_ready
fi

if [[ "$REMOTE_RUN" == "true" ]]; then
  echo "Mode: --remote-run (CI-first, local heavy checks disabled)"
else
  # Warm up Gradle daemon before iterations so each coding step compiles in 1-3 min
  echo ""
  echo "Warming up Gradle daemon (compileDebugKotlin)..."
  ./gradlew compileDebugKotlin --quiet 2>&1 | tail -3 || true
  echo "Gradle daemon ready."
fi

export RALPH_REMOTE_RUN="$REMOTE_RUN"
export RALPH_CREATE_PR="$CREATE_PR"

build_agent_prompt() {
  local prompt_file="$SCRIPT_DIR/CLAUDE.md"

  if [[ "$REMOTE_RUN" == "true" ]]; then
    local pr_json failed_checks
    if [[ "$REMOTE_REPAIR_MODE" == "true" ]]; then
      pr_json=$(get_pr_snapshot)
      if [[ "$pr_json" == "{}" ]]; then
        pr_json="$REMOTE_REPAIR_PR_SNAPSHOT"
      fi
      failed_checks=$(extract_failed_checks "$pr_json")
      if [[ "$failed_checks" == "- none" ]]; then
        failed_checks="$REMOTE_REPAIR_FAILED_CHECKS"
      fi
    else
      pr_json=$(get_pr_snapshot)
      failed_checks=$(extract_failed_checks "$pr_json")
    fi

    prompt_file=$(mktemp)
    cat "$SCRIPT_DIR/CLAUDE.md" > "$prompt_file"
    cat >> "$prompt_file" <<EOF

## REMOTE-RUN STRICT OVERRIDE (generated by ralph.sh)
- You are in CI-first mode. DO NOT run any local ./gradlew commands.
- Work only on story: $REMOTE_STORY_ID — $REMOTE_STORY_TITLE
- Implement exactly one story, commit it locally, and update progress.txt.
- Do NOT set passes:true in prd.json; Ralph shell will mark the story as passed only after green CI.
- Ralph shell will handle git push, draft PR creation/reuse, and waiting for required CI checks after your iteration.
- Do NOT run gh pr checks, gh workflow run, or any long-lived CI waiting commands yourself.
- If CI is failing, use the snapshot below as context for the next fix and keep working on the same story.
- If you are in repair mode, fix the current branch state for the same story. Do NOT start a new story.
- Current PR status snapshot:
$(echo "$pr_json" | jq -c '.')
- Failing checks snapshot:
$failed_checks
EOF
    if [[ "$REMOTE_REPAIR_MODE" == "true" ]]; then
      cat >> "$prompt_file" <<EOF
- Repair mode: true
- Repair attempt: $REMOTE_REPAIR_ATTEMPTS
- Last failing story commit: $REMOTE_REPAIR_LAST_COMMIT
EOF
    fi
  fi

  echo "$prompt_file"
}

for i in $(seq 1 $MAX_ITERATIONS); do
  echo ""
  echo "==============================================================="
  echo "  Ralph Iteration $i of $MAX_ITERATIONS ($TOOL)"
  echo "==============================================================="

  ITERATION_START_HEAD=$(git rev-parse HEAD 2>/dev/null || echo "")
  ITERATION_START_PENDING=$(count_pending_stories)
  AGENT_COMPLETE=false

  if [[ "$REMOTE_RUN" == "true" ]]; then
    load_remote_story_context
    if [[ -z "$REMOTE_STORY_ID" ]]; then
      AGENT_COMPLETE=true
    fi
  fi

  if [[ "$AGENT_COMPLETE" == "false" ]]; then
    # Run the selected tool with the ralph prompt
    AGENT_PROMPT_FILE=$(build_agent_prompt)
    if [[ "$TOOL" == "amp" ]]; then
      OUTPUT=$(cat "$AGENT_PROMPT_FILE" | amp --dangerously-allow-all 2>&1 | tee /dev/stderr) || true
    elif [[ "$TOOL" == "claude" ]]; then
      OUTPUT=$(claude --dangerously-skip-permissions --print < "$AGENT_PROMPT_FILE" 2>&1 | tee /dev/stderr) || true
    elif [[ "$TOOL" == "gemini" ]]; then
      OUTPUT=$(cat "$AGENT_PROMPT_FILE" | gemini --auto 2>&1 | tee /dev/stderr) || true
    elif [[ "$TOOL" == "codex" ]]; then
      OUTPUT=$(codex exec --dangerously-bypass-approvals-and-sandbox - < "$AGENT_PROMPT_FILE" 2>&1 | tee /dev/stderr) || true
    fi
    if [[ "$AGENT_PROMPT_FILE" != "$SCRIPT_DIR/CLAUDE.md" ]]; then
      rm -f "$AGENT_PROMPT_FILE"
    fi

    if [[ "$REMOTE_RUN" != "true" ]] && echo "$OUTPUT" | grep -q "<promise>COMPLETE</promise>"; then
      PENDING_STORIES=$(count_pending_stories)

      if [[ "$PENDING_STORIES" != "0" ]]; then
        echo ""
        echo "WARNING: Agent signaled COMPLETE but $PENDING_STORIES stories still have passes=false. Ignoring completion signal."
      else
        AGENT_COMPLETE=true
      fi
    fi
  fi

  if [[ "$REMOTE_RUN" == "true" && "$AGENT_COMPLETE" == "false" ]]; then
    sync_remote_run_iteration "$ITERATION_START_HEAD" "$ITERATION_START_PENDING" "$REMOTE_STORY_ID" "$REMOTE_STORY_TITLE"
    if [[ "$(count_pending_stories)" == "0" && ! -f "$REMOTE_STATE_FILE" ]]; then
      AGENT_COMPLETE=true
    fi
  fi

  if [[ "$AGENT_COMPLETE" == "true" ]]; then
    echo ""
    echo "Ralph completed all tasks!"
    echo "Completed at iteration $i of $MAX_ITERATIONS"

    # Create PR if --pr flag was passed
    if [[ "$CREATE_PR" == "true" ]]; then
      STORY_SUMMARY=$(build_story_summary)

      echo ""
      echo "==============================================================="
      echo "  Creating PR for branch: $BRANCH_NAME"
      echo "==============================================================="

      require_active_branch

      # Handle dirty working tree before PR operations (default: auto-story)
      if working_tree_has_user_changes; then
        echo "WARN: Working tree has uncommitted changes before PR."
        filtered_git_status
        echo "Auto-story mode: staging and committing remaining changes before PR..."
        git add -A
        if ! git diff --cached --quiet; then
          if ! git commit -m "chore(ralph): auto-commit remaining story-related changes before PR"; then
            echo "ERROR: Auto-story commit failed. Resolve conflicts/issues and re-run."
            exit 1
          fi
        fi
      fi

      push_branch "$BRANCH_NAME"
      ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$STORY_SUMMARY" false

      # ── Code Review Loop (max 2 attempts) ──
      MAX_REVIEWS=2
      REVIEW_CLEAN=false
      for r in $(seq 1 $MAX_REVIEWS); do
        echo ""
        echo "==============================================================="
        echo "  Code Review — attempt $r of $MAX_REVIEWS ($REVIEW_TOOL)"
        echo "==============================================================="

        REVIEW_BEFORE_SHA=$(git rev-parse HEAD 2>/dev/null || echo "")
        REVIEW_OUTPUT=""
        if [[ "$REVIEW_TOOL" == "claude" ]]; then
          REVIEW_OUTPUT=$(claude --dangerously-skip-permissions --print < "$SCRIPT_DIR/review-prompt.md" 2>&1 | tee /dev/stderr) || true
        elif [[ "$REVIEW_TOOL" == "gemini" ]]; then
          REVIEW_OUTPUT=$(cat "$SCRIPT_DIR/review-prompt.md" | gemini --auto 2>&1 | tee /dev/stderr) || true
        elif [[ "$REVIEW_TOOL" == "amp" ]]; then
          REVIEW_OUTPUT=$(cat "$SCRIPT_DIR/review-prompt.md" | amp --dangerously-allow-all 2>&1 | tee /dev/stderr) || true
        elif [[ "$REVIEW_TOOL" == "codex" ]]; then
          REVIEW_OUTPUT=$(codex exec --dangerously-bypass-approvals-and-sandbox - < "$SCRIPT_DIR/review-prompt.md" 2>&1 | tee /dev/stderr) || true
        fi

        if echo "$REVIEW_OUTPUT" | grep -Eq "<review>(CLEAN|NO_CHANGES_NEEDED)</review>"; then
          echo "Code review passed — no issues found."
          REVIEW_CLEAN=true
          break
        elif echo "$REVIEW_OUTPUT" | grep -Eq "<review>(FIXED|FIXED_AND_COMMITTED)</review>"; then
          commit_review_feedback_if_needed || true
          REVIEW_AFTER_SHA=$(git rev-parse HEAD 2>/dev/null || echo "")
          if [[ "$REVIEW_AFTER_SHA" == "$REVIEW_BEFORE_SHA" ]]; then
            echo "Review reported fixes but produced no new commit. Treating as clean."
            REVIEW_CLEAN=true
            break
          fi
          echo "Code review found and fixed issues. Pushing..."
          push_branch "$BRANCH_NAME"
          echo "Fix pushed. Running review again..."
        else
          echo "Code review returned unexpected output. Skipping."
          break
        fi
      done

      if [[ "$REVIEW_CLEAN" == "false" ]]; then
        echo ""
        echo "WARNING: Code review did not pass after $MAX_REVIEWS attempts."
        echo "Manual review required for PR: $PR_URL"
        exit 1
      fi

      # Check for merge conflicts before waiting for CI
      echo ""
      echo "Checking for merge conflicts..."
      MERGEABLE=$(gh pr view "$BRANCH_NAME" --json mergeable -q '.mergeable' 2>/dev/null || echo "UNKNOWN")
      if [[ "$MERGEABLE" == "CONFLICTING" ]]; then
        echo "ERROR: PR has merge conflicts — CI will not run until resolved."
        echo "Resolve conflicts and re-run Ralph: $PR_URL"
        exit 1
      fi

      CI_STATUS="failed"
      if wait_required_checks "$BRANCH_NAME" "$PR_URL"; then
        CI_STATUS="passed"
      else
        exit 1
      fi

      if [[ "$REVIEW_CLEAN" == "true" && "$CI_STATUS" == "passed" ]]; then
        echo ""
        echo "Triggering QA Build..."
        PREVIOUS_QA_RUN_ID=$(latest_workflow_run_id "QA Build" "$BRANCH_NAME")
        if ! gh workflow run "QA Build" --ref "$BRANCH_NAME"; then
          echo "ERROR: Failed to trigger QA Build."
          exit 1
        fi
        if ! RUN_ID=$(wait_for_new_workflow_run_id "QA Build" "$BRANCH_NAME" "$PREVIOUS_QA_RUN_ID"); then
          echo "ERROR: QA Build was triggered but no new workflow run appeared."
          exit 1
        fi
      else
        echo ""
        echo "Skipping QA Build (review not clean)."
      fi

      # Wait for QA Build to complete
      if [[ "$REVIEW_CLEAN" == "true" && "$CI_STATUS" == "passed" ]]; then
        echo "QA Build run ID: $RUN_ID"
        echo "$RUN_ID" > "$SCRIPT_DIR/.qa-run-id"

        # Write run summary for Garlic (Telegram report)
        if [[ "$REVIEW_CLEAN" == "true" ]]; then
          if [[ "$r" -eq 1 ]]; then
            REVIEW_STATUS="CLEAN"
          else
            REVIEW_STATUS="FIXED (attempt $r)"
          fi
        else
          REVIEW_STATUS="UNRESOLVED"
        fi
        STORIES_JSON=$(jq '[.userStories[] | {id: .id, title: .title}]' "$PRD_FILE")
        jq -n \
          --arg branch "$BRANCH_NAME" \
          --arg pr_url "$PR_URL" \
          --argjson iterations "$i" \
          --arg review_status "$REVIEW_STATUS" \
          --arg ci_status "$CI_STATUS" \
          --argjson stories "$STORIES_JSON" \
          '{branch: $branch, pr_url: $pr_url, iterations: $iterations, review_status: $review_status, ci_status: $ci_status, stories: $stories}' \
          > "$SCRIPT_DIR/.run-summary.json"

        if [[ "$DOWNLOAD_APK" == "true" ]]; then
          echo "Waiting for QA Build to finish..."
          gh run watch "$RUN_ID"

          # Remove old APKs before downloading new one
          rm -f "$SCRIPT_DIR/artifacts/"*.apk 2>/dev/null || true

          echo ""
          echo "Downloading QA debug APK..."
          mkdir -p "$SCRIPT_DIR/artifacts"
          gh run download "$RUN_ID" -n qa-debug-apk -D "$SCRIPT_DIR/artifacts/" 2>&1 && \
            echo "APK downloaded to: $SCRIPT_DIR/artifacts/" || \
            echo "Failed to download. Run manually: gh run download $RUN_ID -n qa-debug-apk"
        else
          echo "QA Build triggered. Download when ready:"
          echo "  gh run download $RUN_ID -n qa-debug-apk"
        fi
      fi
    fi

    exit 0
  fi
  
  echo "Iteration $i complete. Continuing..."
  sleep 2
done

echo ""
if [[ "$REMOTE_RUN" == "true" ]]; then
  require_active_branch
  if push_branch_if_ahead "$BRANCH_NAME"; then
    ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" true
  fi
fi
echo "Ralph reached max iterations ($MAX_ITERATIONS) without completing all tasks."
echo "Check $PROGRESS_FILE for status."
exit 1
