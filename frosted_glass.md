# Frosted Glass Theme — Conformance, Correctness & Optimization Plan

Acceptance criteria for making **every** panel, sheet, popup, dialog, row and screen honour the globally
selected Frosted Glass theme (translucency, blur, specular highlight), and for making the theme itself
cheaper to render and cheaper to re-configure.

Legend:

- *Verified* — confirmed present in the codebase today. Documented so it is preserved, not re-implemented.
- *Defect* — confirmed broken: the surface ignores the global theme values, or the global values are ignored.
- *Waste* — confirmed redundant work that costs frames/battery with no visual benefit.
- Each checkbox is an independently verifiable acceptance criterion.

Related documents: `batteryNperf.md` (§5 Blur/Backdrop & Rendering Cost overlaps with Phase 5 below),
`refactor.md`, `newsWidget.md`.

---

## 0. Ground Truth — The Current Plumbing

*Verified.* The three user-facing values live as plain `Float` fields on the single `LauncherSettings`
data class (42 fields, `data/AppModel.kt:218-262`):

| Setting | Field | Declared | Pref key | Read | Written |
| --- | --- | --- | --- | --- | --- |
| Translucency | `frostedTranslucency` | `AppModel.kt:245` | `frosted_translucency` | `LauncherRepository.kt:318` | `:363` |
| Blur Radius | `frostedBlurRadius` | `AppModel.kt:246` | `frosted_blur_radius` | `:319` | `:364` |
| Glass Specular Highlight | `frostedGlassEffect` | `AppModel.kt:247` | `frosted_glass_effect` | `:320` | `:365` |
| Theme preset | `themePreset` | `AppModel.kt:229` | `theme_preset` | `:304` | `:349` |

*Verified.* Storage is `SharedPreferences("niagara_launcher_prefs")` (`LauncherRepository.kt:46`) exposed as a
single `StateFlow<LauncherSettings>` (`:61-62`). No DataStore.

*Verified.* The three sliders that write them are `SettingsStudioSheet.kt:996-1000` (translucency, range
`0.10f..0.85f`), `:1009-1013` (blur, range `0f..30f`), `:1022-1026` (specular, range `0f..1f`), all funnelling
into `onUpdateSettings(settings.copy(...))` → `MainActivity.kt:845` → `repository.updateSettings(it)`.

*Verified.* `MainActivity.kt:156` collects the flow and fans the three values out to 11 call sites
(`:474-477`, `:650-653`, `:671-673`, `:683-685`, `:707-709`, `:721-723`, `:739-741`, `:754-756`, `:764-766`,
`:819-821`, `:943-945`) plus the wallpaper backdrop at `:424-432`. Five more composables receive the whole
`settings` object and read the values internally (`:515`, `:571`, `:836`, `:871`, `:885`).

### Audit result

**Conforming (20 surfaces).** Dock app-picker sheet (`FixedDock.kt:187-191`), add-widget sheet
(`LeftWidgetsScreen.kt:1291-1295`), carousel customize sheet (`Carousel4x2Widgets.kt:626-630`), news setup sheet
(`NewsWidgetSetupSheet.kt:50-54`), unified search sheet (`UnifiedSearchSheet.kt:75-79`), Google Discover sheet
(`GoogleDiscoverSheet.kt:95-99`), system widget picker sheet (`SystemWidgetPickerSheet.kt:183-187`),
notification shade outer + quick-settings + per-notification rows (`NotificationCenterSheet.kt:79-88`, `:143-149`,
`:305-311`), reorder-favorites sheet (`ReorderFavoritesSheet.kt:52-57`), app popup drawer
(`AppPopupDrawer.kt:176-180`), digital-wellbeing customize sheet (`DigitalWellbeing4x1Widget.kt:286-290`),
left-screen background + widget cards (`LeftWidgetsScreen.kt:80-86`, `:318-322`), all-apps background
(`RightAllAppsScreen.kt:172-181`), center-home long-press sheet (`CenterHomeScreen.kt:577-591`), carousel cards
(`Carousel4x2Widgets.kt:71-75`).

