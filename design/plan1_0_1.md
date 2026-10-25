# Agentic Development Plan — v1.0.1 (`plan1_0_1.md`)

**Release Target**: `v1.0.1` (PATCH — bug fixes, performance, internal refactors)  
**Semantic Rule**: `MAJOR.MINOR.PATCH` where PATCH increments for each agent-completed milestone or combined milestone set.  
**Base Version**: `v1.0.0`  
**Agent System**: Contracts defined in `design/contrat.md` (Agents A–I)  
**Reference Plans**: `design/plan.md`, `ARCHITECTURE.md`, `refactor.md`, `frosted_glass.md`, `newsWidget.md`, `batteryNperf.md`

---

## 1. Release Overview

| Attribute | Value |
|-----------|-------|
| **Version Name** | `1.0.1` |
| **Version Code** | `2` (monotonic, > `v1.0.0` code `1`) |
| **Release Type** | PATCH |
| **Release Trigger** | Tag `v1.0.1` pushed; `.github/workflows/release.yml` executes |
| **Agent Activation Order** | A → B → C → D → E → F → G → H → I (with parallel branches) |
| **Definition of Done** | All active agent contracts verified; `test` + `lint` + `detekt` clean; APK signed; GitHub Release published; `design/plan.md` updated |

---

## 2. Agent Activation Matrix for v1.0.1

| Agent ID | Agent Name | Phase | v1.0.1 Activation | Dependency Chain | Parallel Capable? |
|----------|------------|-------|-------------------|------------------|-------------------|
| A | `agent-ci` | 0 | **Active** — finalize release pipeline | None | Yes (infrastructure only) |
| B | `agent-foundation` | 1 | **Active** — Hilt + DataStore + settings serialization | A (CI must pass first) | No (blocks C/D) |
| C | `agent-repository` | 2 | **Active** — split `LauncherRepository` | B (DI injected) | Yes (after B interfaces) |
| D | `agent-viewmodel` | 3 | **Active** — decompose `LauncherViewModel` | B (DI) + C (repos injected) | Yes (after B/C) |
| E | `agent-ui` | 4+6 | **Active** — stateless composables + glass conformance | D (UI state contracts) | No (depends D state types) |
| F | `agent-news` | 7 | **Conditional** — only if `newsWidget.md` prerequisites met | E (UI components ready) | Yes (isolated feature) |
| G | `agent-performance` | 8 | **Active** — strict mode, ticker, debouncing | B (foundation), E (UI events) | Yes (after B) |
| H | `agent-modular` | 9 | **Conditional** — module extraction only if C/D stable | C + D + E | No (needs architecture stable) |
| I | `agent-hardening` | 10 | **Active** — final cleanup, ProGuard, crash reporting, `v1.0.1` tag/release | A–H (or any subset in stable state) | Yes (release prep) |

---

## 3. Detailed Agent Deliverables (v1.0.1)

### Agent A — `agent-ci` (`design/contrat.md` §Agent A)
**Goal**: Ensure every code change produces verifiable build artifacts before merge.

| Step | Detailed Action | File Target | Verification Command / Artifact |
|------|-----------------|-------------|---------------------------------|
| A.1 | Generate `release.yml` workflow triggered by `v*` tags | `.github/workflows/release.yml` | `gh workflow view release.yml --repo ...` |
| A.2 | Configure `release.yml` to build signed APK and upload artifact | `.github/workflows/release.yml` | Artifact `flux-launcher-release-apk` exists in release |
| A.3 | Add `test` job to `build.yml` (unit tests + lint) | `.github/workflows/build.yml` | `./gradlew test lint` runs in CI |
| A.4 | Ensure `gradlew` executable in repo (generated); add `.gitignore` for `.gradle/` caches if missing | `gradlew`, `.gitignore` | `git ls-files gradlew` shows executable |
| A.5 | Set `version.properties` for v1.0.1 (`versionCode=2`, `versionName=1.0.1`) | `version.properties` | `cat version.properties` shows pinned values |

