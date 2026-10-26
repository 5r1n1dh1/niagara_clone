# Module Dependency Graph (Agent H - Phase 9)

## Module List

- `:core:common` — Shared utilities
- `:core:domain` — Pure Kotlin domain interfaces (zero Android dependencies)
- `:core:data` — DataStore/repository implementations
- `:core:ui` — Compose base components / theming
- `:feature:home`
- `:feature:appdrawer`
- `:feature:widgets`
- `:feature:notifications`
- `:feature:wallpaper`
- `:feature:settings`
- `:feature:shortcuts`
- `:feature:onboarding`
- `:system:notificationlistener`
- `:system:accessibility`
- `:system:widgethost`

## Dependency Rules

- `:core:domain` depends only on `:core:common` (pure Kotlin, zero Android imports).
- `:core:data` depends on `:core:common` + `:core:domain`.
- `:core:ui` depends on `:core:common` + `:core:domain`.
- Feature modules (`:feature:*`) depend ONLY on `:core:domain` + `:core:ui` (no cross-feature dependencies).
- System modules (`:system:*`) depend ONLY on `:core:domain`.

## Verification Status

- `build-logic` created with `library`, `feature`, `domain` convention plugins.
- `settings.gradle.kts` updated with module includes and `pluginManagement { includeBuild("build-logic") }`.
- `gradle/libs.versions.toml` updated with `android-library` plugin entry.
- Zero `android.` imports verified in `:core:domain/src/`.
- Cross-feature dependency grep performed; no forbidden references found.
