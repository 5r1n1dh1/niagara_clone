# Agentic Development Contract — Flux Launcher

## Purpose
This document defines the agentic contract for completing pending development items in the Flux Launcher Android project. Each agent operates within a bounded scope, makes observable progress, and produces verifiable artifacts.

---

## Pending Items & Agent Contracts

### Agent A: Infrastructure & CI (`agent-ci`)
- **Scope**: Phase 0 (CI pipeline, semantic versioning, release automation)
- **Input**: `.github/workflows/build.yml`, `version.properties`, `gradle/`
- **Actions**:
  1. Generate `gradlew` executable and wrapper
  2. Commit `build.yml` modifications (zip/unzip artifacts)
  3. Create `release.yml` for tag-triggered semantic releases (`v*`)
  4. Configure `version.properties` with pinned `versionName=1.0.0` and `versionCode=1`
- **Output**: Green CI on PR; signed APK artifact; `v1.0.0` tag ready
- **Boundary**: Does not modify source code architecture (Phase 1-10)
- **Success**: `./gradlew assembleRelease` completes in GitHub Actions; release artifact uploaded

### Agent B: Foundation & DI (`agent-foundation`)
- **Scope**: Phase 1 (Hilt DI, DataStore migration, settings serialization)
- **Input**: `refactor.md` Phase 1, `design/plan.md`
- **Actions**:
  1. Apply `hilt` plugin; annotate `FluxApplication` with `@HiltAndroidApp`
  2. Replace `SharedPreferences` with `Preferences DataStore` in `SettingsRepository`
  3. Define `LauncherSettings` serializable data class (`@Serializable`)
  4. Remove static `instance` from `FluxNotificationListenerService` / `FluxAccessibilityService`
  5. Inject repositories into `LauncherViewModel`
- **Output**: `build.gradle.kts` updated; DI modules compile; services bound to lifecycle
- **Boundary**: Does not decompose `LauncherRepository` (Agent C scope)
- **Success**: `test` passes; `lint` clean; `detekt` passes

### Agent C: Repository Decomposition (`agent-repository`)
- **Scope**: Phase 2 (Split `LauncherRepository` into 10+ repositories)
- **Input**: `ARCHITECTURE.md`, domain interfaces
- **Actions**:
  1. Create interfaces: `AppRepository`, `NotificationRepository`, `MediaPlaybackRepository`, `WidgetRepository`, `WallpaperRepository`, `FolderRepository`, `CategoryRepository`, `DockRepository`, `IconPackRepository`, `CalendarRepository`
  2. Implement concrete repositories in `com.example.data`
  3. Extract wallpaper sources: `GitHubWallpaperSource`, `LocalWallpaperSource`, `GooglePhotosWallpaperSource`
- **Output**: Domain layer pure Kotlin; data layer concrete; no cross-feature dependencies
- **Boundary**: Does not modify ViewModels (Agent D) or UI components (Agent E)
- **Success**: Unit tests ≥60% for domain + data; `compileDebugKotlin` passes

### Agent D: ViewModel Decomposition (`agent-viewmodel`)
- **Scope**: Phase 3 (Decompose `LauncherViewModel` into 6 feature ViewModels)
- **Input**: `refactor.md` Phase 3, `design/plan.md`
- **Actions**:
  1. Create `HomeViewModel` (`HomeUiState` sealed), `AllAppsViewModel`, `WidgetsViewModel`, `SettingsViewModel`, `SearchViewModel`, `NotificationCenterViewModel`
  2. Each exposes exactly one `StateFlow<UiState>` with `SharingStarted.WhileSubscribed(5000)`
  3. Constructor injection only; remove `MathEvaluator`/toast/vibrator from ViewModels
- **Output**: Feature modules (`:feature:home`, `:feature:appdrawer`, etc.) prepared
- **Boundary**: Does not create modular Gradle modules (Agent F); only ViewModels
- **Success**: No god ViewModel; sealed UI states compile

### Agent E: UI Component Architecture (`agent-ui`)
- **Scope**: Phase 4 + Phase 6 (Stateless composables, glass theme conformance)
- **Input**: `frosted_glass.md`, `batteryNperf.md`
- **Actions**:
  1. Extract presentational components (≤10 params): `FavoritesSection`, `DockBar`, `ClockHeader`, `WidgetCard`, `AppSearchBar`, etc.
  2. Introduce `GlassStyle` value type; reduce 23 non-conforming surfaces to one parameter
  3. Remove double blur in `LauncherWallpaperBackdrop`; quantise blur radius
  4. Diff-based settings writes (stop 58-key rewrite storm)