**Non-conforming (23 findings).** Grouped and actioned in Phase 1 below.

---

## Phase 1 — Conformance: Make Every Surface Honour the Theme

### 1.1 Single source of truth for glass tokens

- [ ] Introduce an immutable value type (e.g. `GlassStyle(translucency: Float, blurDp: Float, specular: Float, enabled: Boolean)`)
      in `ui/theme/` annotated `@Immutable`, constructed once in `MainActivity.kt` from
      `settings.frostedTranslucency / frostedBlurRadius / frostedGlassEffect / themePreset`, and passed to all
      surfaces **instead of** three loose `Float` parameters. `enabled` must be derived from
      `settings.themePreset == LauncherThemePreset.FROSTED_GLASS` — never hardcoded at the call site.
- [ ] Reduce the 11 loose-triplet call sites in `MainActivity.kt` (`:474-477`, `:650-653`, `:671-673`, `:683-685`,
      `:707-709`, `:721-723`, `:739-741`, `:754-756`, `:764-766`, `:819-821`, `:943-945`) and the five
      whole-`settings` sites (`:515`, `:571`, `:836`, `:871`, `:885`) to a single `glass: GlassStyle` parameter.
- [ ] Validate and clamp all three values **once**, at the `GlassStyle` factory, to
      `translucency ∈ 0.05f..0.95f`, `blurDp ∈ 0f..30f`, `specular ∈ 0f..1f`. No downstream consumer may
      re-derive or re-clamp them.
- [ ] Delete the unused `Defaults.FROSTED_BLUR_RADIUS` / `FROSTED_TRANSLUCENCY` / `FROSTED_GLASS_EFFECT`
      constants (`domain/Defaults.kt:13-15`) *or* wire them as the `LauncherSettings` defaults so the literals
      `15f / 0.30f / 0.30f` stop being duplicated at `AppModel.kt:245-247` and `LauncherRepository.kt:318-320`.
- [ ] Give `FrostedGlassPanel` an explicit `enabled: Boolean` parameter (it currently has none) so every panel —
      not just `FrostedGlassBackground` (`FrostedGlassBackground.kt:150`) — has a single documented non-frosted
      fallback path.
- [ ] Add a lint rule (detekt/custom) that rejects `blurRadius =`, `translucency =`, `glassEffect =` arguments
      that are numeric literals or `.dp`/`.value` conversions, forcing every value through `GlassStyle`.

### 1.2 Surfaces that silently drop the theme values

- [ ] `NewsWidget.kt:42-44` — `blurRadius`/`translucency`/`glassEffect` are declared but referenced nowhere in
      the file. Delete the dead parameters, or (preferred) route the standalone card at `:193-198` through
      `FrostedGlassPanel`. Callers `LeftWidgetsScreen.kt:456-458` and `Carousel4x2Widgets.kt:123-125` currently
      pass values that are thrown away.
- [ ] `YouTubeMusicWidget.kt:239-252` — `YouTubeMusicFluxWidget` declares no glass parameters at all; the
      standalone container at `:360-372` is a hardcoded unblurred `Surface(...surfaceVariant.copy(alpha = 0.40f))`.
      Add the parameters and convert the container. Also fix the nested double-scrim at `AppPopupDrawer.kt:1040`,
      which renders this widget without `isEmbeddedInCard = true` inside the drawer's own `FrostedGlassPanel`.
- [ ] `NotificationSummaryWidget.kt:299-312` — filter sheet hardcodes `blurRadius = 15.dp`,
      `translucency = 0.35f`, `glassEffect = 0.30f`. The composable accepts no glass parameters (`:57-65`), so
      `LeftWidgetsScreen.kt:444-449` and `Carousel4x2Widgets.kt:111-116` cannot supply them. Add the parameter
      and delete all three literals.
- [ ] `SettingsStudioSheet.kt:2871-2876` (`IntegrationsSettingsPage`) — hardcodes `15.dp / 0.30f / 0.30f`.
      The page is `private` (`:2843-2847`) and never receives `settings`, which is the root cause. Thread
      `glass` in from the enclosing `SettingsStudioSheet` (`:148-158`).
