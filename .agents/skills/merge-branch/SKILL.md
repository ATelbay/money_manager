---
name: merge-branch
description: "Covers the safe workflow for merging a branch or pull request in Money Manager via the gh CLI: checking PR/merge state, waiting on required CI checks, respecting branch protection on main, and the strict rule to NEVER bypass branch policy with --admin without explicit user confirmation. Use when: asked to merge a PR or branch, completing a feature, finishing a review, resolving a BLOCKED merge state, or deciding between squash/merge/rebase."
user-invocable: true
---

# Merge a Branch / Pull Request

The default target is `main`. The repo uses **GitHub branch protection** on `main`
(required CI checks + required approving review), so a merge is rarely just one command.
Use the `gh` CLI; never push directly to `main`.

## Process

1. **Identify the PR.** `gh pr list` (or `gh pr view <n>`). If the user said "merge it"
   without a number and there's exactly one open PR for the current branch, that's the one —
   but confirm the number out loud before acting.

2. **Inspect merge state** before attempting anything:
   ```bash
   gh pr checks <n>
   gh pr view <n> --json mergeable,mergeStateStatus,reviewDecision,baseRefName
   ```
   - `mergeable: MERGEABLE` only means *no conflicts* — it does **not** mean policy is satisfied.
   - `mergeStateStatus: BLOCKED` means a branch-protection gate is unmet (pending checks,
     missing approval, out-of-date branch). Read `reviewDecision` and `gh pr checks` to find which.

3. **Wait for required checks** instead of force-merging:
   ```bash
   gh pr checks <n> --watch --interval 20
   ```
   The required checks here are **Compile Debug Kotlin**, **Lint & Analysis**, **Unit Tests**.

4. **Merge once green** (squash is the repo default — keeps history linear, matches the
   `chore:`/`feat:` single-commit style):
   ```bash
   gh pr merge <n> --squash --delete-branch
   ```
   Use `--merge` only if the user explicitly wants to preserve every commit; `--rebase` rarely.

5. **After merge**, sync local `main` and switch off the deleted branch:
   ```bash
   git checkout main && git pull
   ```

## Branch-protection gates — what each blocker means

| Symptom | Cause | Right action |
|---|---|---|
| `gh pr checks` shows `pending` | CI still running | `--watch`, then merge |
| Some check `fail` | Real CI failure | Fix it — do **not** override |
| `reviewDecision` empty + still `BLOCKED` | Required approving review missing | **Ask the user** (see below) |
| `mergeStateStatus: BEHIND` | Branch out of date with base | `gh pr update-branch <n>`, re-wait on checks |
| Auto-merge errors | `enablePullRequestAutoMerge` off for repo | Don't rely on `--auto`; watch + merge manually |

## CRITICAL: never bypass branch policy without explicit confirmation

`gh pr merge --admin` uses repo-admin privileges to **override** branch protection
(e.g. merging your own PR with no approving review). It is a legitimate `gh` feature, but it
is a **policy-affecting, hard-to-justify-after-the-fact action**.

- A user saying *"merge it"* authorizes a **normal** merge — it is **not** consent to override
  the review/branch policy.
- If the only remaining blocker is a **required approval** (which you cannot self-satisfy on
  your own PR), **STOP and ask**: explain that all checks pass but a required review is missing,
  and ask whether to (a) wait for a human reviewer to approve, or (b) admin-override.
- Only run `--admin` after the user **explicitly** says to override / bypass / force the merge.
- Never use `--admin` to paper over a *failing* check — fix the underlying problem.

> Lesson from PR #71: checks were green but `reviewDecision` was empty (required review).
> "merge it" was treated as consent for `--admin` — it wasn't. Ask first next time.

## Conflicts

If `mergeable: CONFLICTING`, don't merge. Surface the conflicting files, and either resolve
locally on the feature branch (`git merge main`, fix, push) or let the user decide. Never
resolve conflicts by blindly taking one side.
