# Git Troubleshooting

Safe recovery patterns for this project. No force push. No rewriting published history.

## Undo unstaged file change

You edited a file but have not staged:

```powershell
git restore docs/git/GIT_WORKFLOW.md
```

Working copy reverts to last commit. Staged changes to other files are untouched.

## Unstage a file

You ran `git add` but should not commit that file yet:

```powershell
git restore --staged docs/git/GIT_COMMANDS.md
```

File stays modified in working directory; removed from staging area.

## Stale remote-tracking branch

**Symptom:** `git branch -a` shows `remotes/origin/feature/module-00-foundation` but GitHub deleted it after PR #1.

**Why:** Local Git caches remote refs from the last fetch. Deleting on GitHub does not auto-update your machine.

**Fix (safe):**

```powershell
git fetch --prune
```

Output example:

```text
- [deleted] (none) -> origin/feature/module-00-foundation
```

`--prune` removes remote-tracking refs whose upstream branch no longer exists. Does not delete your local branches.

## Recover from a bad local commit (unpushed)

| Situation | Approach |
|-----------|----------|
| Wrong message, same files | `git commit --amend` (only if not pushed) |
| Commit too early, keep changes | `git reset --soft HEAD~1` |
| Commit too early, unstage | `git reset HEAD~1` |
| Discard commit and changes | `git reset --hard HEAD~1` — **local only, never on shared history** |

## Recover from a bad pushed commit

Use **revert**, not reset:

```powershell
git revert <commit-hash>
```

Creates a new commit that undoes the change. History stays intact for collaborators.

Never `git push --force` to `main`.

## Merge conflict (safe example)

Two branches edit the same line in `docs/MODULE_STATUS.md`:

1. Git marks file with `<<<<<<<`, `=======`, `>>>>>>>`
2. Edit file: keep correct content, remove markers
3. `git add docs/MODULE_STATUS.md`
4. Complete merge or rebase

If stuck: `git merge --abort` or `git rebase --abort` returns to pre-merge state.

## local main behind origin/main

```powershell
git switch main
git fetch --prune
git pull origin main
```

## Accidentally committed secrets

1. Do **not** only delete in a follow-up commit if already pushed — secret may remain in history.
2. Rotate the secret immediately.
3. Remove from current files; seek team guidance for history cleanup (BFG, filter-repo) — advanced, coordinated.

Prevention: never commit `.env`, keys, or credentials (see AI_CONTEXT.md).

## Commands to avoid on shared branches

- `git push --force` / `--force-with-lease` on `main`
- `git reset --hard` after push
- `git rebase` of commits others already pulled