- **Output**: `ui/theme` conformance; `public/` resources updated
- **Boundary**: Does not implement News Widget (Agent G) or modularization (Agent F)
- **Success**: Roborazzi goldens pass; no CPU blur storm; frame drops = 0 on scroll

### Agent F: News Widget (`agent-news`)
- **Scope**: Phase 7 (`newsWidget.md`)
- **Input**: `newsWidget.md`, `RemoteConfig`, `AppModel`
- **Actions**:
  1. Implement `NewsStory`, `NewsFeedState`, `NewsPreferencesState`, `NewsError` models
  2. Create `NewsPreferences` (`flux_news_prefs`)
  3. Implement `NewsApiClient` (HttpURLConnection + org.json, header auth, quota headers)
  4. Implement `NewsRepository` (TTL, quota gate, single-flight, backoff, disk cache)
  5. Create `NewsWidget` composable (4 sizes, 9 states) + `NewsWidgetSetupSheet` wizard
  6. Wire through `LauncherViewModel` → `MainActivity`
  7. Security: key only in `Authorization` header; redacted errors; no BuildConfig/log exposure
- **Output**: Full Currents API integration; setup wizard; error states handled
- **Boundary**: Key never committed; CI uses mock `NewsApiClient`; no real API calls in tests
- **Success**: All 13 implementation phases complete; UI states covered; goldens stable

### Agent G: Performance & Battery (`agent-performance`)
- **Scope**: Phase 8 (`batteryNperf.md`)
- **Input**: `batteryNperf.md`
- **Actions**:
  1. StrictMode (disk/network/leaks) in debug; LeakCanary debug build
  2. JankStats / FrameMetricsAggregator
  3. Replace `while(true)` loops → shared ticker flow (per-sec, only when visible)
  4. Suspend polling on `onStop`; resume `onStart`
  5. Debounce search (300ms) + package-change broadcasts
  6. HTTP cache for remote fetches; downsample bitmaps to view size
  7. `batteryOptimizationMode` setting → real policy (reduce polling, disable background refresh)
  8. Release verification: minify, shrink, R8 full mode, Baseline Profile
- **Output**: `build.gradle.kts` optimized; `Baseline Profile` generated; APK <8MB
- **Boundary**: Does not change architecture (Agents B-D); only performance plumbing
- **Success**: Cold start <800ms; Peak PSS <120MB; 0 jank >16ms; 60% coverage

### Agent H: Modularization (`agent-modular`)
- **Scope**: Phase 9 (15+ Gradle modules)
- **Input**: `refactor.md` Phase 7
- **Actions**:
  1. Create modules: `:core:common`, `:core:data`, `:core:domain`, `:core:ui`, `:feature:*`, `:system:*`
  2. Apply convention plugins via `build-logic`
  3. `:core:domain` zero Android dependencies; feature modules no cross-dependencies
- **Output**: Multi-module build; incremental builds; module-level CI
- **Boundary**: Must complete after Agents B, C, D (repositories and ViewModels defined)
- **Success**: `compileDebugKotlin` per module; `test` per module; build time <15 min

### Agent I: Hardening & Release (`agent-hardening`)
- **Scope**: Phase 10 + Release Prep
- **Input**: `ARCHITECTURE.md`, `version.properties`
- **Actions**:
  1. Remove hardcoded package names → `KnownPackages`; URLs → `RemoteConfig`
  2. Replace `catch (_: Throwable) {}` with Timber/logging
  3. Remove `MyApplicationTheme` alias, commented dependencies, unused secrets plugin
  4. ProGuard verification; Crash reporting configured
  5. Final Roborazzi golden suite; manual matrix (API 24/29/31/34)
  6. Create `v1.0.0` tag; create GitHub Release with APK + changelog
- **Output**: Clean, hardened release; no `TODO`/`FIXME` without linked issue; docs updated
- **Boundary**: Final agent; depends on A-H complete or in stable state
- **Success**: `v1.0.0` release published; semantic version enforced; APK signed

---

## Agentic Principles
1. **Autonomy**: Each agent decides implementation order within scope; no cross-agent edits without contract update.
2. **Observability**: Every agent produces a commit; CI must pass before agent completes.
3. **Safety**: Agents never delete design docs (`refactor.md`, `frosted_glass.md`, `newsWidget.md`, `batteryNperf.md`, `ARCHITECTURE.md`).
4. **Semantic Versioning**: Release tag = `v{MAJOR}.{MINOR}.{PATCH}`. For initial release: `v1.0.0`.
5. **Termination**: Agent completes when success criteria verified and artifacts committed/released.

---

## Contract Updates
- Modify this contract only via PR with agent identifier (e.g., `agent-ci: update contract`).
- Unimplemented phases must remain in contract until Agent I verifies completion.
