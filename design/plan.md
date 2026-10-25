# Flux Launcher — Development Plan

## Project Overview
**Flux Launcher** is an ergonomic, minimalist Android launcher featuring glassmorphic theming, 3-page navigation (widgets • home • app drawer), adaptive widgets, and a custom icon engine (Anycons). Built with Clean Architecture + MVVM, Jetpack Compose, and Kotlin.

---

## Current State (as of analysis)

### Architecture Status
| Layer | Status | Notes |
|-------|--------|-------|
| Domain (`com.example.domain`) | **Partial** | Interfaces exist but some are incomplete; needs DI |
| Data (`com.example.data`) | **Partial** | Repositories exist but coupled; SharedPreferences instead of DataStore |
| UI (`com.example.ui`) | **Partial** | God ViewModel (`LauncherViewModel`); components being decomposed |
| System (`com.example.service`) | **Legacy** | Static state in services; needs lifecycle binding |

### Key Technical Debt (from `refactor.md`)
- **Phase 1 Foundation**: No DI (Hilt/Koin), SharedPreferences not DataStore, static service state
- **Phase 2 Decomposition**: `LauncherRepository` is a god object needing split into 10+ repositories
- **Phase 3 ViewModels**: Single `LauncherViewModel` needs decomposition into 6 feature ViewModels
- **Phase 4 UI Components**: Large composables need extraction into stateless presentational components
- **Phase 5 State Normalization**: No sealed UI state interfaces; scattered `StateFlow` fields
- **Phase 6 Testing**: Minimal tests; no CI pipeline; no lint/coverage gates
- **Phase 7 Modularization**: Single module; needs 15+ Gradle modules
- **Phase 8 Cleanup**: Hardcoded strings/URLs, empty catch blocks, commented dependencies

### Performance Debt (from `batteryNperf.md`)
- Main-thread I/O violations (news RSS fetch in composition)
- CPU blur storm in wallpaper backdrop (re-blurs every frame during swipe)
- Unbounded bitmap allocations; no cache recycling
- `while(true)` polling loops for clocks/media/notifications
- No Baseline Profile, no Macrobenchmark

### Theme Debt (from `frosted_glass.md`)
- 23 non-conforming surfaces hardcoding blur/translucency values
- Per-panel `RenderEffect` blurs own gradients instead of backdrop
- No blur on API 24–30 (minSdk=24)
- 58 SharedPreferences keys rewritten on every slider tick

### Feature: News Widget (from `newsWidget.md`)
- **Not implemented** — only placeholder RSS fetch in composition
- Full spec exists for Currents API integration with quota discipline, setup wizard, and all error states
- 13 implementation phases defined with file change map

---

## Development Phases

### Phase 0: Infrastructure & CI (Week 1) — **IMMEDIATE PRIORITY**
> **All builds, tests, validations run in GitHub Actions. No local builds required.**

| Task | Description | Acceptance |
|------|-------------|------------|
| 0.1 | Create GitHub Actions CI workflow | `./gradlew lint test assembleDebug assembleRelease` on PR/push |
| 0.2 | Create GitHub Actions Release workflow | Semantic versioning → signed APK artifacts on tag push |
| 0.3 | Configure Gradle for CI (no local secrets) | Keystore/env vars from GitHub Secrets |
| 0.4 | Add Detekt + KtLint with baseline | `./gradlew detekt ktlintCheck` passes |
| 0.5 | Add Baseline Profile generation (Macrobenchmark) | `./gradlew :app:generateBaselineProfile` works in CI |
| 0.6 | Verify public repo (if private quota limited) | Repo visibility public; Actions minutes sufficient |

**Deliverable**: Green CI badge; signed APK downloadable from GitHub Releases on every version tag.

---

### Phase 1: Foundation & DI (Week 2)
| Task | Description | Acceptance |
|------|-------------|------------|
| 1.1 | Add Hilt DI; `@HiltAndroidApp` on `FluxApplication` | `hilt` plugin applied; `Application` annotated |
| 1.2 | Migrate `SharedPreferences` → `Preferences DataStore` | `SettingsRepository` interface + impl; no `prefs.edit()` |
| 1.3 | Define serializable `LauncherSettings` data class | All 42 fields; `@Parcelize`/`@Serializable` |
| 1.4 | Remove static `instance` from `FluxNotificationListenerService`/`FluxAccessibilityService` | Services receive repos via `LocalContext`/entry points |
| 1.5 | Inject repositories into `LauncherViewModel` via Hilt | No manual instantiation in ViewModel/Activity |

