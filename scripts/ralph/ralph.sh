#!/bin/bash
# Ralph Wiggum - Long-running AI agent loop
# Usage: ./ralph.sh [--tool amp|claude|gemini|codex] [--review-tool amp|claude|gemini|codex] [--codex] [--remote-run] [--pr] [--download] [max_iterations]
#   --tool           AI engine for coding iterations (default: amp)
#   --review-tool    AI engine for code review step (default: same as --tool)
#   --codex          shortcut: codex codes, claude reviews (--tool codex --review-tool claude)
#   --remote-run     CI-first mode: no local heavy checks; draft PR + per-story commit/push + wait CI handled by agent prompt
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

# Avoid interactive git credential prompts in automation
export GIT_TERMINAL_PROMPT=0
PROGRESS_FILE="$SCRIPT_DIR/progress.txt"
ARCHIVE_DIR="$SCRIPT_DIR/archive"
LAST_BRANCH_FILE="$SCRIPT_DIR/.last-branch"

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

# Validate prd.json exists and has pending stories
if [ ! -f "$PRD_FILE" ]; then
  echo "Error: scripts/ralph/prd.json not found. Create it before running Ralph."
  exit 1
fi
if ! jq -e '.userStories[] | select(.passes == false)' "$PRD_FILE" > /dev/null 2>&1; then
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

if [[ "$REMOTE_RUN" == "true" ]]; then
  echo "Mode: --remote-run (CI-first, local heavy checks disabled)"
  if ! command -v gh >/dev/null 2>&1; then
    echo "Error: --remote-run requires GitHub CLI (gh)"
    exit 1
  fi
  if ! gh auth status >/dev/null 2>&1; then
    echo "Error: --remote-run requires authenticated gh CLI (run: gh auth login)"
    exit 1
  fi
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
    local branch
    branch=$(jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null)
    local pr_json failed_checks
    pr_json=$(gh pr view "$branch" --json mergeable,mergeStateStatus,statusCheckRollup,url 2>/dev/null || echo '{}')
    failed_checks=$(echo "$pr_json" | jq -r '.statusCheckRollup[]? | select((.conclusion // "") == "FAILURE") | "- \(.name // "check") :: \(.detailsUrl // "")"')
    [[ -z "$failed_checks" ]] && failed_checks="- none"

    prompt_file=$(mktemp)
    cat "$SCRIPT_DIR/CLAUDE.md" > "$prompt_file"
    cat >> "$prompt_file" <<EOF

## REMOTE-RUN STRICT OVERRIDE (generated by ralph.sh)
- You are in CI-first mode. DO NOT run any local ./gradlew commands.
- Use GitHub CI as the only source of pass/fail.
- If CI is failing, inspect failing jobs/logs and fix accordingly.
- Current PR status snapshot:
$(echo "$pr_json" | jq -c '.')
- Failing checks snapshot:
$failed_checks
EOF
  fi

  echo "$prompt_file"
}

