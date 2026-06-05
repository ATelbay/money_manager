If a spec name is provided as argument ($ARGUMENTS), read /specs/$ARGUMENTS/tasks.md and plan.md.
If no argument is provided, find the most recently modified spec folder in specs/ and use that.

1. Read tasks.md and plan.md to understand the full scope
2. Identify tasks tagged with [P] — these can run in parallel
3. Group [P] tasks into batches where **no two tasks touch the same file**
4. For each batch, launch Agent Teams (isolation: "worktree") — each agent gets its own git worktree:
   - Give each agent a clear list of tasks, file paths, and acceptance criteria from tasks.md
   - Include relevant context from plan.md and spec.md in the prompt
   - Agents can edit files independently without conflicts
5. Sequential (non-[P]) tasks: run after their dependencies complete, in the main worktree
6. After all agents finish, review diffs from each worktree and merge results
7. Run `./gradlew assembleDebug test` to verify everything compiles and passes