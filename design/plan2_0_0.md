# Plan v2.0.0 — CI Quota Safety, Release Naming, In-Place Updates, Glassmorphism, Performance & Animations

**Target**: `v2.0.0` (MINOR — new release pipeline guarantees + theme/perf/animation upgrades, backward compatible)
**Date**: 2026-10-29
**Constraints**: max 2 sub-agents; brief pauses between rapid command batches (rate limits); no design-doc deletion; no remote pushes by agents (orchestrator pushes).

---

## 1. Analysis & Research Summary

### 1.1 CI/CD is not green — root cause: **GitHub Actions quota exhausted by over-triggering**
- Evidence: workflow runs fail with `The job was not started because recent account payments have failed or your spending limit needs to be increased`.
- `build.yml` triggers on **every push to main AND every PR**, and each run executes a **full `assembleRelease` (~12s build + packaging + test)** plus zip/unzip steps. High frequency × long jobs = quota exhaustion.
- `release.yml` exists but also builds + tests inline, has no `flux.apk` naming, and never attaches the APK to the GitHub Release (only `|| true` creates it).
- Code is NOT the blocker — the local `assembleRelease` passes (`BUILD SUCCESSFUL`, signed APK produced).

### 1.2 APK name
- Release asset currently `app-release.apk`; user requires **`flux.apk`** on the releases page.

### 1.3 In-place updates
- `version.properties` pins `versionCode=2` / `versionName=1.0.1` → **future builds cannot update in place** (versionCode must strictly increase).
- Signing is inconsistent: release build falls back to `debug.keystore` when no keystore env/secrets exist; the currently published APK was signed with an ephemeral keystore (`my-production-alias`) that is not reproducible → **signature mismatch = in-place update failure** on the test phone.
- `applicationId = com.aistudio.fluxlauncher.vknwre` is stable (good — must not change).

### 1.4 Glassmorphism
- `GlassStyle` value type exists (`ui/theme/GlassStyle.kt`) with clamp + `enabled` derived from preset. Partial adoption only.
- `frosted_glass.md` documents **23 non-conforming surfaces** (hardcoded `15.dp / 0.30f / 0.30f` literals, dead params in `NewsWidget`/`YouTubeMusicWidget`/`NotificationSummaryWidget`, `isFrostedTheme = true` hardcoded in `SettingsStudioSheet`, wallpaper backdrop scrim literals, `WidgetResizeAndOptionsSheet` unwired, etc.).
- Rendering waste: re-blur per frame/composition, unquantised radius blur storm, pre-API-31 fallback missing in places (minSdk=24).

### 1.5 Refactor / performance
- `batteryNperf.md` items largely implemented (StrictMode, LeakCanary, TickerFlow, 300ms debounce, downsample/recycle, batteryOptimizationMode). Remaining: `while (true)` clock loop in `CenterHomeScreen.kt:62`; icon-cache consolidation; `System.gc()` removal; lazy-list `key`/`contentType` audit; recomposition skippability; JankStats/macrobenchmark.

### 1.6 Animations
- No shared motion system: sheets/popups/dock/pager use default/absent animations. Need a single `MotionTokens` spec (springs/easings) + `graphicsLayer` transforms + `AnimatedVisibility`/`AnimatedContent` transitions + slider-tick feedback.

### 1.7 API quota protection (**most important**)
- News (Currents API): `NewsRepositoryImpl` already implements TTL, quota-aware backoff, disk cache, single-flight, `batteryOptimizationMode` gating. Needs hard gate: **never fetch when `rateLimitRemaining` < reserve threshold unless user forces**, and no auto-fetch on init before first user gesture.
- Gemini AI: `GEMINI_API_KEY` is only in `.env.example`; `libs.firebase.ai` is commented out — confirm zero auto-calls at runtime.

---

## 2. Work Split (exactly 2 agents, parallel)

### Agent A — `agent-ci-release` (CI/CD + release + in-place updates + quota-safe CI)
Files: `.github/workflows/build.yml`, `.github/workflows/release.yml`, `version.properties`, `app/build.gradle.kts` (version/signing/maxVersionCode), `.gitignore`, `docs/RELEASE.md` (new).

1. **Quota-safe `build.yml`** (rewrite):
   - `on: push: branches: [main] paths-ignore: ['**/*.md', 'design/**', 'docs/**']` + `pull_request` (only on PRs, cheap) + `workflow_dispatch`.
   - `concurrency: group: ci-${{ github.ref }}, cancel-in-progress: true`.
   - `timeout-minutes: 15` on every job.
   - PR job = **cheap only**: `compileDebugKotlin` + `testDebugUnitTest` + `lint` + `detekt` (no `assembleRelease`), `continue-on-error` for lint/detekt so docs/test-only pushes stay green; artifact retention 7 days.
   - Push-to-main job = `assembleDebug` only (fast smoke). Full release build **only** on tags / dispatch (`release.yml`).