---

### Phase 2: Repository Decomposition (Week 3–4)
Split `LauncherRepository` per `refactor.md` Phase 2:

| Repository | Responsibility | Interface Location |
|------------|----------------|---------------------|
| `AppRepository` | PackageManager queries, app list, favorites, folders | `domain/AppRepository.kt` |
| `SettingsRepository` | DataStore read/write for all settings | `domain/SettingsRepository.kt` |
| `NotificationRepository` | NotificationListenerService observation | `domain/NotificationRepository.kt` |
| `MediaPlaybackRepository` | MediaSessionManager observation | `domain/MediaPlaybackRepository.kt` |
| `WidgetRepository` | Left/carousel widget CRUD, system widget binding | `domain/WidgetRepository.kt` |
| `WallpaperRepository` | Auto-wallpaper scheduling, bitmap delivery | `domain/WallpaperRepository.kt` |
| `FolderRepository` | Folder CRUD + persistence | `domain/FolderRepository.kt` |
| `CategoryRepository` | User categories + app mapping | `domain/CategoryRepository.kt` |
| `DockRepository` | Dock slots + defaults | `domain/DockRepository.kt` |
| `IconPackRepository` | Installed icon pack detection | `domain/IconPackRepository.kt` |
| `CalendarRepository` | Calendar event reading | `domain/CalendarRepository.kt` |

**Wallpaper Sources** (extracted classes):
- `GitHubWallpaperSource`, `LocalWallpaperSource`, `GooglePhotosWallpaperSource`

---

### Phase 3: ViewModel Decomposition (Week 5)
| ViewModel | Scope | State Type |
|-----------|-------|------------|
| `HomeViewModel` | Center screen: favorites, carousel, dock, clock | `HomeUiState` (sealed) |
| `AllAppsViewModel` | App drawer: list, search, categories, folders | `AllAppsUiState` (sealed) |
| `WidgetsViewModel` | Left screen: widget list, edit mode, resize | `WidgetsUiState` (sealed) |
| `SettingsViewModel` | Settings UI: pages, dialogs, backup | `SettingsUiState` (data) |
| `SearchViewModel` | Search query, math eval, filtered results | `SearchUiState` |
| `NotificationCenterViewModel` | Notification list, reply, dismiss | `NotificationUiState` (sealed) |

- Each ViewModel exposes **exactly one** `StateFlow<UiState>`
- `SharingStarted.WhileSubscribed(5000)` for all UI flows
- Constructor injection only; no `MathEvaluator`/toast/vibrator in ViewModel

---

### Phase 4: UI Component Architecture (Week 6)
Extract presentational components (stateless, ≤10 params):

**Center Home** → `FavoritesSection`, `CarouselWidgetPager`, `DockBar`, `ClockHeader`
**Left Widgets** → `WidgetCard`, `WidgetEditControls`, `WidgetOptionsSheet`, widget bodies (`ClockWidget`, `WeatherWidget`, `MediaWidget`, `QuickNoteWidget`, `MindfulnessWidget`, `NotificationSummaryWidget`, `NewsWidget`, `DualClockWidget`, `SystemWidgetHost`)
**All Apps** → `AppSearchBar`, `CategorySection`, `FolderRow`, `LetterHeader`, `WaveAlphabet`
**Settings** → `SettingsStudioSheet` refactored with ViewModel-backed state holder

Move `SettingsActivity` content into `MainActivity` navigation destination.

---

### Phase 5: State Normalization (Week 7)
Define sealed UI state interfaces:
```kotlin
sealed interface HomeUiState { data class Content(...): HomeUiState; object Loading: HomeUiState; data class Error(...): HomeUiState }
sealed interface AllAppsUiState { ... }
sealed interface WidgetsUiState { ... }
data class SettingsUiState(...)
```
- Remove individual boolean `StateFlow` for dialogs; consolidate into UI state
- Use `combine`/`flatMapLatest` for derived state
- Annotate `LauncherSettings` + new `GlassStyle` with `@Immutable`

---

