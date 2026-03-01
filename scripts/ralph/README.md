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
5. Waits for CI checks (lint + unit tests) to pass
6. Triggers the QA Build workflow on GitHub Actions
7. Waits for the build to finish
8. Downloads the debug APK to `scripts/ralph/artifacts/`
9. Keeps only the most recent APK (old ones are cleaned up automatically)

---

### On a VM (Garlic / headless server) — with PR, no local download

Use this when running Ralph from a Google Cloud VM or any server. Garlic will handle the APK download and delivery separately.

```bash
./scripts/ralph/ralph.sh --tool claude --pr 10
```

What happens:
1. Same as above through step 6 (triggers QA Build)
2. Prints the GitHub Actions run ID for the build
3. Exits — Garlic picks up the run ID and downloads/sends the APK via Telegram

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
  "passes": false,
  "notes": ""
}
```

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
cat scripts/ralph/prd.json | jq '.userStories[] | {id, title, passes}'

# Read the progress log
cat scripts/ralph/progress.txt

# See Ralph's commits
git log --oneline -10
```

---

## Debugging

### Ralph stopped before completing all stories

Check the progress log and recent git history:

```bash
cat scripts/ralph/progress.txt
git log --oneline -5
```

Increase max iterations and re-run — Ralph will skip stories that are already `passes: true`.

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

- **Fresh context per iteration:** each Ralph iteration spawns a new AI session with no memory of previous iterations. State is passed through `prd.json`, `progress.txt`, and git history only.
- **One story per iteration:** Ralph picks the highest-priority story with `passes: false`, implements it, runs `./gradlew assembleDebug`, commits, and marks it done.
- **`prd.json` lives in feature branches only:** do not commit `prd.json` to `main`. It belongs in `ralph/*` branches and is visible in the PR diff for debugging.
- **Artifacts are ephemeral:** only the most recent APK is kept in `scripts/ralph/artifacts/`. Add `scripts/ralph/artifacts/` to `.gitignore` if not already present.