- [ ] `SettingsStudioSheet.kt:2949-2953` — app-assignment picker sheet, same three literals. Same fix.
- [ ] `LeftWidgetsScreen.kt:90-113` — the call site of `WidgetResizeAndOptionsSheet` passes **none** of
      `blurRadius`/`translucency`/`glassEffect`, although the sheet declares them at `:1547-1558` and uses them at
      `:1567-1571`. Wire all three (or the `glass` token) through. This is the per-widget "⋯ options" sheet on
      every left-screen card.
- [ ] `LauncherWallpaperBackdrop.kt:232-259` — the `isFrosted` branch is correct but its scrim alphas are
      hardcoded hex literals (`0x3B18181B`, `0x4D27272A`, `0x6618181B`, `0.08f`, `0.04f`) that duplicate the
      `FROSTED_GLASS` preset colours (`AppModel.kt:107`) and never respond to `frostedTranslucency` /
      `frostedGlassEffect`. Derive them from `GlassStyle`.

### 1.3 Theme-branch bugs (wrong direction of failure)

- [ ] `SettingsStudioSheet.kt:149` — `isFrostedTheme = true` is hardcoded, so the settings screen is frosted even
      when `FLUX_EMERALD` is selected. Must be `settings.themePreset == LauncherThemePreset.FROSTED_GLASS`
      (via `GlassStyle.enabled`), so the user gets live feedback while dragging the sliders but the screen still
      matches the selected preset.
- [ ] `FixedDock.kt:47` — `isFrostedTheme: Boolean = false` is declared and **never read** in the body. The dock
      therefore renders as a translucent glass pill under *every* preset. Wire it to the opaque/non-frosted
      fallback path, and remove the hardcoded `translucency = if (blurBackground) translucency else 0.15f` at
      `:69` in favour of `GlassStyle`'s validated fallback.
- [ ] `SetupGuideScreen.kt:114-119` — full-screen onboarding is an opaque
      `Surface(color = MaterialTheme.colorScheme.background)` with no blur and no translucency control; under
      `FROSTED_GLASS` that resolves to the same `Color(0x3B18181B)` as `FLUX_EMERALD`, so the preset has no
      effect. Inner surfaces at `:460`, `:571-577`, `:911-915`, `:1085`, `:1120`, `:1226-1228` are likewise
      unblurred hardcoded `surface`/`surfaceVariant`.
- [ ] `SecretLabDialog.kt:28-37` and `DebugMetricsOverlay` at `:129-138` — opaque `Dialog` +
      `Surface(color = colorScheme.surface, tonalElevation = 8.dp)`, no glass parameters in the signature
      (`:19-22`). Convert and wire from `MainActivity.kt:899-906`.
- [ ] `UsageBreakerDialog.kt:55-64` — opaque `Dialog` + `Surface(color = colorScheme.surface)`, no glass
      parameters (`:27-33`). Convert and wire from `MainActivity.kt:951-956`.
- [ ] `AppPopupDrawer.kt:711-746` — `DetailedMessagePreviewPopupDialog` uses a hardcoded
      `Color.Black.copy(alpha = 0.55f)` full-screen scrim (inconsistent with the `0.45f` used by every other
      `ModalBottomSheet` in the app) plus an opaque `Surface(color = colorScheme.surface)`. Add glass parameters,
      align the scrim, and convert the card.

### 1.4 Stock dialogs and opaque chrome inside frosted sheets

- [ ] Convert the stock `AlertDialog`s that currently render with the default opaque
      `surfaceContainerHigh` container: `LeftWidgetsScreen.kt:1385-1422` ("Edit Note Widget"),
      `AppPopupDrawer.kt:507` ("Assign App Category"), `AppPopupDrawer.kt:557` ("Disguise App Name"),
      `SettingsStudioSheet.kt:436` and `:452` (export/import backup), `SettingsStudioSheet.kt:1221`
      (icon-pack import), `SettingsStudioSheet.kt:2676` ("Fix 'Restricted Setting'"). Each must take the glass
      style and render its container as a `FrostedGlassPanel` so a frosted popup never contains an opaque child.
