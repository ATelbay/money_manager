#!/bin/bash
# Ralph Wiggum - Long-running AI agent loop
# Usage: ./ralph.sh [--tool amp|claude|gemini|codex] [--review-tool amp|claude|gemini|codex] [--codex] [--remote-run] [--pr] [--download] [max_iterations]
#   --tool           AI engine for coding iterations (default: amp)
#   --review-tool    AI engine for code review step (default: same as --tool)
#   --codex          shortcut: codex codes, claude reviews (--tool codex --review-tool claude)
#   --remote-run     Shell-managed remote mode: no local heavy checks; Ralph keeps the PR in draft and syncs each story via CI-first or deferred mode
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
source "$SCRIPT_DIR/lib/ralph_prd.sh"
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
REMOTE_PR_DRAFT_MODE="${RALPH_REMOTE_PR_DRAFT_MODE:-true}"
REMOTE_RUN_MODE=""

if ! REMOTE_RUN_MODE=$(ralph_resolve_remote_run_mode); then
  exit 1
fi

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

count_todo_stories() {
  ralph_count_todo_stories "$PRD_FILE"
}

count_implemented_stories() {
  ralph_count_implemented_stories "$PRD_FILE"
}

count_passed_stories() {
  ralph_count_passed_stories "$PRD_FILE"
}

count_remaining_stories() {
  ralph_count_remaining_stories "$PRD_FILE"
}

next_todo_story_json() {
  ralph_next_todo_story_json "$PRD_FILE"
}

build_story_summary() {
  ralph_build_story_summary "$PRD_FILE"
}