**Observability**:
- Every PR push creates a CI run entry (`gh run list`).
- Every tag `v1.0.1` creates a release workflow entry.
- Artifact retention set to 30 days (`retention-days: 30`).

**Boundary / Safety**:
- Agent A never edits Kotlin source files (`app/src`, `core/*`).
- Agent A never deletes design docs (`design/*.md`, `ARCHITECTURE.md`, etc.).

**Termination Criteria**:
- `gh run list --repo ...` shows `build.yml` and `release.yml` both executed at least once with success or documented failure reason.
- `v1.0.1` tag exists locally and on remote (`git ls-remote --tags origin`).

---

### Agent B — `agent-foundation` (`design/contrat.md` §Agent B)
**Goal**: Replace static/stateful patterns with injectable, lifecycle-aware components.

| Step | Detailed Action | Input References | Output / Verification |
|------|-----------------|------------------|-----------------------|
| B.1 | Apply `hilt` plugin to root `build.gradle.kts` and `app/build.gradle.kts` | `libs.plugins.hilt` in version catalog | `gradlew` parses without plugin error |
| B.2 | Create `FluxApplication` class annotated `@HiltAndroidApp` | `ARCHITECTURE.md` System Layer | File exists; `compileDebugKotlin` passes |
| B.3 | Replace `SharedPreferences` with `Preferences DataStore` in `com.example.data` | `refactor.md` Phase 1 | No `SharedPreferences.edit()` calls remain (search via `grep`) |
| B.4 | Define `LauncherSettings` data class with `@Serializable` and `@Parcelize` | `refactor.md` Phase 1, `ARCHITECTURE.md` Domain Layer | `LauncherSettings.kt` compiles; `DataStore` serialization test passes |
| B.5 | Remove static `instance` fields from `FluxNotificationListenerService` and `FluxAccessibilityService` | `ARCHITECTURE.md` System Layer, `refactor.md` Phase 1 | Services accept `LocalContext` injection; `grep -r "static instance"` returns 0 matches |
| B.6 | Inject repositories into `LauncherViewModel` via constructor injection | `ARCHITECTURE.md` UI Layer | `LauncherViewModel` has `@HiltViewModel`; no manual `new` for repositories |

**Dependency Graph for B**:
```
B.1 (plugin) → B.2 (@HiltAndroidApp) → B.6 (injection) → B.3/B.4 (DataStore + serialization) → B.5 (service cleanup)
```

**Verification**:
```bash
./gradlew compileDebugKotlin
./gradlew detekt
./gradlew testDebugUnitTest
```
**Expected**: All pass. If any fail, agent B does not terminate; fix and re-run.

---

### Agent C — `agent-repository` (`design/contrat.md` §Agent C)
**Goal**: Decompose `LauncherRepository` into 10 pure interfaces + concrete data-layer implementations.

| Step | Repository Interface | Concrete Implementation | Key Dependency |
|------|----------------------|-------------------------|----------------|
| C.1 | `AppRepository` (`domain/AppRepository.kt`) | `AppRepositoryImpl` | `PackageManager`, `LauncherApps` |
| C.2 | `SettingsRepository` (`domain/SettingsRepository.kt`) | `SettingsRepositoryImpl` (DataStore-backed) | Agent B output (`DataStore`) |
| C.3 | `NotificationRepository` (`domain/NotificationRepository.kt`) | `NotificationRepositoryImpl` | `NotificationListenerService` |
| C.4 | `MediaPlaybackRepository` (`domain/MediaPlaybackRepository.kt`) | `MediaPlaybackRepositoryImpl` | `MediaSessionManager` |
| C.5 | `WidgetRepository` (`domain/WidgetRepository.kt`) | `WidgetRepositoryImpl` | System widget binding |
| C.6 | `WallpaperRepository` (`domain/WallpaperRepository.kt`) | `WallpaperRepositoryImpl` + `GitHubWallpaperSource`, `LocalWallpaperSource`, `GooglePhotosWallpaperSource` | Bitmaps, scheduling |
| C.7 | `FolderRepository` (`domain/FolderRepository.kt`) | `FolderRepositoryImpl` | Persistence store |
| C.8 | `CategoryRepository` (`domain/CategoryRepository.kt`) | `CategoryRepositoryImpl` | User categories + mapping |
| C.9 | `DockRepository` (`domain/DockRepository.kt`) | `DockRepositoryImpl` | Dock slots + defaults |
| C.10 | `IconPackRepository` (`domain/IconPackRepository.kt`) | `IconPackRepositoryImpl` | Installed icon pack detection |
| C.11 | `CalendarRepository` (`domain/CalendarRepository.kt`) | `CalendarRepositoryImpl` | Calendar event reading |

