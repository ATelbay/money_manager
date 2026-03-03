#!/bin/bash
set -u
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RALPH_SCRIPT="$SCRIPT_DIR/ralph.sh"
MONITOR_SCRIPT="$SCRIPT_DIR/telegram-event-monitor.sh"
PRD_FILE="$SCRIPT_DIR/prd.json"
PROGRESS_FILE="$SCRIPT_DIR/progress.txt"
LOG_DIR="$SCRIPT_DIR/logs"
RUN_TIMESTAMP="$(date -u +"%Y%m%dT%H%M%SZ")"
RUN_LOG="$LOG_DIR/run-$RUN_TIMESTAMP.log"

TARGET="${RALPH_TELEGRAM_TARGET:-377219158}"
START_MONITOR="${RALPH_WATCHDOG_START_MONITOR:-true}"

NOTIFICATION_SENT=false
RUN_COMPLETED=false
RUN_EXIT_CODE=""
RUN_SIGNAL=""
CHILD_PID=""

mkdir -p "$LOG_DIR" || exit 1
: > "$RUN_LOG" || exit 1

log_watchdog() {
  printf '[watchdog] %s\n' "$1" | tee -a "$RUN_LOG"
}

send_message() {
  local text="$1"
  local attempt

  [[ -z "$text" ]] && return 0

  for attempt in 1 2 3; do
    if openclaw message send \
      --channel telegram \
      --target "$TARGET" \
      --message "$text" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  log_watchdog "WARN: failed to send Telegram notification after 3 attempts"
  return 1
}

signal_name_from_number() {
  local signal_number="$1"
  local signal_name

  signal_name="$(kill -l "$signal_number" 2>/dev/null || true)"
  if [[ -n "$signal_name" ]]; then
    printf 'SIG%s' "$signal_name"
  fi
}

infer_signal_name() {
  local exit_code="$1"

  if [[ "$exit_code" =~ ^[0-9]+$ ]] && (( exit_code >= 128 )); then
    signal_name_from_number "$((exit_code - 128))"
  fi
}

branch_name() {
  if [[ -f "$PRD_FILE" ]]; then
    jq -r '.branchName // empty' "$PRD_FILE" 2>/dev/null || true
  fi
}

story_total() {
  if [[ -f "$PRD_FILE" ]]; then
    jq -r '.userStories | length' "$PRD_FILE" 2>/dev/null || echo "?"
  else
    echo "?"
  fi
}

story_passed() {
  if [[ -f "$PRD_FILE" ]]; then
    jq -r '[.userStories[]? | select(.passes == true)] | length' "$PRD_FILE" 2>/dev/null || echo "?"
  else
    echo "?"
  fi
}

latest_non_empty_line() {
  if [[ -f "$PROGRESS_FILE" ]]; then
    awk 'NF { line = $0 } END { print line }' "$PROGRESS_FILE" 2>/dev/null || true
  fi
}

status_text() {
  local exit_code="$1"
  local signal_name="$2"
  local inferred_signal=""

  if [[ -n "$signal_name" ]]; then
    printf 'exit %s (%s)' "$exit_code" "$signal_name"
    return 0
  fi

  inferred_signal="$(infer_signal_name "$exit_code")"
  if [[ -n "$inferred_signal" ]]; then
    printf 'exit %s (%s)' "$exit_code" "$inferred_signal"
    return 0
  fi

  printf 'exit %s' "$exit_code"
}

notify_once() {
  local outcome="$1"
  local exit_code="$2"
  local signal_name="$3"
  local branch
  local passed
  local total
  local progress_line
  local message

  if [[ "$NOTIFICATION_SENT" == "true" ]]; then
    return 0
  fi

  branch="$(branch_name)"
  [[ -z "$branch" ]] && branch="unknown"
  passed="$(story_passed)"
  total="$(story_total)"
  progress_line="$(latest_non_empty_line)"
  [[ -z "$progress_line" ]] && progress_line="(no progress recorded)"

  if [[ "$outcome" == "success" ]]; then
    message="Ralph succeeded
branch: $branch
stories: $passed/$total
log: $RUN_LOG"
  else
    message="Ralph failed
branch: $branch
status: $(status_text "$exit_code" "$signal_name")
stories: $passed/$total
latest progress: $progress_line
log: $RUN_LOG"
  fi

  NOTIFICATION_SENT=true
  send_message "$message" || true
}

