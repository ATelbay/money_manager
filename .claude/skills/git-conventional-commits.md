---
description: "Use for analyzing changes (git diff) and creating commits in Conventional Commits format."
---

# Git Conventional Commits

## Process

1. Run `git status` and `git diff` to analyze uncommitted changes.
2. Form a commit message strictly in the format: `<type>(<scope>): <subject>`
3. Choose the correct `<type>`:
   - `feat`: new functionality (e.g., PDF parser).
   - `fix`: bug fix.
   - `test`: adding or updating UI tests (very important for this project!).
   - `refactor`: code refactoring without changing logic.
   - `chore`: dependency updates (in `libs.versions.toml`), build configuration.
4. Use the affected Gradle module or feature as `<scope>` (e.g., `import`, `database`, `ui`).
5. In the commit body, briefly explain the *reason* for the changes (why it was done), not just a list of changed files.
6. Ask for confirmation before running `git commit`.