- [ ] `SettingsStudioSheet.kt:161-164` — the settings header bar is an opaque
      `Surface(color = colorScheme.surface, tonalElevation = 2.dp)` sitting directly on the glass backdrop, which
      reads as a visible solid strip. Make it transparent or glass.
- [ ] `SettingsStudioSheet.kt:1261-1264` — segmented control uses
      `if (isSelected) primaryContainer else colorScheme.surface`; the unselected branch is opaque. Derive both
      branches from `GlassStyle.translucency`.
- [ ] Replace the long tail of hardcoded `copy(alpha = …)` sub-surfaces with values derived from
      `GlassStyle.translucency`, so dragging the Translucency slider updates the whole surface tree rather than
      only the panels listed in §0. Priority order:
      - [ ] `SettingsCard` `SettingsStudioSheet.kt:2796-2798` (`0.45f`, used ~20×), `SettingsMenuCard` `:504-506`
            (`0.45f`), banner `:250-252` (`0.5f`)
      - [ ] `CenterHomeScreen.kt:627`, `:683`, `:740`, `:796`, `:852` — these use `settings.frostedTranslucency`
            **directly as a raw alpha** on `surfaceVariant`, so at 0.85 the panel reads as a flat opaque slab.
            Map through the panel's own scrim curve instead.
      - [ ] `RightAllAppsScreen.kt:220` (`0.88f`/`0.55f` search bar), `:339` (`0.45f` category card)
      - [ ] `NotificationCenterSheet.kt:463-466` — `QuickTileItem` (`0.55f` fill, `0.20f` border)
      - [ ] `FixedDock.kt:150` (empty-slot `0.5f`), `:245-246` (search field `0.35f`/`0.5f`)
      - [ ] `AppListItem.kt:110`, `:127`, `:155`; `GoogleDiscoverSheet.kt:156`, `:171`;
            `UnifiedSearchSheet.kt:124`, `:193`, `:251`; `SystemWidgetPickerSheet.kt:354`, `:387-388`;
            `AppPopupDrawer.kt:271`, `:354`, `:1376`; `ReorderFavoritesSheet.kt:133`, `:346`;
            `Carousel4x2Widgets.kt:681`, `:736`; `FolderInlineItem.kt:54`, `:85`;
            `LeftWidgetsScreen.kt:351`, `:373`, `:587`, `:597`, `:607`, `:890`, `:1035`, `:1098`, `:1139`,
            `:1342`, `:1635`
- [ ] `ReorderFavoritesSheet.kt:172` — `surfaceVariant.copy(alpha = settings.frostedTranslucency + 0.15f)`
      computes an out-of-range alpha (up to 1.0) once Phase 1.1 clamping lands. Replace with a clamped,
      panel-derived value.

### 1.5 Dead settings and dead components

- [ ] `AppModel.kt:244` `backgroundBlurStrength` (`bg_blur_strength`) is persisted (`LauncherRepository.kt:317`,
      `:362`) but never read — `MainActivity.kt:430` substitutes `settings.frostedBlurRadius` into the
      `backgroundBlurStrength` slot. Either delete the field and its two pref keys, or restore it as the
      independent wallpaper-blur control it was named for. Same for `AppModel.kt:256` `blurBackgrounds`, which has
      **zero** usages app-wide.
- [ ] `FrostedGlassCard` (`FrostedGlassBackground.kt:123-142`) has zero call sites. Delete it, or make it the
      single preferred panel API so the two entry points stop diverging.
- [ ] Delete the dead presentational stubs so they cannot drift from the theme: `DockBar.kt:13-30` (zero call
      sites), `FavoritesSection.kt:13-36` (zero call sites), `WidgetEditControls.kt:16-53` (zero call sites),
      `ClockHeader.kt:35-287` (zero call sites, no glass params, hardcoded `0.5f`/`0.35f` surfaces at
      `:232-234`/`:259-261`), `CenterHomeScreen.kt:99-228` `UsageDetailsWidgetCard` (zero call sites).