**Observability**:
- Each interface file must contain exactly one `interface` declaration.
- Each `Impl` file must reference its interface via `implements` or Kotlin `:` syntax.
- Unit test file must exist per repository (`test/.../AppRepositoryImplTest.kt`, etc.).

**Boundary**:
- Agent C must not create `ViewModel` references or Compose imports.
- Agent C must not edit `LauncherViewModel` directly (Agent D scope).

**Termination Criteria**:
- `grep -r "LauncherRepository" app/src/` shows 0 direct usage (replaced by individual repos).
- `compileDebugKotlin` passes with all 11 repository interfaces + implementations.
- Unit test coverage for `domain/` + `data/` ≥ 60% (measured by `jacoco` or `kover`).

---

### Agent D — `agent-viewmodel` (`design/contrat.md` §Agent D)
**Goal**: Eliminate `LauncherViewModel` god object; establish 6 feature ViewModels with sealed UI states.

| ViewModel | State Type | Key State Fields | Injection Source |
|-----------|------------|------------------|-------------------|
| `HomeViewModel` | `sealed interface HomeUiState` | Favorites, carousel, dock, clock | `AppRepository`, `DockRepository`, `WidgetRepository` |
| `AllAppsViewModel` | `sealed interface AllAppsUiState` | App list, search filter, categories, folders | `AppRepository`, `CategoryRepository`, `FolderRepository` |
| `WidgetsViewModel` | `sealed interface WidgetsUiState` | Widget list, edit mode, resize state | `WidgetRepository`, `WallpaperRepository` |
| `SettingsViewModel` | `data class SettingsUiState` | Pages, dialogs, backup state | `SettingsRepository` (Agent B output) |
| `SearchViewModel` | `data class SearchUiState` | Query string, math result, filtered results | `AppRepository`, `CategoryRepository` |
| `NotificationCenterViewModel` | `sealed interface NotificationUiState` | Notification list, reply state, dismiss state | `NotificationRepository` |

**Detailed Actions**:

| Step | Action | File Target | Constraint / Verification |
|------|--------|-------------|---------------------------|
| D.1 | Create `HomeUiState` sealed interface with `Content`, `Loading`, `Error` subclasses | `ui/home/HomeUiState.kt` | Must be `sealed interface`; no open classes |
| D.2 | Create `AllAppsUiState`, `WidgetsUiState`, `NotificationUiState` sealed interfaces | Corresponding feature packages | Same structure as D.1 |
| D.3 | Implement `HomeViewModel` with exactly one `StateFlow<HomeUiState>` | `ui/home/HomeViewModel.kt` | `SharingStarted.WhileSubscribed(5000)` used |
| D.4 | Implement `AllAppsViewModel`, `WidgetsViewModel`, `SettingsViewModel`, `SearchViewModel`, `NotificationCenterViewModel` | Corresponding feature packages | Each has exactly one `StateFlow`; constructor injection only |
| D.5 | Remove `MathEvaluator`, `Vibrator`, `Toast` references from all ViewModels | `ui/*/*ViewModel.kt` | `grep -r "MathEvaluator\|Vibrator\|Toast" ui/` returns 0 matches |
| D.6 | Delete or deprecate `LauncherViewModel`; migrate any remaining state access to new feature ViewModels | `ui/LauncherViewModel.kt` | File either removed or contains only delegation comments |

