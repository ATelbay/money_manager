Execute all pending tasks from the status line task list using Agent Teams with worktree isolation.

**Before starting:** Suggest the user run `/compact` and keep only `TaskList` so the run starts with a smaller context. Tasks persist in `TaskList` and will not be lost. Show the current task count and ask: "Рекомендую выполнить `/compact` и оставить только `TaskList` перед запуском — таски сохранятся. Сжать контекст и перезапустить, или продолжить?" Wait for user confirmation before proceeding. Do not ask the user to enable UI-only options such as auto-accept edits.

0. Use the Agent Teams execution model explicitly:
   - Use Agent Teams, not ad-hoc parallel agents and not plain subagents
   - Treat the current session as the lead
   - If Agent Teams are unavailable or disabled in this session, inform the user and stop instead of silently falling back to a different execution model
   - Only one team can run per session, so always fully clean up the current team before starting another batch

1. Use `TaskList` to get all tasks. Filter to those with status `open` or `in_progress`.
   - If no pending tasks exist, inform the user and stop

2. Read each task's title, description, and priority. Sort by priority: critical -> high -> medium -> low.

3. Analyze tasks for batching and file ownership:
   - From each task's title and description, identify affected files, expected deliverable, and likely verification steps
   - Group tasks into sequential batches where no two tasks in the same batch should modify the same file
   - Tasks with overlapping files go into separate sequential batches
   - If the affected files cannot be identified confidently from a task's title and description, treat that task as conflicting and run it in its own sequential slot instead of parallelizing it
   - Prefer self-contained tasks with a clear deliverable over large, fuzzy batches

4. For each batch, create exactly one Agent Team with `isolation: "worktree"`:
   - The lead should create a shared team task list for that batch before teammates start working
   - The shared team task list may split one user task into multiple smaller team tasks when that improves delegation, but final `TaskList` status must still reflect the original user-facing tasks
   - Spawn an appropriate number of teammates for the batch, usually 3-5 for larger batches and fewer for smaller ones
   - Aim for roughly 5-6 shared team tasks per teammate when possible; if the batch is smaller, keep teammate count lower instead of over-spawning
   - Give each teammate enough context because teammates do not inherit the lead's conversation history

5. Lead responsibilities for each batch:
   - Mark each corresponding user `TaskList` item as `in_progress` before delegation begins
   - Assign teammates to distinct files or clearly separated deliverables
   - Give each teammate: task title, full description, affected files, suggested fix, acceptance criteria, and relevant `CLAUDE.md` or `AGENTS.md` context
   - Ensure no two teammates are expected to edit the same file
   - Monitor progress, answer teammate questions, and redirect work if a teammate is going down the wrong path
   - Wait for teammates to finish before synthesizing, merging, or starting implementation work in the lead session
   - If a teammate appears stuck or task status lags behind the actual work, inspect the result, nudge the teammate, or update the team task manually

6. Teammate responsibilities for each assigned task:
   - Work only within the assigned scope
   - Update shared team task status as work progresses
   - If blocked, explain the blocker clearly instead of guessing
   - Avoid touching files outside the assigned set unless the lead explicitly expands the scope

7. After each batch completes:
   - Review diffs from the batch's worktrees
   - Merge results into the main worktree
   - Run targeted verification for the completed batch when practical
   - Update the user `TaskList`:
     - Mark successfully completed tasks as `completed`
     - Return blocked or failed tasks to `open` and add a comment explaining why
   - Shut down and clean up the current team before starting the next batch

8. After all batches:
   - Detect the project's build tool and run the appropriate build + test command:
     - Gradle (`gradlew`): `./gradlew assembleDebug test`
     - npm/yarn/pnpm: `npm run build && npm test` (or equivalent)
     - Cargo: `cargo build && cargo test`
     - Make: `make build && make test`
     - If no build tool is detected, skip verification and note it in the summary
   - If the build fails, identify which task's changes most likely caused it and flag that task explicitly
   - Run `TaskList` again to show final status

9. Output summary:
   - Batches executed
   - Tasks completed ✅
   - Tasks failed or returned to `open` ❌
   - Remaining open tasks
   - Verification result
