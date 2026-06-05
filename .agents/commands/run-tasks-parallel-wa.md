Execute all pending tasks from the status line task list using Agent Teams with worktree isolation.

**Tip:** If context is large, consider running `/compact` first — tasks persist across compaction.

## 0. Execution model

- Use **Agent Teams** with `isolation: "worktree"` — not ad-hoc parallel agents
- If Agent Teams are unavailable or disabled, inform the user and suggest `/run-tasks-parallel` as fallback. Do not silently degrade
- Only one team runs per session — fully clean up before starting the next batch
- **Edge case:** if file-ownership analysis shows all tasks conflict (touch the same files), warn the user that parallelization is impossible and execute sequentially without worktree overhead

## 1. Gather tasks

- `TaskList` → filter to `pending` or `in_progress`
- If none exist, inform the user and stop
- Read each task's title, description, and priority. Sort: critical → high → medium → low

## 2. Batch by file ownership

- From each task's title and description, identify affected files, expected deliverable, and verification steps. If the description is too vague to determine affected files — use `Glob`/`Grep` to locate the relevant code before batching
- Group into sequential batches: **no two tasks in the same batch touch the same file**
- If affected files cannot be confidently identified — treat the task as conflicting and isolate it in its own sequential slot
- Prefer small, self-contained batches with clear deliverables
- **Batch ordering:** execute batches containing critical/high priority tasks first

## 3. Execute each batch

### Lead responsibilities
- Mark corresponding `TaskList` items as `in_progress`
- Create a shared team task list for the batch before teammates start
- Spawn teammates via `Agent` tool with `team_name` and `isolation: "worktree"` — min 1 / max 5 per batch
- Lead assigns tasks to teammates via `TaskUpdate` with `owner` — teammates do NOT pick tasks themselves
- Ensure no two teammates are assigned tasks that touch the same file
- Give each teammate complete context (they do NOT inherit conversation history):
  - Task title, full description, affected files, suggested approach
  - Acceptance criteria and verification steps
  - Relevant `CLAUDE.md` / `AGENTS.md` rules and architecture constraints
- Monitor via automatic message delivery and idle notifications. If a teammate reports a blocker, inspect and redirect or reassign
- When a teammate completes all assigned tasks, lead may assign next available tasks from the batch via `TaskUpdate`
- **Failed task retry limit:** if a task fails and is returned to `pending`, it may be retried **at most once**. On second failure, mark as `failed` with explanation — do not retry again

**Example — spawning a teammate:**
```
Agent tool call:
  name: "fix-account-screen"
  team_name: "batch-1"
  isolation: "worktree"
  mode: "bypassPermissions"
  prompt: |
    ## Task
    Fix overflow in AccountListScreen header text.

    ## Affected files
    - presentation/accounts/src/main/java/.../AccountListScreen.kt

    ## Acceptance criteria
    - Long account names truncate with ellipsis
    - No horizontal overflow on narrow screens

    ## Verification
    Run: ./gradlew :presentation:accounts:assembleDebug

    ## Architecture rules
    - Presentation modules NEVER depend on core:database
    - Use Material 3 components, TextAutoSize where appropriate
```

### Teammate responsibilities
- Work only within assigned scope
- Update shared team task status as work progresses
- Run the compilation command provided by the lead (default: `./gradlew assembleDebug`) in the worktree to confirm it compiles before marking done
- If blocked, explain the blocker clearly — do not guess or hack around it
- Do not touch files outside the assigned set unless the lead explicitly expands scope

## 4. Merge each batch

After all teammates in a batch complete:

1. **Pre-merge validation** — if a teammate already confirmed compilation in their worktree, skip re-running. Otherwise run `./gradlew assembleDebug` (with `timeout: 600000`). If compilation fails, flag that teammate's tasks and do NOT merge those changes
2. **Merge strategy** — for each worktree with passing compilation:
   - Worktrees are created in `.claude/worktrees/<name>` on branch `worktree-<name>`
   - `git diff` the worktree branch against the current branch to review changes
   - Merge into the current branch: `git merge worktree-<name> --no-ff`
   - If merge conflicts occur: `git merge --abort`, flag the task as `pending` with conflict description, skip it, and continue
3. **Post-merge smoke check** — `./gradlew assembleDebug` (with `timeout: 600000`) on the main worktree after each merge
   - Before each merge, save a rollback point: `git rev-parse HEAD` → `$PRE_MERGE_REF`
   - If smoke check fails, roll back: `git reset --hard $PRE_MERGE_REF`, and flag that task as `pending` with explanation
4. **Update `TaskList`:**
   - Successfully merged → `completed`
   - Failed compilation / merge conflict / blocked → `pending` with explanation comment
5. **Clean up** the team and worktrees before the next batch

## 5. Final verification

After all batches, run each step separately for clear diagnostics:

```bash
# Step 1 — compilation (required, timeout: 600000)
./gradlew assembleDebug
```

```bash
# Step 2 — tests (required, timeout: 600000)
./gradlew test
```

```bash
# Step 3 — static analysis (optional, timeout: 300000, does not block completion)
./gradlew lint detekt
```

- If build fails (step 1), identify which task's changes caused it and flag that task
- If tests fail (step 2), identify the failing test, trace to the responsible task, flag it
- If lint/detekt fail (step 3), fix minor issues inline or flag the task — do not block on this
- Run `TaskList` to show final status

## 6. Summary

Output:
- Batches executed (count and composition)
- Tasks completed ✅
- Tasks failed / returned to `open` ❌ (with reasons)
- Remaining open tasks
- Verification result (build / test / lint / detekt)