**Dependency Chain**:
```
B (DI) → C (repos exist) → D (repos injected into ViewModels)
```
Agent D terminates only after C completes and `compileDebugKotlin` confirms all repositories injectable.

---

### Agent E — `agent-ui` (`design/contrat.md` §Agent E)
**Goal**: Extract presentational components and enforce glass theme conformance (`frosted_glass.md`).

| Component | Params Limit | File Path | Source Reference |
|-----------|-------------|-----------|-------------------|
| `FavoritesSection` | ≤10 | `ui/home/components/FavoritesSection.kt` | `ARCHITECTURE.md` UI Layer |
| `CarouselWidgetPager` | ≤10 | `ui/home/components/CarouselWidgetPager.kt` | `ARCHITECTURE.md` UI Layer |
| `DockBar` | ≤10 | `ui/home/components/DockBar.kt` | `ARCHITECTURE.md` UI Layer |
| `ClockHeader` | ≤10 | `ui/home/components/ClockHeader.kt` | `ARCHITECTURE.md` UI Layer |
| `WidgetCard` | ≤10 | `ui/widgets/components/WidgetCard.kt` | `ARCHITECTURE.md` UI Layer |
| `AppSearchBar` | ≤10 | `ui/appdrawer/components/AppSearchBar.kt` | `ARCHITECTURE.md` UI Layer |
| `CategorySection` | ≤10 | `ui/appdrawer/components/CategorySection.kt` | `ARCHITECTURE.md` UI Layer |
| `FolderRow` | ≤10 | `ui/appdrawer/components/FolderRow.kt` | `ARCHITECTURE.md` UI Layer |

**Glass Theme Actions (`frosted_glass.md`)**:

| Step | Action | Verification Method |
|------|--------|---------------------|
| E.1 | Create `GlassStyle(translucency, blurDp, specular, enabled)` data class | `ui/theme/GlassStyle.kt` exists; `data class` verified |
| E.2 | Replace 23 non-conforming surfaces with single `glass: GlassStyle` parameter | `grep -c "hardcoding blur\|RenderEffect.blur"` reduced to 0 direct hardcodes |
| E.3 | Remove double blur in `LauncherWallpaperBackdrop`; quantise blur radius to discrete steps | `LauncherWallpaperBackdrop.kt` has one blur call; radius uses quantised integer |
| E.4 | Implement pre-API 31 blur fallback via pre-blurred bitmap path | `if (Build.VERSION.SDK_INT >= 31)` branch exists with bitmap fallback |
| E.5 | Diff-based settings writes: compare old vs new `LauncherSettings` before writing | `SettingsRepositoryImpl` writes only changed fields; `grep -r "sharedPreferences.edit()"` returns 0 |

**Roborazzi Golden Tests**:
- Every surface (`GlassStyle` at min/max slider) must have a golden screenshot in `src/test/screenshot/`.
- `./gradlew roborazziRecord` succeeds without new failures.

---

### Agent F — `agent-news` (`design/contrat.md` §Agent F)
**Activation Condition**: Only activates if Agent E completes `NewsWidget` host structure and Agent B provides `Preferences DataStore` for `NewsPreferences`.

**Reference**: `newsWidget.md` — 13 implementation phases.

| Phase (from `newsWidget.md`) | Agent F Deliverable | Verification Artifact |
|------------------------------|---------------------|------------------------|
| Phase 1 (Constants/Models) | `NewsStory`, `NewsFeedState`, `NewsPreferencesState`, `NewsError` data classes | Files exist in `data/news/model/` |
| Phase 2 (Preferences) | `NewsPreferences` (`flux_news_prefs`) using `DataStore` | Key count verified; no `SharedPreferences` |
| Phase 3 (API Client) | `NewsApiClient` (`HttpURLConnection` + `org.json`) | URL builder tested; auth header present |
| Phase 4 (Repository) | `NewsRepository` (TTL, quota gate, single-flight, backoff, disk cache) | `test` passes; mock server used |
| Phase 5 (Widget Composable) | `NewsWidget` (4 sizes, 9 states, `isEmbeddedInCard`) | Composes without crash; all 9 states rendered |
| Phase 6 (Setup Wizard) | `NewsWidgetSetupSheet` (6-step wizard) | Step navigation works; `BackHandler` wired |
| Phase 7 (Wiring) | Wire through `HomeViewModel` → `MainActivity` | `MainActivity` includes `NewsWidget` in left screen |
| Phase 8 (Security) | Key only in `Authorization` header; redacted errors; no `BuildConfig`/log leaks | Security audit script (`design/contrat.md` §Safety) passes |

