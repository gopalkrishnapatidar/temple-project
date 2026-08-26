# Git Commands

Reference for this project. Run from repo root.

## Repository setup

```powershell
git init          # create new repo (already done for this project)
git status        # working tree + staging summary
```

## Daily workflow

```powershell
git add <file>              # stage changes
git add .                   # stage all (review first)
git commit -m "message"     # commit staged changes
git log --oneline -10       # recent history
git log --oneline --graph --all   # branch graph
```

## Inspect changes

```powershell
git diff                    # unstaged: working directory vs staging
git diff --staged           # staged: staging vs last commit
git diff main               # compare working tree to main
```

### Module 01 diff exercise (this repo)

After creating `docs/git/*.md` without staging:

- `git diff` — shows new file contents as unstaged additions.
- `git add docs/git/GIT_WORKFLOW.md docs/git/GIT_COMMANDS.md`
- `git diff` — no output for staged files; other new files still show.
- `git diff --staged` — shows only what is staged for commit.

No commit was made during Module 01 implementation.

## Branches

```powershell
git branch                  # local branches
git branch -a               # local + remote-tracking
git checkout -b feature/x   # create and switch (legacy)
git switch -c feature/x     # create and switch (preferred)
git switch main             # switch branch
```

Current branches (after `git fetch --prune`):

- Local: `main`, `feature/module-01-git-workflow`
- Remote-tracking: `origin/main` (stale `origin/feature/module-00-foundation` removed by prune)

## Undo (safe, local)

```powershell
git restore <file>              # discard unstaged edits
git restore --staged <file>     # unstage, keep working copy
git rm <file>                   # remove and stage deletion
git mv old new                  # rename and stage
```

## Reset (conceptual — use carefully)

| Command | Effect |
|---------|--------|
| `git reset --soft HEAD~1` | Undo last commit; keep changes staged |
| `git reset --mixed HEAD~1` | Undo commit; unstage; keep files |
| `git reset --hard HEAD~1` | **Destructive** — discard commit and changes |

Never use `--hard` on shared/pushed history. Prefer `git revert` for published commits.

## Remote

```powershell
git remote -v                 # list remotes (origin → GitHub URL)
git fetch origin              # download commits; update origin/*
git fetch --prune             # fetch + remove stale origin/* refs
git pull origin main          # fetch + merge origin/main into current branch
git push -u origin feature/x  # push branch; set upstream tracking
```

### fetch vs pull

- **fetch** — updates remote-tracking branches only; does not change your working branch.
- **pull** — `fetch` + merge (or rebase) into current branch.

### local `main` vs `origin/main`

```powershell
git fetch --prune
git log main..origin/main --oneline    # commits on remote not in local main
git log origin/main..main --oneline    # commits on local main not on remote
```

After Module 00 squash merge, local `main` should match `origin/main` when both are updated.

## Project history (inspect)

```powershell
git log --oneline
# 20b8cf2 docs: complete Module 00 architecture foundation (#1)
# 2228f1d chore: initialize temple platform project foundation
```
