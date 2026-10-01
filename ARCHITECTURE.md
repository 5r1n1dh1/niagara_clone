# Flux Launcher Architecture

## Overview
Flux Launcher is built using modern Android development best practices, following **Clean Architecture** principles and **MVVM** (Model-View-ViewModel) design patterns.

## Layer Structure

### 1. Domain Layer (`com.example.domain`)
- **Purity:** Contains pure Kotlin business logic, domain models, and repository interfaces (`SettingsRepository`, `AppRepository`, `NotificationRepository`, `WidgetRepository`, `WallpaperRepository`, etc.).
- **Constants:** Centralized `KnownPackages`, `Defaults`, and `RemoteConfig` registries.

### 2. Data Layer (`com.example.data`)
- **Repositories:** Concrete implementations interacting with Android system services, `PackageManager`, `LauncherApps`, and persistence stores.
- **Caching & State:** Coroutines `StateFlow` streams providing reactive unidirectional data flow.

### 3. UI Layer (`com.example.ui`)
- **Composables:** Stateless presentational Jetpack Compose components (`FavoritesSection`, `DockBar`, `WidgetCard`, `AppSearchBar`, etc.).
- **ViewModels:** Decomposed feature ViewModels (`HomeViewModel`, `AllAppsViewModel`, `WidgetsViewModel`, `SettingsViewModel`, `SearchViewModel`, `NotificationCenterViewModel`) managing normalized UI states (`HomeUiState`, `AllAppsUiState`, `WidgetsUiState`, `SettingsUiState`).

### 4. System Layer (`com.example.service`)
- **System Integrations:** `NotificationListenerService` and `AccessibilityService` implementations handling OS-level integrations.
