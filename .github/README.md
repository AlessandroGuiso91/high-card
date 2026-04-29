# Repository Infrastructure

This folder holds the GitHub-side automation for the project. Its sibling, `.githooks/`, holds the local git hooks. Together they form the lightweight quality pipeline chosen for this assessment.

The guiding principle is **minimal CI**: the pipeline enforces only what blocks integration (build, tests, dependency vulnerabilities). Everything else — code style, static analysis, complexity warnings — is handled locally by the developer's IDE.

---

## `workflows/ci.yml` — Continuous Integration

Triggered on every pull request and on every push to `main`. The job runs on `ubuntu-latest` with Temurin JDK 17 and a Maven cache, and performs:

1. **Build & test** — `./mvnw -B clean verify`. Fails the pipeline on any compile error or test failure.
2. **Dependency review** — `actions/dependency-review-action@v4`, fails the PR if any new dependency introduces a vulnerability of severity `high` or above. Runs **only on pull requests** (the action diffs base vs head, which has no meaning on a direct push). Backed by the GitHub Advisory Database — no NVD API key, no rate limiting, completes in seconds.

**What is intentionally NOT in CI:**
- **SonarCloud / SonarQube** — excluded to keep the pipeline fast and free of external account dependencies. Static analysis is performed locally via **SonarLint** in IntelliJ.
- **OWASP Dependency-Check Maven plugin** — initially adopted, then dropped: without an NVD API key the NVD download is rate-limited (HTTP 429) and the job hangs for 50+ minutes before failing. The GitHub-native action replaces it cleanly.
- **Coverage upload, release automation, deploy steps** — out of scope for this assessment.

The rationale and trade-offs are documented in `ai-assisted/pre-analysis.md` §2.5.

---

## `dependabot.yml` — Dependency updates

GitHub Dependabot is enabled with two ecosystems:

- **`maven`** — weekly scan of `pom.xml`, up to 5 open PRs at a time.
- **`github-actions`** — weekly scan of workflow action versions.

Both are configured to use the `chore` Conventional Commits prefix on the auto-generated PRs, so they fit the project's commit convention without manual rewriting.

Dependabot complements the dependency-review step: the review action **enforces on PR** (blocks merges that *introduce* a high-severity vulnerability), Dependabot **informs continuously** (opens PRs to fix or update vulnerable and outdated dependencies on the existing baseline). The two roles are different and intentionally redundant — one defends the PR boundary, the other patrols the main branch.

---

## `.githooks/pre-push` — Local pre-push hook

Lives in the sibling `.githooks/` folder, not here, but it is part of the same infrastructure. The script runs `./mvnw -q test` before every `git push`, blocking pushes that break the suite — the local equivalent of the CI test step, but caught before the PR is opened.

### One-time setup (required after cloning)

Git does not pick up the `.githooks/` directory automatically. Each developer must run, **once per clone**, on **every machine**:

```sh
git config core.hooksPath .githooks
```

This is a per-repository, per-clone local config — it is not stored in the repository itself and is not propagated by `git pull`. Reviewers cloning the project must run it too if they want the hook to fire.

The Husky-style auto-install path (e.g. binding the config to a Maven `initialize` goal via `git-build-hook-maven-plugin`) was considered and rejected: a plugin in `pom.xml` to save one shell command is not a worthwhile trade.
