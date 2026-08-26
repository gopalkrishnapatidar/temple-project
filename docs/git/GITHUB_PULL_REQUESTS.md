# GitHub Pull Requests

How module work merges into `main` for this project.

## Workflow

1. Update local `main`: `git switch main` → `git pull origin main`
2. Create feature branch: `git switch -c feature/module-XX-short-name`
3. Commit on feature branch (small, logical commits)
4. Push: `git push -u origin feature/module-XX-short-name`
5. Open PR on GitHub: **base** = `main`, **compare** = your feature branch
6. Review, address feedback, merge
7. Delete feature branch on GitHub after merge
8. Locally: `git fetch --prune` to drop stale `origin/<branch>` refs
9. Update local `main` and delete local feature branch when done

## PR terms

| Term | Meaning |
|------|---------|
| **Base branch** | Target (usually `main`) — receives the change |
| **Compare branch** | Your feature branch — source of the change |
| **Code review** | Teammate checks diff before merge |

## Module 00 example

- Compare: `feature/module-00-foundation`
- Base: `main`
- Merged: PR #1, squash merge
- Result on `main`: single commit `20b8cf2`
- GitHub deleted the feature branch; local `origin/feature/module-00-foundation` lingered until `git fetch --prune`

## Merge strategies

Use **squash and merge** for most module PRs:

- One commit per module on `main`
- Easier to read history and revert
- Module branch commits collapsed into one message

Use **merge commit** when preserving detailed branch history matters.

Use **rebase and merge** for linear history without squash; avoid on shared branches if others have pulled the feature branch.

## Merge conflicts

Happens when base and compare branch edit the same lines.

Safe resolution:

1. `git switch feature/x`
2. `git fetch origin`
3. `git merge origin/main` (or rebase — team policy)
4. Git marks conflicted files; edit to resolve
5. `git add <resolved-files>`
6. `git commit` (merge) or `git rebase --continue`
7. Push feature branch; PR updates

Do not force-push to `main`. Do not resolve by deleting others' changes without review.

## Protected main (later)

- Require PR before merge
- Require review
- Block force push
- Require CI checks (Module 28+)