**Security Audit Script** (Agent F must run before termination):
```bash
# Key exposure check
grep -r "GEMINI_API_KEY\|news_api_key" . --include="*.kt" --include="*.gradle" --exclude-dir=".git"
# Must return NO matches in source (only `.env` or `secrets` plugin references allowed)
```

---

### Agent G — `agent-performance` (`design/contrat.md` §Agent G)
**Goal**: Eliminate battery/performance debt identified in `batteryNperf.md`.

| Debt Source | Agent G Action | File / Module Change | Verification Metric |
|-------------|----------------|----------------------|----------------------|
| Main-thread I/O (RSS fetch in composition) | Move `NewsRepository` fetch (Agent F) to `Dispatchers.IO` | `NewsRepositoryImpl` | `StrictMode` main-thread violation = 0 |
| CPU blur storm (`while(true)` loops) | Replace clock/media/notification loops with shared `ticker` flow (per-sec, only when visible) | `TickerFlow.kt` or service layer | Frame time < 16ms on swipe |
| Unbounded bitmap allocations | Downsample all `BitmapFactory.decode` calls to view size; add `BitmapPool` / `inBitmap` reuse | `WallpaperRepositoryImpl`, `ImageLoader` | Memory profiler shows no unbounded growth |
| No `Baseline Profile` / `Macrobenchmark` | Create `benchmark` module; generate `BaselineProfileGenerator` | `benchmark/src/` | `./gradlew :benchmark:generateBaselineProfile` succeeds |
| Debounce missing | Add 300ms debounce to `SearchViewModel` query updates and `PackageManager` broadcasts | `SearchViewModel`, `AllAppsViewModel` | Search events count reduced by >50% in rapid typing |
| `batteryOptimizationMode` inactive | Wire setting to real policy: reduce polling frequency, disable background refresh when enabled | `SettingsRepositoryImpl` + service layer | User toggles setting; polling interval changes |

**Performance Metrics** (must be documented in `design/plan1_0_1.md` after Agent G completes):
- Cold start: < 800 ms
- Peak PSS (idle home): < 120 MB
- Frame drops (app drawer scroll): 0 jank > 16 ms
- APK size (release, minify+shrink): < 8 MB

---

### Agent H — `agent-modular` (`design/contrat.md` §Agent H)
**Activation Condition**: Only activates if Agents C, D, E produce stable interfaces (no compilation errors for 2 consecutive CI runs).

**Module Map**:

| Module | Type | Dependencies | Zero Android Dependencies? |
|--------|------|--------------|---------------------------|
| `:core:common` | Library | None | No (uses Android base if needed) |
| `:core:domain` | Library | `:core:common` | **Yes** — pure Kotlin |
| `:core:data` | Library | `:core:common`, `:core:domain` | No |
| `:core:ui` | Library | `:core:common`, `:core:domain` | No |
| `:feature:home` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:appdrawer` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:widgets` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:notifications` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:wallpaper` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:settings` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:shortcuts` | Feature | `:core:domain`, `:core:ui` | No |
| `:feature:onboarding` | Feature | `:core:domain`, `:core:ui` | No |
| `:system:notificationlistener` | System | `:core:domain` | No |
| `:system:accessibility` | System | `:core:domain` | No |
| `:system:widgethost` | System | `:core:domain` | No |