### Phase 6: Frosted Glass Theme Conformance (Week 8) — per `frosted_glass.md`
- Introduce `GlassStyle(translucency, blurDp, specular, enabled)` value type in `ui/theme/`
- Single factory validates/clamps once; all 16 call sites reduced to one `glass: GlassStyle` parameter
- Fix 23 non-conforming surfaces (sheets, dialogs, rows, backdrop)
- Remove double blur in `LauncherWallpaperBackdrop`
- Pre-API 31 blur fallback (pre-blurred bitmap path)
- Quantise blur radius to stop CPU storm during swipe
- Recycle blur bitmaps; cache across config changes
- Diff-based settings writes (stop 58-key rewrite storm)
- Roborazzi screenshot tests for every surface at min/max slider values

---

### Phase 7: News Widget Implementation (Week 9–10) — per `newsWidget.md`
Full Currents API integration:
1. Constants/models (`RemoteConfig`, `AppModel`: `NewsStory`, `NewsFeedState`, `NewsError`, `NewsPreferencesState`)
2. `NewsPreferences` (dedicated `flux_news_prefs`)
3. `NewsApiClient` (HttpURLConnection + org.json, header auth, quota headers)
4. `NewsRepository` interface + impl (TTL, quota gate, single-flight, backoff, disk cache)
5. `NewsWidget` composable (4 sizes, all 9 states, `isEmbeddedInCard`)
6. `NewsWidgetSetupSheet` (6-step wizard: welcome → key → language/region → categories → keywords → cadence)
7. Wire through `LauncherViewModel` → `MainActivity` → panels + `BackHandler`
8. Security: key only in `Authorization` header; never in BuildConfig/logs; redact in errors
9. Tests: URL builder, date parser, mapper, error mapper, quota policy, repository, UI states, Roborazzi goldens

---

### Phase 8: Performance & Battery (Week 11) — per `batteryNperf.md`
- StrictMode (disk/network/leaks) in debug
- LeakCanary debug build
- JankStats / FrameMetricsAggregator
- Macrobenchmark module + Baseline Profile
- Replace `while(true)` clock loops → shared ticker flow (per-sec only when seconds visible)
- Suspend polling on `onStop`; resume on `onStart`
- Debounce search (300ms) + package-change broadcasts
- HTTP cache / response cache for remote fetches
- Downsample all bitmap decodes to view size
- `batteryOptimizationMode` setting → real policy (reduce polling, disable background refresh)
- Release build verification: minify, shrink, R8 full mode, Baseline Profile measurable improvement

---

### Phase 9: Modularization (Week 12–13) — per `refactor.md` Phase 7
| Module | Type | Dependencies |
|--------|------|--------------|
| `:app` | Application | all feature modules |
| `:core:common` | Library | — |
| `:core:data` | Library | `:core:common` |
| `:core:domain` | Library (pure Kotlin) | `:core:common` |
| `:core:ui` | Library | `:core:common`, `:core:domain` |
| `:feature:home` | Feature | `:core:domain`, `:core:ui` |
| `:feature:appdrawer` | Feature | `:core:domain`, `:core:ui` |
| `:feature:widgets` | Feature | `:core:domain`, `:core:ui` |
| `:feature:notifications` | Feature | `:core:domain`, `:core:ui` |
| `:feature:wallpaper` | Feature | `:core:domain`, `:core:ui` |
| `:feature:settings` | Feature | `:core:domain`, `:core:ui` |
| `:feature:shortcuts` | Feature | `:core:domain`, `:core:ui` |
| `:feature:onboarding` | Feature | `:core:domain`, `:core:ui` |
| `:system:notificationlistener` | System | `:core:domain` |
| `:system:accessibility` | System | `:core:domain` |
| `:system:widgethost` | System | `:core:domain` |

- Feature modules **no dependencies on each other**
- `:core:domain` **zero Android dependencies**
- Convention plugins via `build-logic`

---

### Phase 10: Hardening & Release Prep (Week 14)
- Remove hardcoded package names → `KnownPackages`
- Remove hardcoded URLs → `RemoteConfig` / BuildConfig
- Remove hardcoded defaults → `Defaults` provider
- Replace all `catch (_: Throwable) {}` with Timber/logging
- Add `@VisibleForTesting` to testable internals
- Remove `MyApplicationTheme` alias, commented dependencies, unused secrets plugin
- ProGuard verification on release build
- Crash reporting (Firebase Crashlytics or Sentry)
- 60% unit test coverage threshold (domain + data)
- Final Roborazzi golden suite + manual matrix (API 24/29/31/34, both presets)