for i in $(seq 1 $MAX_ITERATIONS); do
  echo ""
  echo "==============================================================="
  echo "  Ralph Iteration $i of $MAX_ITERATIONS ($TOOL)"
  echo "==============================================================="

  # Run the selected tool with the ralph prompt
  AGENT_PROMPT_FILE=$(build_agent_prompt)
  if [[ "$TOOL" == "amp" ]]; then
    OUTPUT=$(cat "$SCRIPT_DIR/prompt.md" | amp --dangerously-allow-all 2>&1 | tee /dev/stderr) || true
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
  
  # Check for completion signal
  if echo "$OUTPUT" | grep -q "<promise>COMPLETE</promise>"; then
    echo ""
    echo "Ralph completed all tasks!"
    echo "Completed at iteration $i of $MAX_ITERATIONS"

    # Create PR if --pr flag was passed
    if [[ "$CREATE_PR" == "true" ]]; then
      BRANCH=$(jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null)
      DESCRIPTION=$(jq -r '.description // "Ralph auto-PR"' "$PRD_FILE" 2>/dev/null)
      STORY_SUMMARY=$(jq -r '.userStories[] | "- [x] \(.id): \(.title)"' "$PRD_FILE" 2>/dev/null)

      echo ""
      echo "==============================================================="
      echo "  Creating PR for branch: $BRANCH"
      echo "==============================================================="

      # Handle dirty working tree before PR operations (default: auto-story)
      if [[ -n "$(git status --porcelain)" ]]; then
        echo "WARN: Working tree has uncommitted changes before PR."
        git status --short
        echo "Auto-story mode: staging and committing remaining changes before PR..."
        git add -A
        if ! git diff --cached --quiet; then
          if ! git commit -m "chore(ralph): auto-commit remaining story-related changes before PR"; then
            echo "ERROR: Auto-story commit failed. Resolve conflicts/issues and re-run."
            exit 1
          fi
        fi
      fi

      # Push branch (non-interactive)
      if ! git push -u origin "$BRANCH"; then
        echo "ERROR: git push failed (non-interactive mode). Check gh auth / git credentials."
        exit 1
      fi

      # Create PR or reuse existing
      EXISTING_PR=$(gh pr view "$BRANCH" --json url -q '.url' 2>/dev/null || echo "")
      if [ -n "$EXISTING_PR" ]; then
        PR_URL="$EXISTING_PR"
        echo "PR already exists: $PR_URL"
      else
        PR_URL=$(gh pr create \
          --title "$DESCRIPTION" \
          --body "$(cat <<EOF
## Summary
$DESCRIPTION

## Completed Stories
$STORY_SUMMARY

## Progress Log
<details>
<summary>Click to expand</summary>

$(cat "$PROGRESS_FILE")

</details>
EOF
)" 2>&1)
        echo "PR created: $PR_URL"
      fi

      # ── Code Review Loop (max 2 attempts) ──
      MAX_REVIEWS=2
      REVIEW_CLEAN=false
      for r in $(seq 1 $MAX_REVIEWS); do
        echo ""
        echo "==============================================================="
        echo "  Code Review — attempt $r of $MAX_REVIEWS ($REVIEW_TOOL)"
        echo "==============================================================="

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

        if echo "$REVIEW_OUTPUT" | grep -q "<review>CLEAN</review>"; then
          echo "Code review passed — no issues found."
          REVIEW_CLEAN=true
          break
        elif echo "$REVIEW_OUTPUT" | grep -q "<review>FIXED</review>"; then
          echo "Code review found and fixed issues. Pushing..."
          git push origin "$BRANCH"
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
      fi

      # Check for merge conflicts before waiting for CI
      echo ""
      echo "Checking for merge conflicts..."
      MERGEABLE=$(gh pr view "$BRANCH" --json mergeable -q '.mergeable' 2>/dev/null || echo "UNKNOWN")
      if [[ "$MERGEABLE" == "CONFLICTING" ]]; then
        echo "ERROR: PR has merge conflicts — CI will not run until resolved."
        echo "Resolve conflicts and re-run Ralph: $PR_URL"
        exit 1
      fi

      # Wait for CI checks (lint, tests)
      echo ""
      echo "Waiting for CI checks..."
      # gh CLI (Mar 2026) supports --watch/--required; --fail-fast is not available
      if gh pr checks "$BRANCH" --watch --required 2>&1; then
        echo "CI passed!"
      else
        echo "CI failed. Check PR for details: $PR_URL"
      fi

      # Trigger QA Build only for final, review-passed state
      ALL_PASSED=$(jq -r '[.userStories[] | select(.passes==false)] | length' "$PRD_FILE" 2>/dev/null)
      if [[ "$REVIEW_CLEAN" == "true" && "$ALL_PASSED" == "0" ]]; then
        echo ""
        echo "Triggering QA Build..."
        gh workflow run "QA Build" --ref "$BRANCH"
        sleep 5
      else
        echo ""
        echo "Skipping QA Build (not final or review not clean)."
      fi

      # Wait for QA Build to complete (if triggered)
      RUN_ID=$(gh run list --branch "$BRANCH" --workflow "QA Build" --limit 1 --json databaseId,status,createdAt -q '.[0].databaseId' 2>/dev/null)
      if [[ "$REVIEW_CLEAN" == "true" && "$ALL_PASSED" == "0" ]] && [ -n "$RUN_ID" ] && [ "$RUN_ID" != "null" ]; then
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
        CI_STATUS="passed"
        STORIES_JSON=$(jq '[.userStories[] | {id: .id, title: .title}]' "$PRD_FILE")
        jq -n \
          --arg branch "$BRANCH" \
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
echo "Ralph reached max iterations ($MAX_ITERATIONS) without completing all tasks."
echo "Check $PROGRESS_FILE for status."
exit 1
