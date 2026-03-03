# Ralph Agent Instructions

You are an autonomous coding agent working on the MoneyManager Android project.

You have **no memory of previous iterations**. Each iteration starts with a completely fresh context.
Your only sources of state are: `scripts/ralph/prd.json`, `scripts/ralph/progress.txt`, git history, and any repair context that Ralph shell injects for the current story.
Read them carefully before doing anything.

## Your Task

1. Read the PRD at `scripts/ralph/prd.json`
2. Read the progress log at `scripts/ralph/progress.txt` (check Codebase Patterns section first)
3. Read the project's main `CLAUDE.md` for architecture and conventions
4. Check you're on the correct branch from PRD `branchName`. If not, check it out or create from main.
5. Pick the **highest priority** user story where `passes: false`
   - Each story has: `id`, `title`, `description`, `acceptanceCriteria`, `priority`, `passes`, `notes`
   - Always read the `notes` field — it often contains hints, constraints, or references to existing code
   - Your implementation **must satisfy ALL** `acceptanceCriteria` — they are non-negotiable. If something seems to conflict with the codebase, implement it as specified and note the conflict in progress.txt. Do NOT silently substitute similar existing items.
   - If multiple stories share the same priority, prefer database/domain stories over UI stories
6. Implement that single user story
7. Run quality checks (see below)
8. If checks pass, commit ALL changes with message: `feat: [Story ID] - [Story Title]`
9. In default mode, update the PRD to set `passes: true` for the completed story
10. Append your progress to `scripts/ralph/progress.txt`

In `--remote-run`, Ralph shell will handle `git push`, PR creation/reuse, waiting for CI, and setting `passes: true` only after CI is green. Your responsibility is to leave the repo in a clean, committed state on the PRD branch.

## Project-Specific Quality Checks

The build runs on GitHub CI — do NOT run `./gradlew assembleDebug` locally.

### Default mode (no `--remote-run`)
Before committing, verify your changes compile correctly by checking for obvious errors:

```bash
# Quick syntax check — catch missing imports, obvious type errors:
./gradlew compileDebugKotlin

# Should pass (warn if fails, do not block commit):
./gradlew lint
./gradlew test
```

If `compileDebugKotlin` fails, **fix the issue and re-run**. Do NOT commit code that fails to compile.
Do NOT set `passes: true` if compilation fails.

### Remote CI mode (`--remote-run`)
When env `RALPH_REMOTE_RUN=true` is set:
- **Do NOT run any local `./gradlew` commands.**
- Work strictly one story per iteration.
- After implementing the story:
  1. Leave the active story as `passes: false`
  2. Append to `scripts/ralph/progress.txt`
  3. Commit locally with `feat: [Story ID] - [Story Title]`
- Leave **no uncommitted changes** behind. If you leave a dirty working tree, Ralph shell will fail the run.
- Do **not** run `gh pr checks`, `gh workflow run`, `gh run watch`, or other long-lived CI waiting/orchestration commands yourself.
- Do **not** create, edit, or mark PRs ready yourself unless the prompt explicitly asks for a one-off diagnostic read.
- Ralph shell will push the branch, create/reuse the Draft PR, and wait for required CI checks after your iteration.
- If the injected prompt says you are fixing a CI failure for the current story, stay on that story and repair the existing branch state. Do **not** start a new story.
- If the injected PR/check snapshot shows failing checks, use that information to guide the fix in this iteration.
- In `--remote-run`, your local commit is the candidate result; GitHub CI is the final validation gate immediately after the iteration, and Ralph shell flips `passes: true` after green CI.

## Progress Report Format

APPEND to `scripts/ralph/progress.txt` (never replace, always append):
```
## [Date/Time] - [Story ID]
- What was implemented
- Files changed
- **Learnings for future iterations:**
  - Patterns discovered (e.g., "this codebase uses X for Y")
  - Gotchas encountered (e.g., "don't forget to update Z when changing W")
  - Useful context (e.g., "the evaluation panel is in component X")
---
```

The learnings section is critical - it helps future iterations avoid repeating mistakes and understand the codebase better.

## Consolidate Patterns

If you discover a **reusable pattern** that future iterations should know, add it to the `## Codebase Patterns` section at the TOP of progress.txt (create it if it doesn't exist). This section should consolidate the most important learnings:

```
## Codebase Patterns
- Example: Convention Plugins in build-logic/convention/
- Example: Use Type-Safe Navigation for routes
- Example: Room entities live in core:database, domain models in core:model
```

Only add patterns that are **general and reusable**, not story-specific details.

## Stop Condition

After completing a user story, check if ALL stories have `passes: true`.

If ALL stories are complete and passing, reply with:
<promise>COMPLETE</promise>

If there are still stories with `passes: false`, end your response normally (another iteration will pick up the next story).

In `--remote-run`, do **not** emit `<promise>COMPLETE</promise>`. Ralph shell decides completion after CI has marked every story as passed.

## PRD Generation

`scripts/ralph/prd.json` can be created in two ways:

1. **Manually** — copy `scripts/ralph/prd.json.example`, fill in the stories, and save to `scripts/ralph/prd.json`.
2. **Via Garlic (Telegram)** — send a task description to Garlic. Garlic invokes the `prd-agent` (defined at `scripts/ralph/prd-agent.md`), which explores the codebase and writes `scripts/ralph/prd.json` automatically, then triggers Ralph.

The `prd-agent.md` prompt is self-contained: it describes how to explore the MoneyManager codebase, the exact `prd.json` format, story decomposition rules, and Android-specific acceptance criteria.

## Important

- Work on ONE story per iteration
- Commit frequently
- Keep the build green
- Read the Codebase Patterns section in progress.txt before starting
- Follow existing code patterns in the MoneyManager project
- Read relevant `.claude/skills/*.md` files for detailed conventions
- In `--remote-run`, leave the branch clean and committed; Ralph shell handles push/PR/CI synchronization and pass-marking after CI
- Never commit `prd.json` or `progress.txt` to the `main` branch — they belong only in `ralph/*` feature branches
