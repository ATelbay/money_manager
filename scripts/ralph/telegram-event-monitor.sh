#!/bin/bash
set -euo pipefail

WATCH_MODE=false
RESET_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --watch)
      WATCH_MODE=true
      ;;
    --reset)
      RESET_ONLY=true
      ;;
    *)
      echo "Usage: $0 [--watch] [--reset]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/ralph_prd.sh"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PRD_FILE="$SCRIPT_DIR/prd.json"
PROGRESS_FILE="$SCRIPT_DIR/progress.txt"
RUN_SUMMARY_FILE="$SCRIPT_DIR/.run-summary.json"
QA_RUN_FILE="$SCRIPT_DIR/.qa-run-id"
GIT_DIR="$(git -C "$REPO_ROOT" rev-parse --git-dir 2>/dev/null || echo "$REPO_ROOT/.git")"
STATE_FILE="$GIT_DIR/ralph-telegram-monitor-state.json"

TARGET="${RALPH_TELEGRAM_TARGET:-377219158}"
POLL_SECONDS="${RALPH_NOTIFY_POLL_SECONDS:-45}"
DRY_RUN="${RALPH_NOTIFY_DRY_RUN:-false}"
QA_ARTIFACT_NAME="${RALPH_QA_ARTIFACT_NAME:-qa-debug-apk}"

if [[ "$RESET_ONLY" == "true" ]]; then
  rm -f "$STATE_FILE"
  exit 0
fi

send_message() {
  local text="$1"

  if [[ -z "$text" ]]; then
    return 0
  fi

  if [[ "$DRY_RUN" == "true" ]]; then
    printf '[DRY RUN] %s\n' "$text"
    return 0
  fi

  timeout 25s openclaw message send \
    --channel telegram \
    --target "$TARGET" \
    --message "$text" >/dev/null 2>&1 || {
    echo "WARN: failed to send Telegram notification" >&2
    return 0
  }
}

send_media() {
  local text="$1"
  local media_path="$2"

  if [[ -z "$media_path" || ! -f "$media_path" ]]; then
    echo "WARN: media file not found: $media_path" >&2
    return 1
  fi

  if [[ "$DRY_RUN" == "true" ]]; then
    printf '[DRY RUN] %s (media: %s)\n' "$text" "$media_path"
    return 0
  fi

  timeout 90s openclaw message send \
    --channel telegram \
    --target "$TARGET" \
    --message "$text" \
    --media "$media_path" >/dev/null 2>&1 || {
    echo "WARN: failed to send Telegram media notification" >&2
    return 1
  }
}

latest_non_empty_line() {
  if [[ ! -f "$PROGRESS_FILE" ]]; then
    printf ''
    return 0
  fi

  awk 'NF { line = $0 } END { print line }' "$PROGRESS_FILE" 2>/dev/null || true
}

detect_ralph_alive() {
  pgrep -af 'scripts/ralph/ralph\.sh' 2>/dev/null | grep -v 'telegram-event-monitor' >/dev/null 2>&1
}

story_total() {
  jq -r '.userStories | length' "$PRD_FILE" 2>/dev/null || echo "0"
}

todo_count() {
  ralph_count_todo_stories "$PRD_FILE"
}

