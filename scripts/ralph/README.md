# Ralph — Autonomous AI Agent Loop

Ralph is an autonomous coding agent that executes tasks from a PRD (Product Requirements Document) one story at a time, committing each completed story to git. When all stories are done, Ralph can automatically push, open a PR, run a code review, wait for CI, trigger a QA build, and download the resulting APK.

---

## Prerequisites

- [`claude`](https://github.com/anthropics/claude-code) — Claude Code CLI (`npm install -g @anthropic-ai/claude-code`)
- [`gh`](https://cli.github.com/) — GitHub CLI, authenticated (`gh auth login`)
- [`jq`](https://jqlang.github.io/jq/) — JSON processor (`brew install jq`)
- A `prd.json` file in `scripts/ralph/` (see [Preparing a Task](#preparing-a-task))

---

## Quick Start

```bash
# Run up to 10 iterations, no PR
./scripts/ralph/ralph.sh --tool claude 10

# Run and create PR automatically when done
./scripts/ralph/ralph.sh --tool claude --pr 10

# Run, create PR, and download the debug APK to your machine
./scripts/ralph/ralph.sh --tool claude --pr --download 10
```

---

## All Flags

| Flag | Description |
|------|-------------|
| `--tool claude` | Use Claude Code as the AI engine (recommended) |
| `--tool gemini` | Use Gemini CLI as the AI engine |
| `--tool amp` | Use Amp CLI as the AI engine (default for backwards compatibility) |
| `--review-tool claude` | Override the tool used for the post-completion code review loop |
| `--codex` | Shortcut for `--tool codex --review-tool claude` |
| `--remote-run` | Shell-managed remote mode: the agent commits one story locally, Ralph shell handles push/PR/CI, and story state is tracked as `todo` / `implemented` / `passed`. Defaults to `ci-first`; set `RALPH_REMOTE_RUN_MODE=deferred` to defer pass promotion until the final PR gate. |
| `--pr` | After completion: push branch → create PR → code review → wait CI → trigger QA Build |
| `--download` | After QA Build: download the debug APK locally. Use this on your Mac. On a VM, omit this flag and let Garlic handle the download. |
| `N` (number) | Maximum number of iterations (default: 10). Each iteration handles one user story. |

---

## Command Variations

### Development on Mac — no PR needed

Just run the agent loop locally. Useful for quick tasks or testing.

```bash
./scripts/ralph/ralph.sh --tool claude 10
```

Ralph will work through all stories in `prd.json`, committing after each one. Stops when all stories are complete or max iterations is reached.

---

### Remote mode (`--remote-run`)

Use this on slower VMs when you want GitHub Actions to be the source of truth and Ralph shell to manage the PRD state machine.

```bash
./scripts/ralph/ralph.sh --codex --remote-run 10
```

What `--remote-run` implies automatically:
1. Works one story per iteration
2. The agent only codes, updates `progress.txt`, and commits the story locally
3. Ralph shell pushes the branch, creates/reuses a Draft PR targeting `main`, and keeps the branch rebased on fresh `origin/main` before the coding prompt
4. If CI fails, Ralph stays in the same run and enters repair mode for that same story on the next iteration
5. Local heavy checks are not used as blocking gates
6. Story state is explicit:
   - `todo` = eligible for coding
   - `implemented` = coded and committed, but not yet fully validated
   - `passed` = validated and complete

Modes:
- Default remote mode is `ci-first`: after each story commit, Ralph waits for CI and marks the story `passed` only after green CI.
- Set `RALPH_REMOTE_RUN_MODE=deferred` to keep moving after each story by marking it `implemented`; once all stories are implemented, Ralph runs the final PR review/CI flow and promotes them to `passed`.

---

### Development on Mac — with PR and APK download

Full automated flow: code → PR → review → CI → build → APK on your machine.

```bash
./scripts/ralph/ralph.sh --tool claude --pr --download 10
```

What happens:
1. Ralph completes all stories, committing each one
2. Pushes the branch to GitHub
3. Opens a PR (or reuses existing one)
4. Runs an automated code review (up to 2 attempts, fixes issues and pushes)
5. Waits for required CI checks (`Compile Debug Kotlin`, `Lint & Analysis`, `Unit Tests`) to pass
6. Triggers the `QA Build` workflow on GitHub Actions (only after clean review + green CI)
7. Waits for the build to finish
8. Downloads the debug APK to `scripts/ralph/artifacts/`
9. Keeps only the most recent APK (old ones are cleaned up automatically)

---

### On a VM (Garlic / headless server) — with PR, no local download

Use this when running Ralph from a Google Cloud VM or any server. Garlic will handle the APK download and delivery separately.

```bash
./scripts/ralph/ralph.sh --tool claude --remote-run 10
```

What happens:
1. The agent commits one story locally per iteration; Ralph shell handles push/PR/CI and story-state transitions after each iteration
2. If CI goes red, Ralph stays alive and spends the next iteration repairing that same story with the failing check context injected into the prompt
3. If branch freshness rebases onto `main` conflict, Ralph keeps the same outer iteration and asks the agent to resolve the in-progress rebase before continuing
4. After all stories are complete (or all are at least `implemented` in deferred mode), Ralph runs the normal PR review flow and triggers `QA Build`
5. Prints the GitHub Actions run ID for the build
6. Exits — Garlic picks up the run ID and downloads/sends the APK via Telegram

---

### Fewer iterations for small tasks

If your `prd.json` has only 2–3 stories, limit the iterations to avoid unnecessary Claude sessions.

```bash
./scripts/ralph/ralph.sh --tool claude --pr 3
```

---

### Using Gemini CLI instead of Claude Code

Useful on machines where Claude Code is not available or for cost comparison.

```bash
./scripts/ralph/ralph.sh --tool gemini --pr --download 10
```

> Note: Gemini CLI must be installed and authenticated. The `CLAUDE.md` prompt is reused as input.

---

## Preparing a Task

Before running Ralph, you need a `prd.json` file in `scripts/ralph/`.

### Option 1 — Manually convert a PRD

Use the `/ralph` skill in Claude Code to convert a markdown PRD:

```
/ralph  →  "convert scripts/ralph/prd-my-feature.md to ralph format"
```

This creates `scripts/ralph/prd.json`.

### Option 2 — Via Garlic (Telegram)

Send your task to Garlic on Telegram. The `prd-agent` (see `scripts/ralph/prd-agent.md`) will explore the codebase and write `prd.json` automatically, then trigger Ralph.

### Option 3 — Write it manually

Copy `scripts/ralph/prd.json.example` and fill in your stories. Each story must have:

```json
{
  "id": "US-001",
  "title": "Short title",
  "description": "As a [user], I want [feature] so that [benefit]",
  "acceptanceCriteria": [
    "Specific verifiable criterion",
    "./gradlew assembleDebug passes"
  ],
  "priority": 1,
  "status": "todo",
  "passes": false,
  "notes": ""
}
```

`status` is the source of truth:
- `todo` = not implemented yet
- `implemented` = coded/committed, waiting on final validation
- `passed` = validated and done

`passes` remains for compatibility. Ralph keeps `passes=true` only when `status="passed"`.

**Story sizing rule:** each story must be completable in a single Claude Code context window. If in doubt, split it.

**Story ordering rule:** stories with database/domain changes come before UI stories that depend on them.

---

## Files Overview

```
scripts/ralph/
├── ralph.sh            — main loop script
├── CLAUDE.md           — instructions given to the AI agent each iteration
├── review-prompt.md    — instructions for the automated code review step
├── prd-agent.md        — Garlic agent prompt for generating prd.json from a task description
├── prd.json            — current task list (created per feature, not committed to main)
├── prd.json.example    — example prd.json structure
├── progress.txt        — append-only log of what each iteration did
├── artifacts/          — downloaded APK files (only latest is kept)
└── archive/            — past completed runs (prd.json + progress.txt per feature)
```

---

## Monitoring Progress

While Ralph is running, open a second terminal and check:

```bash
# See which stories are done
cat scripts/ralph/prd.json | jq '.userStories[] | {id, title, status, passes}'

# Read the progress log
cat scripts/ralph/progress.txt

# See Ralph's commits
git log --oneline -10
```

---

## Watchdog Launcher

Use the watchdog wrapper when you want Telegram delivery on both success and failure, plus a per-run log file under `scripts/ralph/logs/`.

```bash
./scripts/ralph/run-watchdog.sh --codex --remote-run
```

The wrapper passes all arguments through to `ralph.sh`, starts `telegram-event-monitor.sh --watch` if needed, and sends a Telegram message through OpenClaw when the run exits.

---

## Debugging

### Ralph stopped before completing all stories

Check the progress log and recent git history:

```bash
cat scripts/ralph/progress.txt
git log --oneline -5
```

Increase max iterations and re-run — Ralph will skip stories that are already `status: "passed"` and, in deferred mode, continue from the remaining `todo` stories.

### CI failed on the PR

```bash
gh pr checks <branch-name>
gh run view <run-id> --log-failed
```

### QA Build not found

The `QA Build` workflow must exist on the `main` branch of the repo before it can be triggered on a feature branch. If you see `could not find any workflows named QA Build`, merge or push `.github/workflows/qa.yml` to `main` first.

### Code review loop did not pass after 2 attempts

Ralph will print:
```
WARNING: Code review did not pass after 2 attempts.
Manual review required for PR: https://github.com/...
```

Review the PR manually and merge if the remaining issues are acceptable.

---

## Architecture Notes

- **Fresh context per iteration:** each Ralph iteration spawns a new AI session with no memory of previous iterations. State is passed through `prd.json`, `progress.txt`, git history, and in `--remote-run` an injected repair snapshot for the current story.
- **One story per iteration:** Ralph picks the highest-priority story whose effective status is `todo`, implements it, validates according to mode, commits it, and lets the shell manage the `implemented` / `passed` transitions.
- **`--remote-run` is shell-managed:** the AI agent handles code + local commit only; `ralph.sh` performs branch freshness sync, Draft PR creation/reuse, CI waiting, and story-state promotion.
- **`prd.json` lives in feature branches only:** do not commit `prd.json` to `main`. It belongs in `ralph/*` branches and is visible in the PR diff for debugging.
- **Artifacts are ephemeral:** only the most recent APK is kept in `scripts/ralph/artifacts/`. Add `scripts/ralph/artifacts/` to `.gitignore` if not already present.
