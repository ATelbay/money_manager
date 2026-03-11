If a spec name is provided as argument ($ARGUMENTS), use /specs/$ARGUMENTS/.
If no argument is provided, find the most recently modified spec folder in specs/.

1. Read spec.md — this is the source of truth for what should be implemented
2. Read tasks.md — check which tasks were planned
3. Run `git diff main --stat` to see what files were actually changed

Launch 4 agents in parallel for independent review passes.
Use `explore-sonnet` for Agents 1–3, use `explore-opus` for Agent 4.

**Agent 1 — Requirement Coverage (`explore-sonnet`):**
- For each requirement in spec.md, verify it is implemented in the diff
- Flag any requirement that is missing or partially done
- Check that all tasks from tasks.md have corresponding code changes

**Agent 2 — Scope & Unexpected Changes (`explore-sonnet`):**
- For each changed file NOT mentioned in tasks.md, flag as unexpected change
- Identify scope creep or unintended side effects
- Check for files that were supposed to change but didn't

**Agent 3 — Code Quality (`explore-sonnet`):**
- Review the diff for hardcoded values that should use the shared system
- Check for inconsistencies between what different parts implement
- Flag missing error handling, security issues, anti-patterns
- Verify naming conventions and architecture boundaries (CLAUDE.md rules)

**Agent 4 — Architecture Compliance (`explore-opus`):**
- Verify new/changed files are in the correct module per layer rules (CLAUDE.md)
- Check build.gradle.kts for dependency violations (e.g. presentation → core:database)
- Verify Hilt bindings are in data modules, not domain
- Check package naming follows `com.atelbay.money_manager.{layer}.{feature}` convention
- Ensure domain modules only contain interfaces + use cases, no impl details

After all agents complete:

1. Synthesize findings into a summary:
   - What's done ✅
   - What's missing ❌
   - What's suspicious ⚠️

2. For each issue that needs a fix, use TaskCreate:
   - Title: short description of the issue
   - Description: affected files, what's wrong, suggested fix
   - Priority: critical / high / medium / low
   - Group related findings into a single task where logical
   - Do NOT create tasks for informational notes or minor style nits

3. Output the summary table + created task count