story_status() {
  local story_id="$1"
  ralph_story_status "$PRD_FILE" "$story_id"
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

ensure_prd_branch_checked_out() {
  local active_branch
  active_branch=$(git branch --show-current 2>/dev/null || echo "")
  if [[ "$active_branch" == "$BRANCH_NAME" ]]; then
    return 0
  fi

  echo "Switching to PRD branch: $BRANCH_NAME"

  if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    if ! git checkout "$BRANCH_NAME"; then
      echo "ERROR: Failed to checkout local branch '$BRANCH_NAME'."
      exit 1
    fi
    return 0
  fi

  if git ls-remote --exit-code --heads origin "$BRANCH_NAME" >/dev/null 2>&1; then
    if ! git fetch origin "$BRANCH_NAME:$BRANCH_NAME"; then
      echo "ERROR: Failed to fetch remote branch '$BRANCH_NAME'."
      exit 1
    fi
    if ! git checkout "$BRANCH_NAME"; then
      echo "ERROR: Failed to checkout fetched branch '$BRANCH_NAME'."
      exit 1
    fi
    return 0
  fi

  if ! git show-ref --verify --quiet "refs/heads/$TARGET_BASE_BRANCH"; then
    git fetch origin "$TARGET_BASE_BRANCH:$TARGET_BASE_BRANCH" >/dev/null 2>&1 || true
  fi

  if git show-ref --verify --quiet "refs/heads/$TARGET_BASE_BRANCH"; then
    if ! git checkout -B "$BRANCH_NAME" "$TARGET_BASE_BRANCH"; then
      echo "ERROR: Failed to create '$BRANCH_NAME' from '$TARGET_BASE_BRANCH'."
      exit 1
    fi
  else
    if ! git checkout -B "$BRANCH_NAME"; then
      echo "ERROR: Failed to create '$BRANCH_NAME'."
      exit 1
    fi
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

fail_remote_run_on_dirty_worktree() {
  local reason="${1:-Dirty working tree detected in --remote-run}"

  echo "ERROR: $reason"
  echo "Refusing to discard uncommitted changes while --remote-run is active."
  echo "Commit or stash the worktree, then rerun Ralph."
  filtered_git_status
  exit 1
}

git_rebase_in_progress() {
  local rebase_merge rebase_apply
  rebase_merge=$(git rev-parse --git-path rebase-merge 2>/dev/null || echo ".git/rebase-merge")
  rebase_apply=$(git rev-parse --git-path rebase-apply 2>/dev/null || echo ".git/rebase-apply")
  [[ -d "$rebase_merge" || -d "$rebase_apply" ]]
}

unmerged_conflict_files() {
  git diff --name-only --diff-filter=U 2>/dev/null || true
}

remote_branch_exists() {
  local branch="$1"
  git show-ref --verify --quiet "refs/remotes/origin/$branch"
}

branch_includes_ref() {
  local ref="$1"
  git merge-base --is-ancestor "$ref" HEAD >/dev/null 2>&1
}

run_sync_repair_prompt() {
  local story_id="$1"
  local story_title="$2"
  local upstream="$3"
  local reason="$4"
  local attempt="$5"
  local max_attempts="$6"
  local prompt_file
  local conflicted_files
  local status_snapshot

  conflicted_files=$(unmerged_conflict_files)
  status_snapshot=$(git status --porcelain 2>/dev/null || true)

  prompt_file=$(mktemp)
  cat "$SCRIPT_DIR/CLAUDE.md" > "$prompt_file"
  cat >> "$prompt_file" <<EOF

## REMOTE-RUN SYNC REPAIR OVERRIDE (generated by ralph.sh)
- Remote-run mode: $REMOTE_RUN_MODE
- Active story: $story_id — $story_title
- Repair reason: $reason
- Rebase target: $upstream
- Repair attempt: ${attempt}/${max_attempts}
- A git rebase is already in progress. Resolve the current conflict only.
- Do NOT start a new story, do NOT edit prd.json status flags, and do NOT create any commits.
- Do NOT run git rebase, git reset, git cherry-pick, git commit --amend, or force-push/history-rewrite commands.
- Resolve the conflicted files, stage the resolutions, and leave the repository ready for Ralph shell to run git rebase --continue.
- Current git status:
$status_snapshot
- Conflicted files:
${conflicted_files:-<none>}
EOF

  run_tool_with_prompt_file "$TOOL" "$prompt_file" >/dev/null || true
  rm -f "$prompt_file"
}

rebase_onto_ref_with_sync_repair() {
  local upstream="$1"
  local story_id="$2"
  local story_title="$3"
  local reason="$4"
  local max_attempts="${RALPH_REMOTE_SYNC_REPAIR_ATTEMPTS:-3}"
  local attempt=1

  if git rebase "$upstream"; then
    return 0
  fi

  echo "WARN: Rebase onto $upstream failed during $reason. Entering sync repair loop..."

  while git_rebase_in_progress; do
    if (( attempt > max_attempts )); then
      echo "ERROR: Unable to finish rebase onto $upstream after ${max_attempts} sync-repair attempts."
      git rebase --abort >/dev/null 2>&1 || true
      exit 1
    fi

    run_sync_repair_prompt "$story_id" "$story_title" "$upstream" "$reason" "$attempt" "$max_attempts"

    if [[ -n "$(unmerged_conflict_files)" ]]; then
      echo "WARN: Rebase still has unresolved conflicts after repair attempt ${attempt}/${max_attempts}."
      ((attempt++))
      continue
    fi

    if GIT_EDITOR=true git rebase --continue; then
      if ! git_rebase_in_progress; then
        return 0
      fi
    else
      if [[ -n "$(unmerged_conflict_files)" ]]; then
        echo "WARN: Rebase produced another conflict set after attempt ${attempt}/${max_attempts}."
      else
        echo "WARN: git rebase --continue failed during $reason. Resolve the staged state in the next repair attempt."
      fi
      ((attempt++))
      continue
    fi
  done
}

sync_local_branch_from_remote_if_behind() {
  if [[ "$REMOTE_RUN" != "true" ]]; then
    return 0
  fi

  require_active_branch

  if ! git fetch origin "$TARGET_BASE_BRANCH"; then
    echo "ERROR: Failed to fetch origin/$TARGET_BASE_BRANCH for --remote-run sync."
    exit 1
  fi
  git fetch origin "$BRANCH_NAME" >/dev/null 2>&1 || true

  if ! remote_branch_exists "$BRANCH_NAME"; then
    return 0
  fi

  if working_tree_has_user_changes; then
    fail_remote_run_on_dirty_worktree "Dirty working tree detected before --remote-run startup"
  fi

  if git merge-base --is-ancestor HEAD "origin/$BRANCH_NAME" >/dev/null 2>&1 && \
     ! git merge-base --is-ancestor "origin/$BRANCH_NAME" HEAD >/dev/null 2>&1; then
    echo "Fast-forwarding local '$BRANCH_NAME' to origin/$BRANCH_NAME before --remote-run..."
    if ! git merge --ff-only "origin/$BRANCH_NAME"; then
      echo "ERROR: Failed to fast-forward '$BRANCH_NAME' to origin/$BRANCH_NAME."
      exit 1
    fi
    return 0
  fi

  if ! git merge-base --is-ancestor "origin/$BRANCH_NAME" HEAD >/dev/null 2>&1; then
    echo "WARN: Local '$BRANCH_NAME' diverged from origin/$BRANCH_NAME. Keeping local commits and relying on story-specific push recovery if needed."
  fi
}

prepare_remote_run_branch() {
  local story_id="$1"
  local story_title="$2"

  if [[ "$REMOTE_RUN" != "true" ]]; then
    return 0
  fi

  require_active_branch
  sync_local_branch_from_remote_if_behind

  if working_tree_has_user_changes; then
    fail_remote_run_on_dirty_worktree "Dirty working tree detected before preparing --remote-run branch"
  fi

  if ! branch_includes_ref "origin/$TARGET_BASE_BRANCH"; then
    echo "Rebasing '$BRANCH_NAME' onto origin/$TARGET_BASE_BRANCH before remote iteration..."
    rebase_onto_ref_with_sync_repair "origin/$TARGET_BASE_BRANCH" "$story_id" "$story_title" "pre-agent freshness sync"
  fi
}

push_branch() {
  local branch="$1"
  local push_output=""

  echo "Pushing branch: $branch"
  if push_output=$(git push -u origin "$branch" 2>&1); then
    [[ -n "$push_output" ]] && echo "$push_output"
    return 0
  fi

  [[ -n "$push_output" ]] && echo "$push_output"

  echo "ERROR: git push failed (non-interactive mode). Check gh auth / git credentials."
  exit 1
}

push_remote_story_branch() {
  local branch="$1"
  local story_id="$2"
  local story_title="$3"
  local original_subjects="${4:-}"
  local push_output=""

  echo "Pushing branch: $branch"
  if push_output=$(git push -u origin "$branch" 2>&1); then
    [[ -n "$push_output" ]] && echo "$push_output"
    return 0
  fi

  [[ -n "$push_output" ]] && echo "$push_output"

  if ! grep -qiE 'non-fast-forward|fetch first|failed to push some refs|\[rejected\]' <<<"$push_output"; then
    echo "ERROR: git push failed (non-interactive mode). Check gh auth / git credentials."
    exit 1
  fi

  echo "Push rejected (non-fast-forward). Replaying local story commits onto origin/$branch..."
  if ! git fetch origin "$TARGET_BASE_BRANCH"; then
    echo "ERROR: Auto-recovery fetch failed for origin/$TARGET_BASE_BRANCH."
    exit 1
  fi
  git fetch origin "$branch" >/dev/null 2>&1 || true

  if remote_branch_exists "$branch"; then
    rebase_onto_ref_with_sync_repair "origin/$branch" "$story_id" "$story_title" "remote branch recovery"
  fi

  if ! branch_includes_ref "origin/$TARGET_BASE_BRANCH"; then
    rebase_onto_ref_with_sync_repair "origin/$TARGET_BASE_BRANCH" "$story_id" "$story_title" "post-push freshness sync"
  fi

  if [[ -n "$original_subjects" ]]; then
    validate_synced_story_iteration "$story_id" "$original_subjects"
  fi

  if push_output=$(git push -u origin "$branch" 2>&1); then
    [[ -n "$push_output" ]] && echo "$push_output"
    return 0
  fi

  [[ -n "$push_output" ]] && echo "$push_output"
  echo "ERROR: git push still failed after remote-run recovery."
  exit 1
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

story_has_commit_in_history() {
  local story_id="$1"
  local subject

  while IFS= read -r subject; do
    if [[ "$subject" =~ ^feat:\ \[$story_id\]\ -\  ]]; then
      return 0
    fi
    if [[ "$subject" =~ ^feat:\ $story_id\ -\  ]]; then
      return 0
    fi
  done < <(git log --format=%s HEAD 2>/dev/null || true)

  return 1
}

story_commit_subjects_since() {
  local start_head="$1"

  if [[ -n "$start_head" ]]; then
    git log --format=%s "${start_head}..HEAD" 2>/dev/null || true
  else
    git log --format=%s HEAD -n 1 2>/dev/null || true
  fi
}

validate_story_subjects_for_active_story() {
  local story_id="$1"
  local subjects="$2"
  local other_story_commits=""
  local subject commit_story_id

  while IFS= read -r subject; do
    [[ -n "$subject" ]] || continue
    [[ "$subject" =~ ^feat:\  ]] || continue

    commit_story_id=""
    if [[ "$subject" =~ ^feat:\ \[([^]]+)\]\ -\  ]]; then
      commit_story_id="${BASH_REMATCH[1]}"
    elif [[ "$subject" =~ ^feat:\ ([A-Za-z]+-[0-9]+)\ -\  ]]; then
      commit_story_id="${BASH_REMATCH[1]}"
    fi

    if [[ -n "$commit_story_id" && "$commit_story_id" != "$story_id" ]]; then
      other_story_commits+="$subject"$'\n'
    fi
  done <<<"$subjects"

  if [[ -n "$other_story_commits" ]]; then
    echo "ERROR: --remote-run iteration targeted a different story while $story_id is active."
    printf '%s' "$other_story_commits"
    exit 1
  fi
}

validate_agent_remote_story_iteration() {
  local start_head="$1"
  local story_id="$2"
  local repair_mode="${3:-false}"
  local head_subject current_head subjects

  current_head=$(git rev-parse HEAD 2>/dev/null || echo "")

  if [[ -n "$start_head" ]] && ! git merge-base --is-ancestor "$start_head" "$current_head"; then
    echo "ERROR: --remote-run iteration rewrote branch history while processing $story_id."
    echo "Start HEAD: $start_head"
    echo "Current HEAD: $current_head"
    echo "History rewrites (rebase/reset/cherry-pick/amend) are not allowed during the agent step in --remote-run."
    exit 1
  fi

  subjects=$(story_commit_subjects_since "$start_head")
  validate_story_subjects_for_active_story "$story_id" "$subjects"

  head_subject=$(git log -1 --format=%s HEAD 2>/dev/null || echo "")
  if [[ "$repair_mode" != "true" ]]; then
    if [[ "$head_subject" =~ ^feat:\ \[$story_id\]\ -\  ]]; then
      :
    elif [[ "$head_subject" =~ ^feat:\ $story_id\ -\  ]]; then
      echo "WARN: Non-canonical commit subject format detected for $story_id; expected 'feat: [$story_id] - ...'."
    else
      echo "ERROR: Expected latest commit to target $story_id in --remote-run."
      echo "Latest commit: ${head_subject:-<none>}"
      exit 1
    fi
  fi
}

validate_synced_story_iteration() {
  local story_id="$1"
  local original_subjects="$2"
  local subject_count replayed_subjects

  validate_story_subjects_for_active_story "$story_id" "$original_subjects"

  subject_count=$(printf '%s\n' "$original_subjects" | awk 'NF { count++ } END { print count + 0 }')
  if [[ "$subject_count" == "0" ]]; then
    return 0
  fi

  replayed_subjects=$(git log --format=%s -n "$subject_count" HEAD 2>/dev/null || true)
  if [[ "$replayed_subjects" != "$original_subjects" ]]; then
    echo "ERROR: Shell-managed sync changed the active story commit sequence for $story_id."
    echo "Expected:"
    printf '%s\n' "$original_subjects"
    echo "Actual:"
    printf '%s\n' "$replayed_subjects"
    exit 1
  fi
}

set_story_status_staged() {
  local story_id="$1"
  local status="$2"

  if ! ralph_set_story_status_in_file "$PRD_FILE" "$story_id" "$status"; then
    echo "ERROR: Failed to mark $story_id as status '$status' in prd.json."
    exit 1
  fi

  git add "$PRD_FILE"
}

mark_story_status_locally() {
  local story_id="$1"
  local status="$2"
  local commit_message="$3"

  set_story_status_staged "$story_id" "$status"

  if git diff --cached --quiet; then
    return 0
  fi

  if ! git commit -m "$commit_message"; then
    echo "ERROR: Failed to commit status marker for $story_id."
    exit 1
  fi
}

mark_story_implemented_locally() {
  local story_id="$1"
  mark_story_status_locally "$story_id" "implemented" "chore(ralph): mark $story_id as implemented"
}

mark_story_passed_locally() {
  local story_id="$1"
  mark_story_status_locally "$story_id" "passed" "fix(ralph): mark $story_id as passed"
}

mark_all_implemented_stories_passed_locally() {
  if ! ralph_promote_implemented_to_passed_in_file "$PRD_FILE"; then
    echo "ERROR: Failed to promote implemented stories to passed in prd.json."
    exit 1
  fi

  git add "$PRD_FILE"
  if git diff --cached --quiet; then
    return 0
  fi

  if ! git commit -m "chore(ralph): finalize implemented stories as passed"; then
    echo "ERROR: Failed to commit final implemented->passed promotion."
    exit 1
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

run_tool_with_prompt_file() {
  local tool="$1"
  local prompt_file="$2"

  if [[ "$tool" == "claude" ]]; then
    claude --dangerously-skip-permissions --print < "$prompt_file" 2>&1 | tee /dev/stderr
  elif [[ "$tool" == "gemini" ]]; then
    cat "$prompt_file" | gemini --auto 2>&1 | tee /dev/stderr
  elif [[ "$tool" == "amp" ]]; then
    cat "$prompt_file" | amp --dangerously-allow-all 2>&1 | tee /dev/stderr
  elif [[ "$tool" == "codex" ]]; then
    codex exec --dangerously-bypass-approvals-and-sandbox - < "$prompt_file" 2>&1 | tee /dev/stderr
  else
    echo "ERROR: Unsupported tool '$tool' in run_tool_with_prompt_file."
    return 1
  fi
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
    local todo_story_json
    todo_story_json=$(next_todo_story_json)
    REMOTE_STORY_ID=$(jq -r '.id // empty' <<<"$todo_story_json")
    REMOTE_STORY_TITLE=$(jq -r '.title // empty' <<<"$todo_story_json")
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
    if [[ "$draft" == "true" ]]; then
      gh pr ready --undo "$branch" >/dev/null 2>&1 || true
    else
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
  local snapshot checks_output checks_exit
  local effective_pr_url="$pr_url"
  local max_attempts="${RALPH_CHECKS_BOOTSTRAP_RETRIES:-12}"
  local sleep_seconds="${RALPH_CHECKS_BOOTSTRAP_SLEEP_SEC:-10}"
  local attempt=1
  local check_count=0
  local incomplete_count=0

  CHECKS_LAST_SNAPSHOT="{}"
  CHECKS_LAST_FAILURE_SUMMARY="- none"

  while (( attempt <= max_attempts )); do
    echo ""
    echo "Waiting for required CI checks... (attempt $attempt/$max_attempts)"

    checks_output=$(gh pr checks "$branch" --watch --required 2>&1)
    checks_exit=$?
    [[ -n "$checks_output" ]] && echo "$checks_output"

    if [[ "$checks_exit" -eq 0 ]]; then
      echo "CI passed!"
      return 0
    fi

    if ! snapshot=$(gh pr view "$branch" --json url,statusCheckRollup 2>/dev/null); then
      if (( attempt == max_attempts )); then
        echo "ERROR: gh pr checks failed and Ralph could not read PR status afterwards."
        echo "Treating this as a GitHub CLI/API issue, not a code failure."
        return 2
      fi
      echo "WARN: Unable to read PR status snapshot; retrying in ${sleep_seconds}s..."
      sleep "$sleep_seconds"
      ((attempt++))
      continue
    fi

    CHECKS_LAST_SNAPSHOT="$snapshot"
    CHECKS_LAST_FAILURE_SUMMARY=$(extract_failed_checks "$snapshot")

    if [[ -z "$effective_pr_url" ]]; then
      effective_pr_url=$(jq -r '.url // empty' <<<"$snapshot")
    fi

    if [[ "$CHECKS_LAST_FAILURE_SUMMARY" != "- none" ]]; then
      if [[ -n "$effective_pr_url" ]]; then
        echo "CI failed. Check PR for details: $effective_pr_url"
      else
        echo "CI failed."
      fi
      return 1
    fi

    check_count=$(jq -r '[.statusCheckRollup[]?] | length' <<<"$snapshot")
    incomplete_count=$(jq -r '
      [
        .statusCheckRollup[]?
        | if .status? != null then
            ((.status | ascii_upcase) != "COMPLETED")
          elif .state? != null then
            ((.state | ascii_upcase) == "PENDING" or (.state | ascii_upcase) == "EXPECTED" or (.state | ascii_upcase) == "IN_PROGRESS")
          else
            false
          end
        | select(. == true)
      ]
      | length
    ' <<<"$snapshot")

    if [[ "$check_count" -gt 0 && "$incomplete_count" -eq 0 ]]; then
      echo "CI passed! (all reported checks completed successfully)"
      return 0
    fi

    if (( attempt == max_attempts )); then
      echo "ERROR: Timed out while waiting for required checks to appear/settle."
      echo "Treating this as a GitHub CLI/API issue, not a code failure."
      return 2
    fi

    if [[ "$check_count" -eq 0 ]]; then
      echo "No checks reported yet; retrying in ${sleep_seconds}s..."
    else
      echo "Checks still running (${incomplete_count} unfinished); retrying in ${sleep_seconds}s..."
    fi

    sleep "$sleep_seconds"
    ((attempt++))
  done

  echo "ERROR: Unexpected CI wait loop termination."
  return 2
}

wait_required_checks_with_transient_retries() {
  local branch="$1"
  local pr_url="${2:-}"
  local wait_status

  wait_required_checks "$branch" "$pr_url"
  wait_status=$?

  if [[ "$wait_status" -eq 2 ]]; then
    local transient_retries="${RALPH_CHECKS_TRANSIENT_RETRIES:-3}"
    local transient_sleep="${RALPH_CHECKS_TRANSIENT_SLEEP_SEC:-15}"
    local transient_attempt=1

    while [[ "$wait_status" -eq 2 && "$transient_attempt" -le "$transient_retries" ]]; do
      echo "WARN: CI status unavailable (transient GitHub/API state). Retry ${transient_attempt}/${transient_retries} in ${transient_sleep}s..."
      sleep "$transient_sleep"
      wait_required_checks "$branch" "$pr_url"
      wait_status=$?
      ((transient_attempt++))
    done
  fi

  return "$wait_status"
}

run_final_ci_with_auto_repair() {
  local branch="$1"
  local pr_url="$2"
  local max_attempts="${RALPH_FINAL_CI_REPAIR_ATTEMPTS:-3}"
  local repair_attempt=1
  local wait_status
  local failed_checks prompt_file review_output
  local before_sha after_sha

  while true; do
    wait_required_checks_with_transient_retries "$branch" "$pr_url"
    wait_status=$?

    if [[ "$wait_status" -eq 0 ]]; then
      return 0
    fi

    if [[ "$wait_status" -eq 2 ]]; then
      echo "ERROR: Unable to determine final CI state after retries due to GitHub CLI/API instability."
      return 2
    fi

    if (( repair_attempt > max_attempts )); then
      echo "ERROR: Final CI is still failing after ${max_attempts} auto-repair attempt(s)."
      return 1
    fi

    failed_checks="${CHECKS_LAST_FAILURE_SUMMARY:-- none}"
    echo "Final CI failed. Starting auto-repair attempt ${repair_attempt}/${max_attempts}."
    echo "Failing checks:"
    printf '%s\n' "$failed_checks"

    prompt_file=$(mktemp)
    cat "$SCRIPT_DIR/review-prompt.md" > "$prompt_file"
    cat >> "$prompt_file" <<EOF

## FINAL CI AUTO-REPAIR (generated by ralph.sh)
- PR URL: $pr_url
- Branch: $branch
- Auto-repair attempt: ${repair_attempt}/${max_attempts}
- Failing checks summary:
$failed_checks

Fix the underlying failures and make real code/test updates if needed.
When done, output one tag:
- <review>FIXED</review> if you made changes
- <review>CLEAN</review> if no changes are needed
EOF

    before_sha=$(git rev-parse HEAD 2>/dev/null || echo "")
    review_output=$(run_tool_with_prompt_file "$REVIEW_TOOL" "$prompt_file") || true
    rm -f "$prompt_file"

    commit_review_feedback_if_needed || true
    after_sha=$(git rev-parse HEAD 2>/dev/null || echo "")

    if [[ "$after_sha" == "$before_sha" ]]; then
      if echo "$review_output" | grep -Eq "<review>(CLEAN|NO_CHANGES_NEEDED)</review>"; then
        echo "ERROR: Reviewer reports CLEAN but CI is still failing; cannot auto-repair further."
      else
        echo "ERROR: Auto-repair attempt produced no commit."
      fi
      return 1
    fi

    echo "Auto-repair produced commit $after_sha. Pushing and re-running CI checks..."
    push_branch "$branch"
    ensure_pr_exists "$branch" "$RUN_DESCRIPTION" "$(build_story_summary)" false

    ((repair_attempt++))
  done
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
  local start_todo="$2"
  local story_id="$3"
  local story_title="$4"
  local end_head end_todo
  local snapshot failed_checks attempts wait_status original_subjects current_status

  REMOTE_CI_PASSED=false
  REMOTE_CI_FAILED=false

  require_active_branch

  if working_tree_has_user_changes; then
    fail_remote_run_on_dirty_worktree "--remote-run iteration left uncommitted changes"
  fi

  end_head=$(git rev-parse HEAD 2>/dev/null || echo "")
  end_todo=$(count_todo_stories)

  if [[ "$end_head" == "$start_head" && "$end_todo" == "$start_todo" ]]; then
    current_status=$(story_status "$story_id")
    if [[ "$REMOTE_RUN_MODE" == "deferred" && "$current_status" == "todo" && -n "$story_id" ]] && story_has_commit_in_history "$story_id"; then
      echo "No new commit this iteration, but found existing feat commit for $story_id in branch history."
      push_remote_story_branch "$BRANCH_NAME" "$story_id" "$story_title"
      mark_story_implemented_locally "$story_id"
      ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" "$REMOTE_PR_DRAFT_MODE"
      clear_remote_repair_state
      REMOTE_CI_PASSED=true
      echo "Backfilled implemented status for $story_id from existing branch history (deferred mode)."
      return 0
    fi

    echo "No committed progress detected in this iteration; skipping PR/CI sync."
    return 0
  fi

  validate_agent_remote_story_iteration "$start_head" "$story_id" "$REMOTE_REPAIR_MODE"
  original_subjects=$(story_commit_subjects_since "$start_head")

  push_remote_story_branch "$BRANCH_NAME" "$story_id" "$story_title" "$original_subjects"
  validate_synced_story_iteration "$story_id" "$original_subjects"

  if [[ "$REMOTE_RUN_MODE" == "deferred" ]]; then
    mark_story_implemented_locally "$story_id"
    ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" "$REMOTE_PR_DRAFT_MODE"
    clear_remote_repair_state
    REMOTE_CI_PASSED=true
    echo "Deferred CI mode: pushed story commit for $story_id and marked it implemented locally. Final PR CI will promote implemented stories to passed."
    return 0
  fi

  ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" "$REMOTE_PR_DRAFT_MODE"

  wait_required_checks_with_transient_retries "$BRANCH_NAME" "$PR_URL"
  wait_status=$?

  if [[ "$wait_status" -eq 2 ]]; then
    echo "ERROR: Unable to determine CI state after retries due to GitHub CLI/API instability."
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
  ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" "$REMOTE_PR_DRAFT_MODE"
  REMOTE_CI_PASSED=true
  echo "CI passed for $story_id. Marked it passed locally; the status commit will sync on the next branch push."
}

ensure_prd_branch_checked_out
sync_local_branch_from_remote_if_behind

INITIAL_TODO_STORIES=$(count_todo_stories)
INITIAL_IMPLEMENTED_STORIES=$(count_implemented_stories)
INITIAL_REMAINING_STORIES=$(count_remaining_stories)
if [[ "$REMOTE_RUN" != "true" && -f "$REMOTE_STATE_FILE" ]]; then
  clear_remote_repair_state
fi
if [[ "$INITIAL_REMAINING_STORIES" == "0" && -f "$REMOTE_STATE_FILE" ]]; then
  clear_remote_repair_state
fi

if [[ "$INITIAL_REMAINING_STORIES" == "0" ]]; then
  if [[ "$CREATE_PR" == "false" && "$DOWNLOAD_APK" == "false" ]]; then
    echo "Nothing to do: all stories in prd.json already have status=passed."
    exit 0
  fi
  echo "All stories already passed — skipping iterations, proceeding to PR/download flow."
elif [[ "$REMOTE_RUN" == "true" && "$REMOTE_RUN_MODE" == "deferred" && "$INITIAL_TODO_STORIES" == "0" && "$INITIAL_IMPLEMENTED_STORIES" != "0" ]]; then
  echo "All stories are implemented locally — skipping coding iterations and proceeding to final PR/review/CI flow."
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
  echo "Mode: --remote-run ($REMOTE_RUN_MODE, draft PR flow, local heavy checks disabled)"
else
  # Warm up Gradle daemon before iterations so each coding step compiles in 1-3 min
  echo ""
  echo "Warming up Gradle daemon (compileDebugKotlin)..."
  ./gradlew compileDebugKotlin --quiet 2>&1 | tail -3 || true
  echo "Gradle daemon ready."
fi

export RALPH_REMOTE_RUN="$REMOTE_RUN"
export RALPH_CREATE_PR="$CREATE_PR"
export RALPH_REMOTE_RUN_MODE="$REMOTE_RUN_MODE"

build_agent_prompt() {
  local prompt_file="$SCRIPT_DIR/CLAUDE.md"

  if [[ "$REMOTE_RUN" == "true" ]]; then
    local pr_json failed_checks remote_mode_note
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

    if [[ "$REMOTE_RUN_MODE" == "deferred" ]]; then
      remote_mode_note="Ralph shell will mark the story as status=implemented after this iteration. Final PR CI will later promote implemented stories to status=passed."
    else
      remote_mode_note="Ralph shell will wait for required CI after this iteration and only mark the story as status=passed after green CI."
    fi

    prompt_file=$(mktemp)
    cat "$SCRIPT_DIR/CLAUDE.md" > "$prompt_file"
    cat >> "$prompt_file" <<EOF

## REMOTE-RUN STRICT OVERRIDE (generated by ralph.sh)
- Remote-run mode: $REMOTE_RUN_MODE
- DO NOT run any local ./gradlew commands.
- Work only on story: $REMOTE_STORY_ID — $REMOTE_STORY_TITLE
- Implement exactly one story, commit it locally, and update progress.txt.
- Do NOT set status or passes in prd.json; Ralph shell manages story state transitions.
- Ralph shell will handle git push and PR creation/reuse.
- $remote_mode_note
- Do NOT run gh pr checks, gh workflow run, or any long-lived CI waiting commands yourself.
- Do NOT run git rebase, git reset, git cherry-pick, git commit --amend, or any force-push/history-rewrite commands.
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

  AGENT_COMPLETE=false

  if [[ "$REMOTE_RUN" == "true" ]]; then
    sync_local_branch_from_remote_if_behind
    load_remote_story_context
    if [[ -z "$REMOTE_STORY_ID" && ! -f "$REMOTE_STATE_FILE" ]]; then
      AGENT_COMPLETE=true
    elif [[ -n "$REMOTE_STORY_ID" ]]; then
      prepare_remote_run_branch "$REMOTE_STORY_ID" "$REMOTE_STORY_TITLE"
    fi
  fi

  ITERATION_START_HEAD=$(git rev-parse HEAD 2>/dev/null || echo "")
  ITERATION_START_PENDING=$(count_todo_stories)

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
      REMAINING_STORIES=$(count_remaining_stories)

      if [[ "$REMAINING_STORIES" != "0" ]]; then
        echo ""
        echo "WARNING: Agent signaled COMPLETE but $REMAINING_STORIES stories still do not have status=passed. Ignoring completion signal."
      else
        AGENT_COMPLETE=true
      fi
    fi
  fi

  if [[ "$REMOTE_RUN" == "true" && "$AGENT_COMPLETE" == "false" ]]; then
    sync_remote_run_iteration "$ITERATION_START_HEAD" "$ITERATION_START_PENDING" "$REMOTE_STORY_ID" "$REMOTE_STORY_TITLE"
    if [[ "$(count_todo_stories)" == "0" && ! -f "$REMOTE_STATE_FILE" ]]; then
      AGENT_COMPLETE=true
    fi
  fi

  if [[ "$AGENT_COMPLETE" == "true" ]]; then
    echo ""
    if [[ "$REMOTE_RUN" == "true" && "$REMOTE_RUN_MODE" == "deferred" && "$(count_remaining_stories)" != "0" ]]; then
      echo "Ralph completed implementation for all stories. Proceeding to final review/CI/promotion flow."
    else
      echo "Ralph completed all tasks!"
    fi
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
        REVIEW_OUTPUT=$(run_tool_with_prompt_file "$REVIEW_TOOL" "$SCRIPT_DIR/review-prompt.md") || true

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
      if run_final_ci_with_auto_repair "$BRANCH_NAME" "$PR_URL"; then
        CI_STATUS="passed"
      else
        echo "ERROR: Final CI gate did not pass after auto-repair attempts."
        exit 1
      fi

      if [[ "$REMOTE_RUN" == "true" && "$REMOTE_RUN_MODE" == "deferred" && "$(count_implemented_stories)" != "0" ]]; then
        echo ""
        echo "Deferred mode: promoting implemented stories to passed after green final CI..."
        mark_all_implemented_stories_passed_locally
        push_branch "$BRANCH_NAME"
        ensure_pr_exists "$BRANCH_NAME" "$RUN_DESCRIPTION" "$(build_story_summary)" false

        echo "Re-validating CI after syncing final story-status promotion..."
        if run_final_ci_with_auto_repair "$BRANCH_NAME" "$PR_URL"; then
          CI_STATUS="passed"
        else
          echo "ERROR: Final CI did not stay green after syncing passed story statuses."
          exit 1
        fi
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
