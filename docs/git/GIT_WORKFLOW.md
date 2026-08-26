# Git Workflow

Professional workflow for the Temple Digital Services Platform.

## Three areas of Git

| Area | What it is |
|------|------------|
| **Working directory** | Files on disk you edit |
| **Staging area (index)** | Changes marked for the next commit |
| **Local repository** | Committed history on your machine (`.git/`) |

Flow: edit files → `git add` (stage) → `git commit` (save snapshot locally).

## Key concepts

- **HEAD** — pointer to your current commit (usually the tip of the checked-out branch).
- **Commit hash** — unique SHA-1 ID for a commit object, calculated from the tree, parent commit(s), author/committer metadata, timestamps, and commit message (e.g. `20b8cf2`).
- **Branch** — movable pointer to a line of commits.
- **Local branch** — e.g. `main`, `feature/module-01-git-workflow` (exists only on your machine until pushed).
- **Remote branch** — branch on GitHub (e.g. `main` on `origin`).
- **Remote-tracking branch** — local read-only pointer `origin/<branch>` updated by `fetch`; mirrors what Git last saw on the remote.

Deleting a branch on GitHub does **not** remove `origin/<branch>` locally until you refresh with `git fetch --prune`.

## Branching strategy

| Pattern | Use |
|---------|-----|
| `main` | Stable, merge target; protected later |
| `feature/*` | Module or feature work (primary for this project) |
| `fix/*` | Bug fixes |
| `hotfix/*` | Urgent production fixes |
| `release/*` | Release preparation (later) |

## This project's rules

- **Short-lived feature branches** — one module or focused change per branch.
- **Pull requests** — all module work merges via PR; no direct commits to `main`.
- **Squash merge** — default for module work; one clean commit on `main` per PR.
- **Protected `main`** — enable on GitHub later (require PR, reviews, status checks).

## Module 00 workflow (actual)

1. Branch: `feature/module-00-foundation`
2. Commits on feature branch
3. PR #1 → base `main`, compare feature branch
4. Squash merge → `20b8cf2 docs: complete Module 00 architecture foundation (#1)`
5. Feature branch deleted on GitHub
6. Stale `origin/feature/module-00-foundation` remained locally until `git fetch --prune`

Current work: `feature/module-01-git-workflow` from updated `main`.

## Merge strategies (GitHub)

| Strategy | Result |
|----------|--------|
| **Merge commit** | Preserves all branch commits; adds merge commit |
| **Squash and merge** | One commit on base; clean history (**preferred here**) |
| **Rebase and merge** | Linear history; rewrites branch commits onto base |

## Safety

- Do not commit directly to `main`.
- Do not force-push shared branches.
- Do not rewrite published history without team agreement.