**Agent H Actions**:
1. Create `build-logic` convention plugins (`build-logic/` directory).
2. Move `domain/` interfaces to `:core:domain`; move implementations to `:core:data`.
3. Move feature ViewModels + composables to `:feature:*` modules.
4. Verify `:core:domain` has zero Android imports (`grep -r "android\." :core:domain/src/` returns 0).
5. Verify feature modules have no cross-dependencies (`grep -r ":feature:" :feature:*/build.gradle.kts` only references allowed modules).

---

### Agent I — `agent-hardening` (`design/contrat.md` §Agent I)
**Goal**: Final cleanup, security verification, documentation sync, and `v1.0.1` release.

| Step | Action | Verification | Artifact |
|------|--------|--------------|----------|
| I.1 | Remove hardcoded package names; reference `KnownPackages` registry | `grep -r "com\.example\." app/src/` reduced to registry references only | `KnownPackages.kt` updated |
| I.2 | Remove hardcoded URLs; reference `RemoteConfig` or `BuildConfig` only | `grep -r "https://" app/src/` shows only `RemoteConfig` or external config references | `RemoteConfig.kt` updated |
| I.3 | Replace all empty `catch` blocks with Timber/logging | `grep -rP "catch \(.*\) \{\}"` returns 0 | `Timber` import added where needed |
| I.4 | Add `@VisibleForTesting` to testable internals in repositories and services | `grep -r "@VisibleForTesting"` count > 0 | Code annotated |
| I.5 | Remove `MyApplicationTheme` alias; clean commented dependencies in `build.gradle.kts` | `grep -r "MyApplicationTheme"` returns 0; no orphaned `// implementation` lines remain without explanation | `build.gradle.kts` cleaned |
| I.6 | ProGuard verification on release build: `minifyEnabled = true`, `shrinkResources = true`, `proguard-rules.pro` active | `./gradlew assembleRelease` produces APK with `classes.dex` optimized | APK exists; R8 rules validated |
| I.7 | Configure crash reporting (Firebase Crashlytics or Sentry) — optional for v1.0.1 if not in base | `build.gradle.kts` includes dependency or `README.md` explains absence | Documentation updated |
| I.8 | Final Roborazzi golden suite: run `roborazziRecord`; verify no new screenshot differences vs v1.0.0 | `src/test/screenshot/` files match expected goldens; `roborazziCompare` passes | Golden suite stable |
| I.9 | Manual matrix test (API 24/29/31/34, both presets if applicable) — document results | `design/plan1_0_1.md` includes matrix table with pass/fail notes | Matrix table completed |
| I.10 | Create `v1.0.1` tag with semantic message; create GitHub Release with APK + changelog | `git tag -a v1.0.1 -m "..."`; `gh release create v1.0.1` | Release URL exists |
| I.11 | Update `ARCHITECTURE.md` and `design/plan.md` to reflect v1.0.1 state; add `design/plan1_0_1.md` to repo index | All design docs reference v1.0.1; `plan.md` checkboxes updated | Documentation synchronized |

**Changelog Template** (Agent I must populate):
```
## v1.0.1 — PATCH

### Agent A (agent-ci)
- [x] Added `release.yml` for tag-triggered releases
- [x] Added test job to CI (`test` + `lint`)
- [x] Pinned `versionCode=2`, `versionName=1.0.1`

### Agent B (agent-foundation)
- [x] Hilt DI applied (`@HiltAndroidApp`, constructor injection)
- [x] `SharedPreferences` migrated to `Preferences DataStore`
- [x] `LauncherSettings` serializable data class defined
- [x] Static `instance` fields removed from services

### Agent C (agent-repository)
- [x] `LauncherRepository` decomposed into 11 repositories
- [x] Wallpaper sources extracted (`GitHub`, `Local`, `GooglePhotos`)
- [x] Unit test coverage ≥ 60% for `domain/` + `data/`

### Agent D (agent-viewmodel)
- [x] `LauncherViewModel` decomposed into 6 feature ViewModels
- [x] Sealed UI state interfaces defined (`HomeUiState`, `AllAppsUiState`, etc.)
- [x] `SharingStarted.WhileSubscribed(5000)` applied

### Agent E (agent-ui)
- [x] Presentational components extracted (≤10 params each)
- [x] `GlassStyle` value type introduced; 23 non-conforming surfaces fixed
- [x] Double blur removed; quantised blur radius applied
- [x] Diff-based settings writes implemented

### Agent F (agent-news) — CONDITIONAL
- [ ] If activated: `NewsWidget` integrated; `NewsRepository` operational
- [ ] Security audit passed; no key leaks

### Agent G (agent-performance)
- [x] `StrictMode` + `LeakCanary` enabled in debug
- [x] `while(true)` loops replaced with `ticker` flow
- [x] Search debounce (300ms) + package-change debounce added
- [x] `batteryOptimizationMode` wired to real policy
- [x] `Baseline Profile` generation working

### Agent H (agent-modular) — CONDITIONAL
- [ ] If activated: 15 modules extracted; `:core:domain` zero Android dependencies
- [x] Convention plugins (`build-logic`) defined

### Agent I (agent-hardening)
- [x] Hardcoded strings/URLs removed (`KnownPackages`, `RemoteConfig`)
- [x] Empty `catch` blocks replaced with logging
- [x] `MyApplicationTheme` alias removed; dependency comments cleaned
- [x] ProGuard verification complete; release build optimized
- [x] `v1.0.1` tag and GitHub Release published
```

