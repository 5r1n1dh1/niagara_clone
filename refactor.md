# Flux Launcher — Refactoring Acceptance Criteria

## Phase 1: Foundation

- [ ] Add Hilt (or Koin) dependency injection to the project
- [ ] Create an `Application` class annotated with `@HiltAndroidApp`
- [ ] Annotate `MainActivity` and `SettingsActivity` with `@AndroidEntryPoint`
- [ ] Replace all `SharedPreferences` usage with Jetpack DataStore (Proto or Preferences)
- [ ] Define a serializable `LauncherSettings` data class with all current settings fields
- [ ] Create a `SettingsRepository` interface with a DataStore-backed implementation
- [ ] Remove all manual `prefs.edit()` put/get calls from `LauncherRepository`
- [ ] Create an `AppRepository` interface exposing app list, favorites, folders, and reload operations
- [ ] Create a `NotificationRepository` interface exposing active notifications and media playback flows
- [ ] Remove static `instance` references from `FluxNotificationListenerService` and `FluxAccessibilityService`
- [ ] Remove companion-object `StateFlow` fields from service classes
- [ ] Inject repositories into `LauncherViewModel` via Hilt constructor injection
- [ ] Remove direct instantiation of `LauncherRepository`, `WidgetIntegrationsManager`, and `AutoWallpaperManager` from ViewModel and Activity

## Phase 2: Decompose God Objects

- [ ] Split `LauncherRepository` into domain-specific repositories:
  - [ ] `AppRepositoryImpl` — handles PackageManager queries, LauncherApps, BroadcastReceivers for package changes
  - [ ] `SettingsRepositoryImpl` — handles DataStore read/write for all settings
  - [ ] `NotificationRepositoryImpl` — observes NotificationListenerService and exposes flows
  - [ ] `MediaPlaybackRepository` — observes MediaSessionManager and exposes playback state
  - [ ] `WidgetRepository` — handles left widget CRUD, carousel widget CRUD, and system widget binding
  - [ ] `WallpaperRepository` — handles auto-wallpaper scheduling and bitmap delivery
  - [ ] `FolderRepository` — handles folder CRUD and persistence
  - [ ] `CategoryRepository` — handles user categories and app-to-category mapping
  - [ ] `DockRepository` — handles dock slot persistence and default dock population
  - [ ] `IconPackRepository` — handles installed icon pack detection
- [ ] Extract wallpaper source resolvers into separate classes:
  - [ ] `GitHubWallpaperSource` — fetches wallpaper list from GitHub API
  - [ ] `LocalWallpaperSource` — scans local directories for images
  - [ ] `GooglePhotosWallpaperSource` — scrapes Google Photos album pages
- [ ] Extract app loading logic into a dedicated `AppLoader` class
- [ ] Extract calendar reading logic into a `CalendarRepository`
- [ ] Ensure each repository has a corresponding interface in the domain layer
- [ ] Ensure no repository directly instantiates another repository

## Phase 3: ViewModel Decomposition

- [ ] Create a `HomeViewModel` that only manages center-screen UI state (favorites, carousel, dock, clock)
- [ ] Create an `AllAppsViewModel` that only manages all-apps-screen UI state (list, search, categories, folders)
- [ ] Create a `WidgetsViewModel` that only manages left-screen UI state (widget list, edit mode, resize)
- [ ] Create a `SettingsViewModel` that only manages settings UI state (current page, dialogs, backup)
- [ ] Create a `SearchViewModel` that manages search query, math evaluation, and filtered results
- [ ] Create a `NotificationCenterViewModel` that manages notification list, reply, and dismiss actions
- [ ] Remove `MathEvaluator` from `LauncherViewModel` and place it in a utility/domain class
- [ ] Remove toast and vibrator helpers from ViewModel; use a `UserFeedback` abstraction
- [ ] Remove `triggerSystemNotificationShade` reflection hack from ViewModel; encapsulate in a system service wrapper
- [ ] Ensure no ViewModel exposes more than 5 StateFlow fields to the UI
- [ ] Ensure all ViewModel dependencies are injected via constructor

## Phase 4: UI Component Architecture

- [ ] Extract each pager page into its own Composable screen with a dedicated ViewModel
- [ ] Break `CenterHomeScreen` into smaller presentational components:
  - [ ] `FavoritesSection` — displays favorite app list
  - [ ] `CarouselWidgetPager` — displays swipable widget cards
  - [ ] `DockBar` — displays fixed dock icons
- [ ] Break `LeftWidgetsScreen` into smaller presentational components:
  - [ ] `WidgetCard` — displays a single widget with header and body
  - [ ] `WidgetEditControls` — resize chips, move up/down, delete buttons
  - [ ] `WidgetOptionsSheet` — bottom sheet for resize and reorder
- [ ] Break `RightAllAppsScreen` into smaller presentational components:
  - [ ] `AppSearchBar` — search input with clear and settings buttons
  - [ ] `CategorySection` — expandable category card with app list
  - [ ] `FolderRow` — folder header with expandable app list
  - [ ] `LetterHeader` — alphabet section header
  - [ ] `WaveAlphabet` — fast-scroll alphabet sidebar