2. **Quota-safe `release.yml`** (rewrite): triggers `push tags v*` + `workflow_dispatch`; single job, `timeout-minutes: 20`; build → rename APK copy to **`flux.apk`** → attach to existing release (`gh release upload <tag> flux.apk --clobber`) with `continue-on-error: false`; Gradle cache via `gradle/actions/setup-gradle`; skip steps with `if` when no keystore.
3. **Release asset rename (now)**: delete `app-release.apk` from `v1.0.1` release, upload `flux.apk` from the locally built `app/build/outputs/apk/release/app-release.apk`.
4. **In-place updates**:
   - `version.properties`: comment out pinned values (restore auto timestamp `versionCode`, `1.0.<code>` name) so every build > previous; document one-time uninstall requirement when signing key changes.
   - Persistent signing: create `release-key.jks` (alias `flux-release`, local, **gitignored**) + set GitHub secrets `RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS` via `gh secret set`; `build.gradle.kts` keeps existing env-first logic; add `maxOf(autoVersionCode, pinned...)` guard so auto always increases.
   - Document in `docs/RELEASE.md`: install `flux.apk` from releases; after the key change there is **one mandatory uninstall**; thereafter all in-place updates succeed.
5. Commit `agent-ci-release: ...` (files only). No push.

### Agent B — `agent-glass-anim` (glassmorphism + animations + perf + API quota gate)
Files: `app/src/main/**` (theme, components, viewmodel, data).

1. **Glassmorphism conformance** (`frosted_glass.md`):
   - Thread a single `glass: GlassStyle` into all 16+ call sites; delete hardcoded `15.dp / 0.30f / 0.30f` literals (NewsWidget, YouTubeMusicWidget, NotificationSummaryWidget, SettingsStudioSheet ×2, WidgetResizeAndOptionsSheet call site, etc.).
   - `SettingsStudioSheet` `isFrostedTheme = true` → derived from `glass.enabled` (live slider feedback + correct preset fallback).
   - `FixedDock` `isFrostedTheme` wiring + `GlassStyle` fallback instead of hardcoded `0.15f`.
   - `FrostedGlassPanel` gains explicit `enabled`; `LauncherWallpaperBackdrop` scrim alphas derived from `GlassStyle` (no hex literals).
   - Rendering: blur applied once per surface to a cached `RenderEffect` on API 31+; radius **quantised to integer steps** (no per-frame re-blur); pre-API-31 pre-blurred bitmap fallback with `Bitmap` recycle; blur layer removed off-screen (`Modifier.graphicsLayer`).
2. **Animations** (`ui/theme/MotionTokens.kt` new):
   - Spring specs (`dampingRatioMediumLow`/`stiffnessLow` for sheets, `MediumLow` for dock), `FastOutSlowInEasing` tweens, `AnimatedVisibility` (fade+scale) for sheet/dialog/popup entry, `AnimatedContent` (`SizeTransform`) for widget state changes, `Crossfade` for screen transitions, `animateFloatAsState` for specular-highlight strength, pager `snapFlingAnimation` via `PagerState.animateScrollToPage`.
   - Infinite/periodic animations pause when not visible (`LifecycleEventEffect`/`LaunchedEffect` keys); no animation work in background.
3. **Performance refactor**: replace `CenterHomeScreen.kt:62` `while (true)` with `TickerFlow`; lazy lists get stable `key` + `contentType`; immutable model state; `derivedStateOf` for expensive derivations; consolidate icon caches (single eviction policy) + `onTrimMemory` clearing; remove `System.gc()` calls; skip unstable lambda params on hot items.
4. **API quota gate (most important)**: in `NewsRepositoryImpl` add `QuotaPolicy` — skip auto refresh when `rateLimitRemaining <= 10` (show `Stale`/`QuotaExceeded` + user "retry" only via forced refresh); never auto-fetch in `init` when quota exhausted; unit test for the policy with mocked client. Verify zero Gemini/AI auto-calls (grep).
5. Verify with local build (SDK at `/tmp/android-sdk`, Gradle at `/tmp/gradle_tmp/gradle-9.3.1/bin/gradle`): `:app:compileDebugKotlin` must pass; run `testDebugUnitTest` if feasible. Commit `agent-glass-anim: ...`. No push.

---

## 3. Verification (orchestrator)
- Local: `:app:assembleDebug` + `:app:assembleRelease` pass; unit tests pass.
- `gh release view v1.0.1 --json assets` shows **`flux.apk`**.
- `gh secret list` shows the three release secrets.
- `version.properties` auto-mode → `versionCode` increases each build (print resolved values).
- In-place update checklist documented: same keystore + monotonic versionCode + unchanged applicationId.
- CI: `build.yml` no longer runs release builds on push; estimate quota reduction (jobs×minutes) in final report.

## 4. Rollout
- Commit both agent PRs, orchestrator pushes `main`, tags `v2.0.0` after CI is green (CI still requires billing fix; document link `https://github.com/settings/billing`).
- User installs `flux.apk` from `v1.0.1` (one-time uninstall due to new signing key) → all future releases update in place.