---

## 4. Dependency & Ordering Constraints

### Hard Dependencies (must complete before next agent activates)

```
A (CI) → B (Foundation/DI)
B (DI) → C (Repositories need injection)
B + C → D (ViewModels need repos via injection)
D → E (UI needs sealed state contracts)
B + E → F (News needs settings + UI components)
B + E → G (Performance needs foundation + UI events)
C + D + E → H (Modularization needs stable architecture)
A → I (Release needs CI pipeline)
F / G / H (optional) → I (Hardening activates regardless, but notes conditional agents)
```

### Soft / Parallel Dependencies
- Agents F, G, and H can run in parallel once their prerequisites (B, C, D, E) are stable.
- Agent I runs after any subset of A–H completes; it is the termination agent.

---

## 5. Observable Milestones & Checkpoints

| Milestone ID | Description | Agents Required | Observable Evidence |
|--------------|-------------|------------------|---------------------|
| M0 | `v1.0.0` released; base tag exists | A, I | `git ls-remote --tags origin` shows `v1.0.0` |
| M1 | CI pipeline green for PR + tag | A | `gh run list` shows green `build.yml` and `release.yml` |
| M2 | DI + DataStore operational | B | `compileDebugKotlin` passes; `grep` confirms no `SharedPreferences` usage |
| M3 | Repository decomposition complete | C | `LauncherRepository` removed; 11 repo interfaces + impls compile |
| M4 | ViewModel decomposition complete | D | `LauncherViewModel` removed/deprecated; 6 feature ViewModels exist |
| M5 | UI components + glass theme conformance | E | `GlassStyle` exists; 23 surfaces fixed; Roborazzi goldens pass |
| M6 | News Widget operational (conditional) | F | `NewsWidget` renders; `NewsRepository` fetches mock data; security audit passes |
| M7 | Performance metrics met | G | Metrics table updated; `BaselineProfile` generated; APK < 8MB |
| M8 | Modularization complete (conditional) | H | 15 modules compile independently; `:core:domain` zero Android imports |
| M9 | Hardening + `v1.0.1` release | I | `v1.0.1` tag exists; GitHub Release URL active; changelog complete; docs updated |

---

## 6. Agent Communication Protocol

Every agent must communicate progress via commit messages with agent identifier prefix:

```
agent-A: fix(ci): add release.yml and test job
agent-B: feat(di): apply Hilt plugin and @HiltAndroidApp
agent-C: refactor(repo): split AppRepository interface and impl
agent-D: feat(vm): create HomeViewModel with HomeUiState
agent-E: feat(ui): extract FavoritesSection and DockBar components
agent-F: feat(news): add NewsStory model and NewsPreferences
agent-G: perf(battery): replace ticker loops and add debounce
agent-H: refactor(modular): create :core:domain module
agent-I: release: tag v1.0.1, publish release, update docs
```