- [ ] Extract widget body composables from `LeftWidgetsScreen.kt` into standalone files:
  - [ ] `ClockWidget`
  - [ ] `WeatherWidget`
  - [ ] `MediaWidget`
  - [ ] `QuickNoteWidget`
  - [ ] `MindfulnessWidget`
  - [ ] `NotificationSummaryWidget`
  - [ ] `NewsWidget`
  - [ ] `DualClockWidget`
  - [ ] `SystemWidgetHost`
- [ ] Reduce `SettingsStudioSheet` parameter count below 10 by using a ViewModel-backed state holder
- [ ] Ensure no Composable function has more than 10 parameters
- [ ] Ensure all presentational components are stateless (state hoisted to ViewModel)
- [ ] Move `SettingsActivity` content into a navigation destination within `MainActivity`

## Phase 5: State Normalization

- [ ] Define a `HomeUiState` sealed interface with Loading, Content, and Error variants
- [ ] Define an `AllAppsUiState` sealed interface with Loading, Content, and Error variants
- [ ] Define a `WidgetsUiState` sealed interface with Loading, Content, and Error variants
- [ ] Define a `SettingsUiState` data class holding all settings-page UI state
- [ ] Ensure each screen ViewModel exposes exactly one `StateFlow` of its UI state type
- [ ] Remove all individual boolean `StateFlow` fields for dialog/sheet visibility from ViewModels
- [ ] Consolidate dialog visibility into the screen's UI state
- [ ] Use `combine` or `flatMapLatest` to derive composed state from multiple repository flows
- [ ] Ensure `SharingStarted.WhileSubscribed(5000)` is used for all UI-facing flows
- [ ] Remove `SharingStarted.Eagerly` from all flows except truly global state

## Phase 6: Testing and Quality

- [ ] Add unit test infrastructure with JUnit5, Turbine, and MockK
- [ ] Write unit tests for `SettingsRepositoryImpl` (mock DataStore)
- [ ] Write unit tests for `AppRepositoryImpl` (mock PackageManager and LauncherApps)
- [ ] Write unit tests for `WallpaperRepository` and all three wallpaper sources (mock HttpURLConnection)
- [ ] Write unit tests for `MathEvaluator`
- [ ] Write unit tests for `WidgetIntegrationsManager`
- [ ] Write ViewModel tests for `HomeViewModel`, `AllAppsViewModel`, and `WidgetsViewModel`
- [ ] Add Compose UI tests for `HomeScreen`, `AllAppsScreen`, and `WidgetsScreen`
- [ ] Add Detekt and KtLint to the build with a baseline configuration
- [ ] Add a CI pipeline that runs lint, unit tests, and Compose tests on every PR
- [ ] Set a minimum unit test coverage threshold of 60% for domain and data layers
- [ ] Add a crash reporting tool (Firebase Crashlytics or Sentry)
- [ ] Replace all empty catch blocks with proper logging (Timber or similar)

## Phase 7: Module Structure

- [ ] Reorganize the project into Gradle modules:
  - [ ] `:app` — Application entry point, DI modules, MainActivity
  - [ ] `:core:common` — Shared utilities, extensions, constants
  - [ ] `:core:data` — DataStore, serialization, base repository classes
  - [ ] `:core:domain` — Repository interfaces, use cases, domain models
  - [ ] `:core:ui` — Base Compose components, theming, design system
  - [ ] `:feature:home` — Center screen, favorites, carousel, dock
  - [ ] `:feature:appdrawer` — All apps, categories, folders, search
  - [ ] `:feature:widgets` — Left screen, widget CRUD, system widgets
  - [ ] `:feature:notifications` — Notification center, media playback
  - [ ] `:feature:wallpaper` — Auto wallpaper, sources, scheduling
  - [ ] `:feature:settings` — Settings UI, backup/restore
  - [ ] `:feature:shortcuts` — App shortcuts, quick actions
  - [ ] `:feature:onboarding` — Setup guide, first run
  - [ ] `:system:notificationlistener` — NotificationListenerService implementation
  - [ ] `:system:accessibility` — AccessibilityService implementation
  - [ ] `:system:widgethost` — AppWidgetHost management
- [ ] Ensure feature modules have no dependencies on each other
- [ ] Ensure all feature modules depend only on `:core:domain` and `:core:ui`
- [ ] Ensure `:core:domain` has zero Android dependencies (pure Kotlin)
- [ ] Apply the `build-logic` convention plugin pattern for shared build configuration

## Phase 8: Cleanup and Hardening

- [ ] Remove all hardcoded package name strings and extract to a centralized `KnownPackages` object
- [ ] Remove all hardcoded URLs and extract to a `RemoteConfig` or build-config field
- [ ] Remove all hardcoded default values (weather, calendar, dock defaults) and extract to a `Defaults` provider
- [ ] Replace all `catch (_: Throwable) {}` blocks with proper error handling and logging
- [ ] Add `@VisibleForTesting` annotations to all testable internal methods
- [ ] Remove the `MyApplicationTheme` backward-compatibility alias
- [ ] Remove all commented-out dependencies from `build.gradle.kts`
- [ ] Remove the `secrets` Gradle plugin if no secrets are actually used
- [ ] Add `@Deprecated` annotations to all methods scheduled for removal
- [ ] Create an `ARCHITECTURE.md` document describing the final module and layer structure
