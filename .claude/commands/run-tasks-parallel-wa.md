Execute all pending tasks from the status line task list in parallel using worktree agents.

1. Use TaskList to get all tasks. Filter to those with status `open` or `in_progress`.
   - If no pending tasks exist, inform the user and stop.

2. Read each task's title, description, and priority. Sort by priority: critical → high → medium → low.

3. Analyze tasks for file overlap:
   - From each task's description, identify affected files
   - Group tasks into parallel batches where **no two tasks in the same batch touch the same file**
   - Tasks with overlapping files go into separate sequential batches

4. For each batch, launch Agent Teams (isolation: "worktree"):
   - Give each agent: task title, full description, affected files, and suggested fix
   - Include relevant context: CLAUDE.md rules, architecture constraints
   - Each agent should TaskUpdate its task to `in_progress` at start and `completed` when done
   - If an agent cannot complete a task, TaskUpdate with status `open` and add a comment explaining why

5. After each batch completes, review diffs and merge results before starting the next batch.

6. After all batches:
   - Run `./gradlew assembleDebug test` to verify compilation and tests pass
   - If build fails, identify which task's changes broke it and flag it
   - TaskList again to show final status

7. Output summary:
   - Tasks completed ✅
   - Tasks failed/skipped ❌
   - Remaining open tasks (if any)
