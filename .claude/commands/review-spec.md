If a spec name is provided as argument ($ARGUMENTS), use /specs/$ARGUMENTS/.
If no argument is provided, find the most recently modified spec folder in .specify/specs/.

1. Read spec.md — this is the source of truth for what should be implemented
2. Read tasks.md — check which tasks were planned
3. Run git diff main --stat to see what files were actually changed
4. For each requirement in spec.md:
   - Verify it is implemented in the diff
   - Flag any requirement that is missing or partially done
5. For each changed file NOT mentioned in tasks.md:
   - Flag as unexpected change — may be scope creep or side effect
6. Look for obvious code quality issues in the diff:
   - Hardcoded values that should use the shared system
   - Inconsistencies between what different teammates implemented
   - Missing error handling
7. Summarize: what's done, what's missing, what's suspicious