ensure_monitor_running() {
  local monitor_log="$LOG_DIR/telegram-event-monitor.log"

  if [[ "$START_MONITOR" == "false" ]]; then
    return 0
  fi

  if [[ ! -f "$MONITOR_SCRIPT" ]]; then
    log_watchdog "WARN: monitor script not found at $MONITOR_SCRIPT"
    return 0
  fi

  if pgrep -af "telegram-event-monitor\\.sh --watch" >/dev/null 2>&1; then
    return 0
  fi

  bash "$MONITOR_SCRIPT" --reset >/dev/null 2>&1 || true
  nohup bash "$MONITOR_SCRIPT" --watch >>"$monitor_log" 2>&1 &
  disown || true
  log_watchdog "Started telegram-event-monitor.sh --watch"
}

handle_signal() {
  local signal_name="$1"
  local signal_number="$2"
  local signal_exit_code=$((128 + signal_number))
  local child_status=""

  RUN_SIGNAL="$signal_name"

  if [[ -n "$CHILD_PID" ]] && kill -0 "$CHILD_PID" 2>/dev/null; then
    kill "-$signal_number" "$CHILD_PID" 2>/dev/null || kill "$CHILD_PID" 2>/dev/null || true
    wait "$CHILD_PID" 2>/dev/null
    child_status=$?
  fi

  if [[ -n "$child_status" ]]; then
    RUN_EXIT_CODE="$child_status"
  else
    RUN_EXIT_CODE="$signal_exit_code"
  fi

  notify_once "failure" "$RUN_EXIT_CODE" "$RUN_SIGNAL"
  RUN_COMPLETED=true

  trap - EXIT
  exit "$signal_exit_code"
}

handle_exit() {
  local exit_code=$?
  local final_exit_code
  local final_signal

  if [[ "$RUN_COMPLETED" == "true" || "$NOTIFICATION_SENT" == "true" ]]; then
    return 0
  fi

  final_exit_code="$exit_code"
  final_signal="$RUN_SIGNAL"

  if [[ -n "$RUN_EXIT_CODE" ]]; then
    final_exit_code="$RUN_EXIT_CODE"
  fi

  if [[ -z "$final_signal" ]]; then
    final_signal="$(infer_signal_name "$final_exit_code")"
  fi

  if [[ "$final_exit_code" == "0" ]]; then
    notify_once "success" "$final_exit_code" "$final_signal"
  else
    notify_once "failure" "$final_exit_code" "$final_signal"
  fi
}

trap 'handle_exit' EXIT
trap 'handle_signal SIGINT 2' INT
trap 'handle_signal SIGTERM 15' TERM

if [[ ! -x "$RALPH_SCRIPT" ]]; then
  log_watchdog "ERROR: missing executable $RALPH_SCRIPT"
  RUN_EXIT_CODE=1
  notify_once "failure" "$RUN_EXIT_CODE" ""
  RUN_COMPLETED=true
  exit "$RUN_EXIT_CODE"
fi

ensure_monitor_running

log_watchdog "Launching $RALPH_SCRIPT $*"
"$RALPH_SCRIPT" "$@" > >(tee -a "$RUN_LOG") 2> >(tee -a "$RUN_LOG" >&2) &
CHILD_PID=$!

wait "$CHILD_PID"
RUN_EXIT_CODE=$?

if [[ "$RUN_EXIT_CODE" == "0" ]]; then
  notify_once "success" "$RUN_EXIT_CODE" ""
else
  RUN_SIGNAL="$(infer_signal_name "$RUN_EXIT_CODE")"
  notify_once "failure" "$RUN_EXIT_CODE" "$RUN_SIGNAL"
fi

RUN_COMPLETED=true

exit "$RUN_EXIT_CODE"
