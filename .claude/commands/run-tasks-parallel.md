Execute all pending tasks from the status line task list in parallel using subagents (no worktrees).
Best for small, isolated fixes that don't require file-level isolation.

**Before starting:** Suggest the user to run `/clear` first to free up context. Tasks persist in TaskList and won't be lost. Show the current task count and ask: "Рекомендую очистить контекст (`/clear`) перед запуском — таски сохранятся. Очистить и перезапустить, или продолжить?" Wait for user confirmation before proceeding.

1. Use TaskList to get all tasks. Filter to those with status `open` or `in_progress`.
   - If no pending tasks exist, inform the user and stop.

2. Read each task's title, description, and priority. Sort by priority: critical → high → medium → low.

3. Launch one Agent per task in parallel (no isolation — all agents work on the same repo):
   - Give each agent: task title, full description, affected files, and suggested fix
   - Include relevant context: CLAUDE.md rules, architecture constraints
   - Each agent should TaskUpdate its task to `in_progress` at start and `completed` when done
   - If an agent cannot complete a task, TaskUpdate with status `open` and add a comment explaining why
   - IMPORTANT: tasks must NOT touch overlapping files — if they do, warn the user and fall back to sequential execution for conflicting tasks

4. After all agents complete:
   - Run `./gradlew assembleDebug test` to verify compilation and tests pass
   - If build fails, identify which task's changes broke it and flag it
   - TaskList again to show final status

5. Output summary:
   - Tasks completed ✅
   - Tasks failed/skipped ❌
   - Remaining open tasks (if any)