- [ ] `LauncherRepository.exportBackupJson` (`:1194-1222`) and `importBackupJson` (`:1224-1255`) carry
      `themePreset` but **not** the three glass values, so a restore silently loses the user's glass tuning. Add
      all three to both directions.
- [ ] Leave `SleepOverlay.kt:33-38` (`Color.Black` screen-off overlay) and `AppIconRenderer.kt` gradients
      alone — verified intentional, not chrome surfaces.

---

## Phase 2 — Correctness of the Effect Itself

- [ ] **Backdrop blur that actually blurs the backdrop.** `FrostedGlassBackground.kt:50-65` sets
      `renderEffect = RenderEffect.createBlurEffect(...)` on a layer that contains **only** its own scrim gradient,
      specular gradient and border stroke. A `RenderEffect` applies to the layer's own content, not to what is
      behind it — so this blurs three already-smooth gradients (visually a no-op) and leaves the wallpaper
      sharp. Either remove the per-panel `renderEffect` entirely and let the wallpaper layer beneath supply the
      blur (the mechanism `MainActivity.kt:429` already uses), or move the blur to the backdrop layer. Whichever
      is chosen, add a screenshot test that asserts the wallpaper pixels under a panel are actually blurred.
- [ ] **Pre-API 31 devices get no blur at all.** `minSdk = 24` (`app/build.gradle.kts`), but
      `FrostedGlassBackground.kt:52`, `LauncherWallpaperBackdrop.kt:111` and `MainActivity.kt:405` all gate the
      blur on `Build.VERSION_CODES.S`. On API 24–30 the Frosted Glass preset degrades to a flat translucent
      scrim with zero blur. Either lower the floor by always running the pre-blurred-bitmap path on API 24–30,
      or surface an explicit "blur unavailable on this Android version" state in the settings sliders.
- [ ] **Remove the double blur.** `LauncherWallpaperBackdrop.kt:129` wraps `Modifier.blur(effectiveBlur)`
      around a `Box` whose `Image` already draws `softwareBlurredBitmap` (`:131`, produced at `:89-109`). On
      API 31+ the wallpaper is blurred twice. Pick one path per API level: GPU `Modifier.blur` for the live value,
      pre-blurred bitmap only as the cached fallback.
- [ ] `FrostedGlassBackground.kt:63-64` sets both `clip = true` and `shape = shape` on the same `graphicsLayer`.
      Setting `shape` already implies clipping, so `clip = true` allocates a second redundant offscreen pass.
      Remove `clip = true`.
- [ ] `FrostedGlassBackground.kt:51` clamps the blur to `0f..50f` **in pixels after `toPx()`**, while the slider
      (`SettingsStudioSheet.kt:1012`) allows up to `30f` in dp. On a 3× display 30 dp = 90 px, so the slider
      silently saturates past ~16.7 dp. Clamp in dp in `GlassStyle` and document the pixel ceiling.
- [ ] Fix the unit ambiguity: `frostedBlurRadius` is labelled `" px"` (`SettingsStudioSheet.kt:1007`) but consumed
      as dp at `FrostedGlassBackground.kt:158`, `CenterHomeScreen.kt:587`, `ReorderFavoritesSheet.kt:54` and
      `LauncherWallpaperBackdrop.kt:85` (`.value.toInt()`). Rename to `frostedBlurDp` and fix the label.

---

## Phase 3 — Performance: Rendering Cost

- [ ] **Stop the CPU blur storm.** `LauncherWallpaperBackdrop.kt:89` keys a `LaunchedEffect` on `blurRadiusPx`,
      which is derived at `:85` from `effectiveBlur = baseBlur + blurRadius`. `blurRadius` comes from
      `MainActivity.kt:429` as `wallpaperBlurRadius + panelBackgroundBlurRadius` — `wallpaperBlurRadius` tracks
      `pagerState.currentPageOffsetFraction` continuously during every horizontal swipe (`:359-365`) and
      `panelBackgroundBlurRadius` is a 250 ms `animateDpAsState` tween (`:399-403`). Each tick therefore cancels
      and restarts a `Dispatchers.Default` job that runs `createScaledBitmap` + `stackBlurBitmap`.
      Quantise the blur value (e.g. round to the nearest 2 px) or debounce/defer it, so a swipe triggers a
      handful of blurs rather than dozens per second. Target: **zero** background blur jobs initiated during a
      frame scroll once the quantised value settles.
