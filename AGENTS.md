# AGENTS.md — Development Rules for AI Agents & Contributors

> Binding rules for every agent, sub-agent, or contributor working in this repository
> (Flux Launcher). Read this file FIRST in every session. Violations = PR rejected.

---

## 1. Token Efficiency (always)

- Batch related shell operations into a single command; never run one trivial command per file.
- Prefer `grep -rn` over opening files wholesale; read only the lines you need (`sed -n`, `Read` with offset/limit).
- Edit with surgical `oldString/newString` patches — never rewrite whole files to change one line.
- Summaries: report file paths + one-line what/why, not diffs or full file dumps.
- Do not re-verify what was already verified in the same session unless its inputs changed.
- Keep prompts/outputs terse; no restating the task back.

## 2. Rate Limits & Quotas (never exhaust)

- Insert brief `sleep 1`–`sleep 2` pauses between rapid command batches; never fire parallel bursts of identical calls.
- GitHub Actions is a metered resource:
  - Docs/design-only changes (`**/*.md`, `design/**`, `docs/**`) must NOT trigger CI (enforced via `paths-ignore`).
  - Heavy jobs (full APK builds) run only on tags or explicit `workflow_dispatch` — never on ordinary pushes/PRs.
  - Always set `timeout-minutes` on jobs and `concurrency` with `cancel-in-progress: true`.
  - Prefer `gradle/actions/setup-gradle` caching; never run two workflows that duplicate the same Gradle work.
- External APIs (news/Currents, Gemini/AI, wallpaper sources):
  - No auto-fetch unless the user opted in (`autoRefreshEnabled`, default OFF).
  - Hard gate: auto-refetch blocked when quota remaining ≤ 10; user-forced refresh only.
  - TTL + disk cache + exponential backoff mandatory; no polling loops (`while (true)`).
- Local verification (Gradle with the local SDK) is preferred over CI for quick checks — it consumes zero Actions minutes.

## 3. STRIDE (security design, applied continuously)

- **Spoofing**: app identity via stable `applicationId` + signed releases only; never accept unsigned/foreign-keyed APKs for in-place updates.
- **Tampering**: release builds signed with the persistent keystore (GitHub Secrets `RELEASE_KEYSTORE_BASE64`); `release-key.jks` is gitignored; secrets never committed, never logged.
- **Repudiation**: meaningful commit messages with agent prefix (`agent-x: ...`); releases carry changelogs.
- **Information disclosure**: API keys only in `Authorization` headers; never in `BuildConfig`, logs, or crash traces (redact). `grep` for key leakage before every release.
- **Denial of Service**: quota gates + backoff (see §2); bounded caches with eviction; no unbounded bitmap/network allocation.
- **Elevation of Privilege**: exported components minimal (`exported="false"` unless a launcher/service requires it); runtime permissions requested contextually only.

## 4. SOLID (applied to Kotlin/Compose code)

- **Single Responsibility**: one module/feature per concern (`:core:*`, `:feature:*`, `:system:*`); one repository per data domain; ViewModels expose exactly one `StateFlow<UiState>`.
- **Open/Closed**: extend via sealed interfaces + new implementations (`GlassStyle`, `NewsError`, UI states) — never mutate shared contracts to add behavior.
- **Liskov Substitution**: implementations honour interface contracts (`NewsRepository` TTL/backoff semantics hold for any impl, incl. test fakes).
- **Interface Segregation**: narrow interfaces per consumer need; no god-repository (`LauncherRepository` split precedent).
- **Dependency Inversion**: UI layer depends on domain interfaces; concrete impls injected (Hilt) at composition roots; `:core:domain` stays pure Kotlin with ZERO Android dependencies.

## 5. Modular Code (always)

- Feature modules must not depend on each other; only on `:core:domain` + `:core:ui`.
- Convention plugins in `build-logic/` define module types (`library`, `feature`, `domain`); no copy-paste build scripts.
- Stub modules must compile: manifest without `package=` attr (AGP 9 rule), Compose runtime on classpath when the convention plugin applies the Compose compiler.
- Shared UI/motion/theming primitives live in `:core:ui` or `ui/theme/` (`GlassStyle`, `MotionTokens`) — never duplicated per screen.
- Every module change must keep the FULL Gradle graph green (`compileDebugKotlin` + `testDebugUnitTest` at root), not just `:app`.

---

## 6. Change Protocol

1. Read the relevant design doc first (`design/plan*.md`, `AGENTS.md` scope).
2. Surgical, module-scoped commits: `agent-<name>: <what>`.
3. Always Verify, Run Tests & Build in Github Actions, never locally.
4. Split each action to its own workflow so that every step is faster and can be performed parallelly.
5. Update `design/plan*.md` checkboxes when milestones complete.
6. Never delete design docs; never commit keystores/secrets/APKs/build outputs.