---

## Release & Versioning Strategy

### Semantic Versioning
| Component | Scheme | Example |
|-----------|--------|---------|
| **VersionName** | `MAJOR.MINOR.PATCH` | `1.2.3` |
| **VersionCode** | Monotonic integer (auto from timestamp) | `173000001` |
| **Git Tag** | `v{versionName}` | `v1.2.3` |

- **MAJOR**: Breaking changes (API, schema, minSdk bump)
- **MINOR**: New features (widgets, settings, integrations) — backward compatible
- **PATCH**: Bug fixes, performance, internal refactors

### Release Process (Automated via GitHub Actions)
1. Developer pushes tag `v1.2.3` (or uses "Release" UI)
2. CI workflow triggers on tag push
3. Gradle computes `versionCode` from timestamp; `versionName` from tag
4. Build signed **release APK** (and optionally AAB)
5. Run full test suite (unit + instrumented + Roborazzi)
6. Generate changelog from conventional commits
7. Create GitHub Release with APK artifact + changelog
8. (Optional) Publish to Play Console internal track

### Version Properties
`version.properties` is **not committed** (in `.gitignore`). CI generates it at build time:
```properties
# Auto-generated by CI — do not commit
versionCode=173000001
versionName=1.2.3
```

---

## GitHub Actions Workflows

### 1. `ci.yml` — Pull Request & Push Validation
```yaml
# Triggers: push to main, PR to main
# Jobs: lint → unit-test → instrumented-test (emulator) → assembleDebug → detekt → ktlint
# Artifacts: test reports, lint reports, debug APK (PR only)
```

### 2. `release.yml` — Semantic Release & APK
```yaml
# Trigger: push tag matching v*
# Jobs: validate-version → build-release-apk → sign → test → create-release
# Secrets: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_PASSWORD, KEY_ALIAS
# Artifact: app-release.apk attached to GitHub Release
```

### 3. `nightly.yml` — Scheduled Nightly Build (optional)
```yaml
# Trigger: cron (daily 02:00 UTC)
# Jobs: build-debug-apk → upload-artifact (retention 7 days)
```

---

## Definition of Done (per task)
- [ ] Code compiles (`./gradlew compileDebugKotlin`)
- [ ] Lint clean (`./gradlew lint`)
- [ ] Detekt/KtLint clean (`./gradlew detekt ktlintCheck`)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Instrumented tests pass on emulator (`./gradlew connectedAndroidTest`)
- [ ] Roborazzi screenshot tests pass (no regression vs goldens)
- [ ] No new `TODO`/`FIXME` without linked issue
- [ ] Documentation updated (ARCHITECTURE.md, relevant .md files)
- [ ] CI pipeline green on PR

---

## Risk Register
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Private repo Actions quota exhausted | High | Blocks CI | Promote to public repo immediately |
| Keystore misconfiguration in CI | Medium | Unsigned/unaligned APK | Test signing in PR workflow; validate keystore in release workflow |
| Baseline Profile generation flakes | Medium | No profile shipped | Retry logic; cache profile; allow skip on failure |
| Roborazzi emulator flakes | Medium | False negatives | `--no-animations`; stable emulator image; retry once |
| News API quota exhaustion during test | Low | 429 in CI | Mock `NewsApiClient` in tests; no real calls in CI |
| Modularization breaks builds | High | Blocked merges | Incremental module extraction; CI on each module PR |

---

## Success Metrics
| Metric | Target |
|--------|--------|
| CI build time (PR) | < 15 min |
| Release build time | < 20 min |
| Cold start (release, Baseline Profile) | < 800 ms |
| Peak PSS (idle home) | < 120 MB |
| Frame drops (app drawer scroll) | 0 jank > 16ms |
| Unit test coverage (domain+data) | ≥ 60% |
| APK size (release, minify+shrink) | < 8 MB |
| Zero critical/security lint issues | Enforced |

---

## Document Maintenance
- This plan is **living** — update checkboxes as phases complete
- Each phase completion → update `ARCHITECTURE.md` to reflect current structure
- Major scope changes → new GitHub Issue + update this plan
- All design docs (`refactor.md`, `frosted_glass.md`, `newsWidget.md`, `batteryNperf.md`) are **source of truth** for their domains; this plan references them, does not duplicate.