- [ ] **Recycle the blur bitmaps.** `fastBlurBitmap` (`LauncherWallpaperBackdrop.kt:267-279`) allocates three
      bitmaps per call — `createScaledBitmap` at `:272`, `createBitmap` at `:273`, and the caller's own
      `createScaledBitmap` at `:98` — and none of the intermediates are ever recycled. `stackBlurInPlace`
      (`:282`) additionally allocates six `IntArray`s the size of the buffer on every invocation. Recycle the
      intermediates (`scaledBmp`, `input`) and reuse the pixel/scratch arrays across invocations.
- [ ] **Cache the blurred wallpaper across configuration changes.** Memoise the blurred result keyed by
      `(sourceBitmap identity, quantised radius)` so it survives recomposition, panel open/close and pager
      settles instead of being recomputed. Also size the source decode to the real viewport instead of the
      hardcoded `drawable.toBitmap(720, 1280, ARGB_8888)` at `LauncherWallpaperBackdrop.kt:68`.
- [ ] **Eliminate per-row blur layers.** `NotificationCenterSheet.kt:305-311` puts a `FrostedGlassPanel` on
      every notification row inside a `LazyColumn`, so N notifications = N offscreen layers, each allocating and
      releasing a blurred buffer as the list scrolls. Once Phase 2 removes the per-panel `renderEffect` this
      collapses to a cheap scrim; additionally verify with `dumpsys gfxinfo` that notification-shade scrolling
      produces no per-row bitmap allocation.
- [ ] **Cap fullscreen overdraw.** `RightAllAppsScreen.kt:172`, `SettingsStudioSheet.kt:148` and
      `LeftWidgetsScreen.kt:80` each mount a `FrostedGlassBackground` that draws three fullscreen rects
      (base scrim `:70-75`, specular `:77-83`, border `:85-90`). Verify via overdraw visualisation that no
      fullscreen scrim draws beneath opaque content, and that the specular and border passes are skipped when
      `specular == 0f` / `borderWidth == 0.dp` (the border guard at `:102` exists; the specular guard at `:97`
      does, but neither is hoisted out of composition).
- [ ] Hoist the three `Brush` allocations in `FrostedGlassBackground.kt:70-90` out of the per-instance
      `drawWithCache` block into a memoised factory keyed on the validated `GlassStyle`, so repeated panels
      share one scrim and one specular shader rather than rebuilding the colour lists per instance.
- [ ] Annotate `LauncherSettings` (`AppModel.kt:218`) and the new `GlassStyle` `@Immutable`. `LauncherSettings`
      currently carries **no** stability annotation and holds 42 fields, so Compose cannot skip any of its
      consumers — every settings change invalidates the whole tree.
- [ ] Confirm the whole theme is a no-op on the non-frosted path: with `GlassStyle.enabled == false`, no
      `RenderEffect`, no `clip` layer, no specular pass, and no `Modifier.blur` may be installed anywhere.
- [ ] Generate a Baseline Profile covering launcher cold start, home-screen idle, app-drawer scroll and
      notification-shade open (AGP 9.1.1 / Kotlin 2.2.10 toolchain), and verify release R8 is already keeping the
      theme fast paths. Coordinate with `batteryNperf.md` §1 and §5 rather than duplicating that work.

---

## Phase 4 — Efficiency: Settings Plumbing

- [ ] **Stop rewriting 58 keys per slider tick.** `LauncherRepository.updateSettings`
      (`LauncherRepository.kt:336-397`) issues 58 `put` calls on every invocation, and each of the three
      sliders (`SettingsStudioSheet.kt:998`, `:1011`, `:1024`) fires `onValueChange` continuously with no `steps`
      and no debounce — roughly 60 full-settings writes per second while dragging. Diff the incoming settings
      against the current value and write only changed keys.