implemented_ids_json() {
  jq -c '
    [
      .userStories[]?
      | select((.status // (if .passes == true then "passed" else "todo" end)) == "implemented")
      | .id
    ]
  ' "$PRD_FILE" 2>/dev/null || echo '[]'
}

implemented_count() {
  ralph_count_implemented_stories "$PRD_FILE"
}

passed_ids_json() {
  jq -c '
    [
      .userStories[]?
      | select((.status // (if .passes == true then "passed" else "todo" end)) == "passed")
      | .id
    ]
  ' "$PRD_FILE" 2>/dev/null || echo '[]'
}

passed_count() {
  ralph_count_passed_stories "$PRD_FILE"
}

story_label() {
  local story_id="$1"
  jq -r --arg story_id "$story_id" '
    .userStories[]?
    | select(.id == $story_id)
    | "\(.id) - \(.title)"
  ' "$PRD_FILE" 2>/dev/null | head -n 1
}

format_story_labels() {
  local ids_json="$1"
  local labels=()
  local story_id label

  while IFS= read -r story_id; do
    [[ -z "$story_id" ]] && continue
    label=$(story_label "$story_id")
    if [[ -n "$label" ]]; then
      labels+=("$label")
    else
      labels+=("$story_id")
    fi
  done < <(jq -r '.[]' <<<"$ids_json")

  if [[ "${#labels[@]}" -eq 0 ]]; then
    printf ''
    return 0
  fi

  local joined=""
  local idx=0
  for label in "${labels[@]}"; do
    if [[ "$idx" -gt 0 ]]; then
      joined+=", "
    fi
    joined+="$label"
    idx=$((idx + 1))
  done

  printf '%s' "$joined"
}

branch_name() {
  jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null || true
}

pr_snapshot() {
  local branch="$1"
  if [[ -z "$branch" ]]; then
    printf '{}'
    return 0
  fi

  gh pr view "$branch" --json number,url,mergeable,mergeStateStatus,statusCheckRollup 2>/dev/null || echo '{}'
}

failing_checks_summary() {
  local snapshot="$1"
  local summary

  summary=$(jq -r '
    [
      .statusCheckRollup[]?
      | select(((.status // "COMPLETED") | ascii_upcase) == "COMPLETED")
      | (.conclusion // "" | ascii_upcase) as $conclusion
      | select($conclusion != "" and $conclusion != "SUCCESS" and $conclusion != "SKIPPED" and $conclusion != "NEUTRAL")
      | "\(.name // "check") [\($conclusion)]"
    ] | join(", ")
  ' <<<"$snapshot" 2>/dev/null)

  if [[ -z "$summary" || "$summary" == "null" ]]; then
    printf '%s' "- none"
  else
    printf '%s' "$summary"
  fi
}

latest_review_fix_sha() {
  git -C "$REPO_ROOT" log --grep '^fix(review): apply Ralph review feedback$' -n 1 --format=%H 2>/dev/null || true
}

qa_run_snapshot() {
  local run_id="$1"

  if [[ -z "$run_id" ]]; then
    printf '{}'
    return 0
  fi

  gh run view "$run_id" --json status,conclusion,url 2>/dev/null || echo '{}'
}

qa_artifact_path() {
  local run_id="$1"
  local download_dir="$SCRIPT_DIR/artifacts/qa-run-$run_id"
  local apk_path

  mkdir -p "$download_dir"
  rm -f "$download_dir"/*.apk 2>/dev/null || true

  if ! gh run download "$run_id" -n "$QA_ARTIFACT_NAME" -D "$download_dir" >/dev/null 2>&1; then
    return 1
  fi

  apk_path=$(find "$download_dir" -type f -name '*.apk' | head -n 1)
  if [[ -z "$apk_path" ]]; then
    return 1
  fi

  printf '%s' "$apk_path"
}

load_prev_state() {
  if [[ -f "$STATE_FILE" ]]; then
    cat "$STATE_FILE"
  else
    printf '{}'
  fi
}

save_state() {
  local seen_alive="$1"
  local alive="$2"
  local implemented_ids="$3"
  local passed_ids="$4"
  local ci_failed="$5"
  local failing_checks="$6"
  local review_fix_sha="$7"
  local qa_run_id="$8"
  local qa_status="$9"
  local qa_conclusion="${10}"
  local qa_delivered="${11}"
  local terminal="${12}"

  jq -n \
    --argjson seenAlive "$seen_alive" \
    --argjson alive "$alive" \
    --argjson implementedIds "$implemented_ids" \
    --argjson passedIds "$passed_ids" \
    --argjson ciFailed "$ci_failed" \
    --arg failingChecks "$failing_checks" \
    --arg reviewFixSha "$review_fix_sha" \
    --arg qaRunId "$qa_run_id" \
    --arg qaStatus "$qa_status" \
    --arg qaConclusion "$qa_conclusion" \
    --argjson qaDelivered "$qa_delivered" \
    --arg terminal "$terminal" \
    --arg checkedAt "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
    '{
      seenAlive: $seenAlive,
      alive: $alive,
      implementedIds: $implementedIds,
      passedIds: $passedIds,
      ciFailed: $ciFailed,
      failingChecks: $failingChecks,
      reviewFixSha: $reviewFixSha,
      qaRunId: $qaRunId,
      qaStatus: $qaStatus,
      qaConclusion: $qaConclusion,
      qaDelivered: $qaDelivered,
      terminal: $terminal,
      checkedAt: $checkedAt
    }' > "$STATE_FILE"
}

check_once() {
  local prev_json prev_seen_alive prev_alive prev_implemented_ids prev_passed_ids prev_ci_failed prev_failing_checks
  local prev_review_fix_sha prev_qa_run_id prev_qa_status prev_qa_conclusion prev_qa_delivered prev_terminal
  local branch pr_json pr_number pr_url checks_summary ci_failed
  local alive total todo_count_value implemented_ids implemented_count_value passed_ids passed_count_value latest_line review_fix_sha qa_run_id
  local qa_json qa_status qa_conclusion qa_url qa_completed qa_delivered_now
  local new_implemented_ids new_passed_ids new_labels terminal="" should_exit=false qa_apk_path qa_caption

  prev_json=$(load_prev_state)
  prev_seen_alive=$(jq -r '.seenAlive // false' <<<"$prev_json")
  prev_alive=$(jq -r '.alive // false' <<<"$prev_json")
  prev_implemented_ids=$(jq -c '.implementedIds // []' <<<"$prev_json")
  prev_passed_ids=$(jq -c '.passedIds // []' <<<"$prev_json")
  prev_ci_failed=$(jq -r '.ciFailed // false' <<<"$prev_json")
  prev_failing_checks=$(jq -r '.failingChecks // "- none"' <<<"$prev_json")
  prev_review_fix_sha=$(jq -r '.reviewFixSha // empty' <<<"$prev_json")
  prev_qa_run_id=$(jq -r '.qaRunId // empty' <<<"$prev_json")
  prev_qa_status=$(jq -r '.qaStatus // empty' <<<"$prev_json")
  prev_qa_conclusion=$(jq -r '.qaConclusion // empty' <<<"$prev_json")
  prev_qa_delivered=$(jq -r '.qaDelivered // false' <<<"$prev_json")
  prev_terminal=$(jq -r '.terminal // empty' <<<"$prev_json")

  branch=$(branch_name)
  if detect_ralph_alive; then
    alive=true
  else
    alive=false
  fi

  total=$(story_total)
  todo_count_value=$(todo_count)
  implemented_ids=$(implemented_ids_json)
  implemented_count_value=$(implemented_count)
  passed_ids=$(passed_ids_json)
  passed_count_value=$(passed_count)
  latest_line=$(latest_non_empty_line)
  review_fix_sha=$(latest_review_fix_sha)
  qa_run_id=$(cat "$QA_RUN_FILE" 2>/dev/null || true)
  qa_status=""
  qa_conclusion=""
  qa_url=""
  qa_completed=false
  qa_delivered_now=false

  if [[ -n "$qa_run_id" ]]; then
    qa_json=$(qa_run_snapshot "$qa_run_id")
    qa_status=$(jq -r '.status // empty' <<<"$qa_json")
    qa_conclusion=$(jq -r '.conclusion // empty' <<<"$qa_json")
    qa_url=$(jq -r '.url // empty' <<<"$qa_json")
    if [[ "${qa_status^^}" == "COMPLETED" ]]; then
      qa_completed=true
    fi
  fi

  pr_json=$(pr_snapshot "$branch")
  pr_number=$(jq -r '.number // empty' <<<"$pr_json")
  pr_url=$(jq -r '.url // empty' <<<"$pr_json")
  checks_summary=$(failing_checks_summary "$pr_json")
  if [[ "$checks_summary" == "- none" ]]; then
    ci_failed=false
  else
    ci_failed=true
  fi

  if [[ "$prev_seen_alive" != "true" ]]; then
    if [[ "$alive" == "true" ]]; then
      send_message "Ralph запущен. Ветка: ${branch:-unknown}. Stories: todo=${todo_count_value}, implemented=${implemented_count_value}, passed=${passed_count_value}/${total}."
      save_state "true" "$alive" "$implemented_ids" "$passed_ids" "$ci_failed" "$checks_summary" "$review_fix_sha" "$qa_run_id" "$qa_status" "$qa_conclusion" "false" ""
    else
      save_state "false" "$alive" "$implemented_ids" "$passed_ids" "$ci_failed" "$checks_summary" "$review_fix_sha" "$qa_run_id" "$qa_status" "$qa_conclusion" "false" ""
    fi
    return 0
  fi

  new_implemented_ids=$(jq -c --argjson prev "$prev_implemented_ids" '
    [.[] | select(($prev | index(.)) | not)]
  ' <<<"$implemented_ids")
  if [[ "$new_implemented_ids" != "[]" ]]; then
    new_labels=$(format_story_labels "$new_implemented_ids")
    send_message "Ralph реализовал story: ${new_labels}. Stories: todo=${todo_count_value}, implemented=${implemented_count_value}, passed=${passed_count_value}/${total}."
  fi

  new_passed_ids=$(jq -c --argjson prev "$prev_passed_ids" '
    [.[] | select(($prev | index(.)) | not)]
  ' <<<"$passed_ids")
  if [[ "$new_passed_ids" != "[]" ]]; then
    new_labels=$(format_story_labels "$new_passed_ids")
    send_message "CI подтвердил story: ${new_labels}. Stories: todo=${todo_count_value}, implemented=${implemented_count_value}, passed=${passed_count_value}/${total}."
  fi

  if [[ "$ci_failed" == "true" && ( "$prev_ci_failed" != "true" || "$checks_summary" != "$prev_failing_checks" ) ]]; then
    if [[ -n "$pr_url" ]]; then
      send_message "CI упал. Ralph должен перейти в repair mode. Checks: ${checks_summary}. PR: ${pr_url}"
    else
      send_message "CI упал. Ralph должен перейти в repair mode. Checks: ${checks_summary}."
    fi
  fi

  if [[ -n "$review_fix_sha" && "$review_fix_sha" != "$prev_review_fix_sha" ]]; then
    if [[ -n "$pr_url" ]]; then
      send_message "Ревьюер нашел проблему. Ralph применил фиксы и повторяет review/CI. PR: ${pr_url}"
    else
      send_message "Ревьюер нашел проблему. Ralph применил фиксы и повторяет review/CI."
    fi
  fi

  if [[ -n "$qa_run_id" && "$qa_run_id" != "$prev_qa_run_id" ]]; then
    send_message "QA Build запущен. Run id: ${qa_run_id}."
  fi

  if [[ -n "$qa_run_id" && "$qa_completed" == "true" && "$prev_qa_delivered" != "true" ]]; then
    if [[ "${qa_conclusion^^}" == "SUCCESS" ]]; then
      qa_apk_path=""
      if qa_apk_path=$(qa_artifact_path "$qa_run_id"); then
        qa_caption="QA Build завершен успешно. Отправляю APK. Run id: ${qa_run_id}"
        if [[ -n "$pr_url" ]]; then
          qa_caption="${qa_caption}. PR: ${pr_url}"
        elif [[ -n "$qa_url" ]]; then
          qa_caption="${qa_caption}. Run: ${qa_url}"
        fi

        if send_media "$qa_caption" "$qa_apk_path"; then
          qa_delivered_now=true
        fi
      fi

      if [[ "$qa_delivered_now" != "true" ]]; then
        if [[ -n "$qa_url" ]]; then
          send_message "QA Build завершен успешно, но APK не удалось автоматически отправить. Run id: ${qa_run_id}. Run: ${qa_url}"
        else
          send_message "QA Build завершен успешно, но APK не удалось автоматически отправить. Run id: ${qa_run_id}."
        fi
        qa_delivered_now=true
      fi
    else
      if [[ -n "$qa_url" ]]; then
        send_message "QA Build завершился неуспешно. Conclusion: ${qa_conclusion:-unknown}. Run id: ${qa_run_id}. Run: ${qa_url}"
      else
        send_message "QA Build завершился неуспешно. Conclusion: ${qa_conclusion:-unknown}. Run id: ${qa_run_id}."
      fi
      qa_delivered_now=true
    fi
  elif [[ "$prev_qa_delivered" == "true" && "$qa_run_id" == "$prev_qa_run_id" ]]; then
    qa_delivered_now=true
  fi

  if [[ "$prev_seen_alive" == "true" && "$prev_alive" == "true" && "$alive" != "true" ]]; then
    if [[ "$passed_count_value" == "$total" && "$total" != "0" ]]; then
      terminal="completed"
      if [[ -f "$RUN_SUMMARY_FILE" ]]; then
        pr_url=$(jq -r '.pr_url // empty' "$RUN_SUMMARY_FILE" 2>/dev/null || echo "$pr_url")
      fi
      if [[ -n "$pr_url" ]]; then
        send_message "Ralph завершил run. Stories: todo=${todo_count_value}, implemented=${implemented_count_value}, passed=${passed_count_value}/${total}. PR: ${pr_url}${qa_run_id:+. QA Build: ${qa_run_id}}"
      else
        send_message "Ralph завершил run. Stories: todo=${todo_count_value}, implemented=${implemented_count_value}, passed=${passed_count_value}/${total}.${qa_run_id:+ QA Build: ${qa_run_id}}"
      fi
    elif [[ "$prev_terminal" != "crashed" ]]; then
      terminal="crashed"
      send_message "Ralph остановился раньше времени. Stories: todo=${todo_count_value}, implemented=${implemented_count_value}, passed=${passed_count_value}/${total}. Последнее: ${latest_line:-нет данных}."
    fi
  fi

  if [[ "$terminal" == "completed" ]]; then
    if [[ -n "$qa_run_id" && "$qa_delivered_now" != "true" ]]; then
      should_exit=false
    else
      should_exit=true
    fi
  elif [[ "$terminal" == "crashed" ]]; then
    should_exit=true
  elif [[ "$prev_terminal" == "completed" ]]; then
    if [[ -n "$qa_run_id" && "$qa_delivered_now" == "true" ]]; then
      terminal="completed"
      should_exit=true
    else
      terminal="completed"
      should_exit=false
    fi
  elif [[ "$prev_terminal" == "crashed" ]]; then
    terminal="crashed"
    should_exit=true
  fi

  if [[ "$prev_terminal" == "completed" && "$terminal" != "completed" ]]; then
    terminal="completed"
  fi

  save_state "$prev_seen_alive" "$alive" "$implemented_ids" "$passed_ids" "$ci_failed" "$checks_summary" "$review_fix_sha" "$qa_run_id" "$qa_status" "$qa_conclusion" "$qa_delivered_now" "$terminal"

  if [[ "$should_exit" == "true" ]]; then
    return 10
  fi
  return 0
}

if [[ "$WATCH_MODE" == "true" ]]; then
  while true; do
    if check_once; then
      :
    else
      status=$?
      if [[ "$status" -eq 10 ]]; then
        exit 0
      fi
      exit "$status"
    fi
    sleep "$POLL_SECONDS"
  done
else
  if check_once; then
    :
  else
    status=$?
    if [[ "$status" -eq 10 ]]; then
      exit 0
    fi
    exit "$status"
  fi
fi