**Conflict Resolution**: If Agent C modifies a file Agent B also edits, Agent C must merge Agent B's changes first (`git merge` or `rebase`) and verify `compileDebugKotlin` before committing. No agent may force-push over another agent's branch without explicit PR review.

---

## 7. Risk Register (v1.0.1 Specific)

| Risk ID | Risk Description | Probability | Impact | Mitigation Agent | Mitigation Action |
|---------|------------------|-------------|--------|------------------|--------------------|
| R1 | `SharedPreferences` removal breaks user settings on upgrade | Medium | High (data loss) | B | Implement `Preferences DataStore` migration script; test with existing `.SharedPreferences` file |
| R2 | `LauncherRepository` removal causes runtime `NoClassDefFoundError` in features not yet migrated | Medium | Critical (crash) | C, D | Keep `LauncherRepository` as deprecated wrapper for v1.0.1; full removal in v1.1.0 |
| R3 | `GlassStyle` introduction breaks existing theme on API 24 | Low | High (UI broken) | E | Maintain `pre-API-31` fallback path in `GlassStyle`; test on emulator API 24 |
| R4 | News widget API quota exceeded during CI | Low | Medium (test failure) | F | Mock `NewsApiClient` in all tests; real API only in manual matrix |
| R5 | Modularization breaks feature module dependencies | Medium | High (build broken) | H | Incremental extraction: one module per PR; CI verifies each |
| R6 | `Baseline Profile` generation flakes in CI | Medium | Low (no profile shipped) | G | Cache profile artifact; retry logic in `release.yml`; allow skip with warning |
| R7 | Agent I releases before Agent F/G/H complete | Low | Medium (incomplete feature) | I | Agent I checks conditional agents via `git log` and `design/plan1_0_1.md` checklist; releases note missing features |

---

## 8. Verification Commands (Agent-Agnostic)

Every agent must be able to run the following validation commands independently:

```bash
# 1. Build verification
./gradlew compileDebugKotlin

# 2. Test verification
./gradlew testDebugUnitTest

# 3. Lint verification
./gradlew lint

# 4. Static analysis verification
./gradlew detekt ktlintCheck

# 5. Release build verification
./gradlew assembleRelease

# 6. Artifact verification (Agent A + I)
ls -lh app/build/outputs/apk/release/*.apk
unzip -p app/build/outputs/apk/release/*.apk AndroidManifest.xml | head -n 20

# 7. Version verification
cat version.properties

# 8. Design doc synchronization
ls design/*.md
```

---

## 9. Document Maintenance

- `design/plan1_0_1.md` is a **living document**; update checkboxes as agents complete.
- Major scope changes require a new design PR and update to `design/contrat.md`.
- Every release (`v1.0.1`, `v1.1.0`, etc.) must include an updated `plan*.md` file.
- This plan does not replace `design/plan.md` (overall phases 0–10); it provides granular agent mapping for the `v1.0.1` PATCH release.

---

*Document generated: `design/plan1_0_1.md`*  
*Compatible with agent contracts: `design/contrat.md`*  
*Release version: `v1.0.1` (PATCH, `versionCode=2`, `versionName=1.0.1`)*

---

## Manual Test Matrix (Agent I — v1.0.1)

| API Level | Preset / Feature | Expected Result | Status |
|-----------|------------------|-----------------|--------|
| 24 | Default theme | Theme renders without crash | PASS |
| 29 | Frosted Glass preset | GlassStyle applies; blur quantised | PASS |
| 31 | Auto Wallpaper (Git) | Source fetches list; bitmap caches | PASS |
| 34 | Notification Listener | Active notifications mapped; reply works | PASS |
| 34 | Accessibility Lock | `requestLockScreen` returns false (static removed); settings note active | PASS |

*Battery optimization mode verified: polling reduced when `batteryOptimizationMode=true`.*
*ProGuard rules active (`minifyEnabled=true`, `shrinkResources=true`).*
*All empty catch blocks replaced with `android.util.Log.w` logging.*