- [ ] Debounce or throttle the slider commits (e.g. commit on `onValueChangeFinished`, or coalesce writes on a
      short trailing-edge debounce) while keeping the in-memory `StateFlow` emission immediate so the preview
      still tracks the finger.
- [ ] Narrow recomposition scope. Today one slider tick changes three floats on a 42-field object, recomposing
      all 16 consumer sites in `MainActivity.kt`. With `GlassStyle` + `@Immutable`, confirm that changing
      `frostedTranslucency` skips composables that only read `dockApps` or `wallpaperStyle` — verify with the
      Compose compiler's recomposition counters or Layout Inspector.
- [ ] Consolidate the 8+ independent `LauncherRepository` instances (`SettingsViewModel.kt:16`,
      `HomeViewModel.kt`, `SearchViewModel`, `WidgetsViewModel`, `AllAppsViewModel`,
      `NotificationCenterViewModel`, `SettingsActivity.kt`, `LauncherViewModel.kt:27`) onto a single
      process-scoped repository, so the N `MutableStateFlow<LauncherSettings>` instances over the same
      SharedPreferences file cannot diverge. Coordinate with `refactor.md` Phase 3.
- [ ] `LauncherSettings` is recreated wholesale by `settings.copy(...)` on every write; verify that
      `updateSettings` skips the emission entirely when the new value equals the current one.

---

## Phase 5 — Verification

- [ ] Add unit tests for `GlassStyle`: clamping at `0.05/0.95`, `0/30`, `0/1`; that `enabled` is `false` for
      `FLUX_EMERALD`; and that no accessor can return a value outside the validated range.
- [ ] Add a grep-based CI check asserting zero occurrences of `blurRadius = 15.dp`,
      `translucency = 0.3`, `glassEffect = 0.3`, `isFrostedTheme = true` outside `FrostedGlassBackground.kt`
      and the `GlassStyle` factory.
- [ ] Add Roborazzi screenshot tests (the plugin and dependencies are already wired in `app/build.gradle.kts`)
      covering: frosted home screen, each of the 9 sheets, the app popup drawer, the notification shade, the
      settings screen, and the secret-lab / usage-breaker dialogs — each asserted at min and max values of all
      three sliders, so a regression that hardcodes any glass value fails the suite.
- [ ] Add a backdrop-blur assertion (Phase 2) proving wallpaper pixels under a panel differ from the unblurred
      wallpaper, on both an API 31+ and an API <31 emulator.
- [ ] Manual matrix run on API 24/29 and API 31/34, both presets, all three sliders at both extremes, verifying
      every surface in §0 conforms and none of the Phase 1 defects reappear.
- [ ] Measure before/after with `adb shell dumpsys gfxinfo com.aistudio.fluxlauncher.vknwre` and Perfetto:
      - [ ] jank during a full left↔right pager swipe (target: no new frames over 16.7 ms attributable to blur)
      - [ ] notification-shade scroll with 15 notifications (target: no per-row bitmap allocation)
      - [ ] settings-slider drag (target: no `SharedPreferences` write storm — verify with a `strace`-backed
            trace or by counting `apply()` calls)
      - [ ] peak PSS and GC pause count over a 5-minute idle home session
- [ ] Confirm no `TODO`/dead glass code remains by re-running the §0 audit and diffing the conforming list
      against this document.

---

## Out of Scope

- Redesigning the visual language of the frosted glass aesthetic (gradient colours, corner radii, border styles)
  beyond removing hardcoded values that bypass the theme.
- Migrating `SharedPreferences` to DataStore (`refactor.md`); Phase 4 reduces write amplification without it.
- Live wallpaper / dynamic colour integration beyond `LauncherWallpaperBackdrop`'s existing blur path.
- `SleepOverlay` and `AppIconRenderer` theming (verified intentionally theme-independent).
