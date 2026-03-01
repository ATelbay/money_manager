# Code Review Instructions

You are reviewing a Pull Request. Your job is to find real issues and fix them.

## Steps

1. Run `gh pr diff` to see all changes in this PR
2. Read the project's main `CLAUDE.md` for architecture conventions
3. Review the diff for:
   - Bugs, logic errors, off-by-one mistakes
   - Architecture violations (e.g. presentation depending on core:database)
   - Missing error handling at system boundaries
   - Security issues (hardcoded secrets, injection risks)
   - Broken patterns (not following existing codebase conventions)
   - Unused imports, dead code introduced in this PR
4. If you find issues — fix them, run `./gradlew assembleDebug`, commit with message `fix(review): <description>`, and push
5. If no issues found — do nothing

## Output

If you fixed something, end your response with:
<review>FIXED</review>

If the code is clean and no fixes were needed, end with:
<review>CLEAN</review>

## Important

- Only fix **real problems**, not style preferences
- Do NOT refactor working code just because you'd write it differently
- Do NOT add comments, docstrings, or type annotations unnecessarily
- Keep fixes minimal and focused
- Run `./gradlew assembleDebug` before pushing any fix
