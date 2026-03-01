#!/bin/bash
# Ralph Wiggum - Long-running AI agent loop
# Usage: ./ralph.sh [--tool amp|claude|gemini] [--pr] [--download] [max_iterations]
#   --pr         push branch, create PR, code review, wait CI, trigger QA Build
#   --download   after QA Build: download APK locally (use on Mac; skip on VM — Garlic handles it)

# Parse arguments
TOOL="amp"  # Default to amp for backwards compatibility
MAX_ITERATIONS=10
CREATE_PR=false
DOWNLOAD_APK=false

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

# Validate tool choice
if [[ "$TOOL" != "amp" && "$TOOL" != "claude" && "$TOOL" != "gemini" ]]; then
  echo "Error: Invalid tool '$TOOL'. Must be 'amp', 'claude', or 'gemini'."
  exit 1
fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRD_FILE="$SCRIPT_DIR/prd.json"
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

echo "Starting Ralph - Tool: $TOOL - Max iterations: $MAX_ITERATIONS"

for i in $(seq 1 $MAX_ITERATIONS); do
  echo ""
  echo "==============================================================="
  echo "  Ralph Iteration $i of $MAX_ITERATIONS ($TOOL)"
  echo "==============================================================="

  # Run the selected tool with the ralph prompt
  if [[ "$TOOL" == "amp" ]]; then
    OUTPUT=$(cat "$SCRIPT_DIR/prompt.md" | amp --dangerously-allow-all 2>&1 | tee /dev/stderr) || true
  elif [[ "$TOOL" == "claude" ]]; then
    OUTPUT=$(claude --dangerously-skip-permissions --print < "$SCRIPT_DIR/CLAUDE.md" 2>&1 | tee /dev/stderr) || true
  elif [[ "$TOOL" == "gemini" ]]; then
    OUTPUT=$(cat "$SCRIPT_DIR/CLAUDE.md" | gemini --auto 2>&1 | tee /dev/stderr) || true
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

      # Push branch
      git push -u origin "$BRANCH"

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
        echo "  Code Review — attempt $r of $MAX_REVIEWS"
        echo "==============================================================="

        REVIEW_OUTPUT=""
        if [[ "$TOOL" == "claude" ]]; then
          REVIEW_OUTPUT=$(claude --dangerously-skip-permissions --print < "$SCRIPT_DIR/review-prompt.md" 2>&1 | tee /dev/stderr) || true
        elif [[ "$TOOL" == "gemini" ]]; then
          REVIEW_OUTPUT=$(cat "$SCRIPT_DIR/review-prompt.md" | gemini --auto 2>&1 | tee /dev/stderr) || true
        elif [[ "$TOOL" == "amp" ]]; then
          REVIEW_OUTPUT=$(cat "$SCRIPT_DIR/review-prompt.md" | amp --dangerously-allow-all 2>&1 | tee /dev/stderr) || true
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

      # Wait for CI checks (lint, tests)
      echo ""
      echo "Waiting for CI checks..."
      if gh pr checks "$BRANCH" --watch --fail-fast 2>&1; then
        echo "CI passed!"
      else
        echo "CI failed. Check PR for details: $PR_URL"
      fi

      # Trigger QA Build manually
      echo ""
      echo "Triggering QA Build..."
      gh workflow run "QA Build" --ref "$BRANCH"
      sleep 5

      # Wait for QA Build to complete
      RUN_ID=$(gh run list --branch "$BRANCH" --workflow "QA Build" --limit 1 --json databaseId,status -q '.[0].databaseId' 2>/dev/null)
      if [ -n "$RUN_ID" ] && [ "$RUN_ID" != "null" ]; then
        echo "QA Build run ID: $RUN_ID"

        if [[ "$DOWNLOAD_APK" == "true" ]]; then
          echo "Waiting for QA Build to finish..."
          gh run watch "$RUN_ID"

          # Cleanup old APKs, keep only last one
          find "$SCRIPT_DIR/artifacts/" -name "*.apk" 2>/dev/null | sort | head -n -1 | xargs rm -f 2>/dev/null || true

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
