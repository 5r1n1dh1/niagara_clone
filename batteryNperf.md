# Battery & Performance Optimization Plan

Acceptance criteria for optimizing the launcher for best performance and energy efficiency.
Each item is an implementation task to complete. Items are grouped by area.

Legend: items noted as *already present* describe behavior that exists in the codebase today — they are
documented so they are not re-implemented, and only need to be preserved/verified during the other work.

---

## 1. Measurement & Tooling (baseline before optimizing)

- [ ] Add StrictMode (`diskRead`/`diskWrite`/`network`/`detectLeakedClosableObjects`) enabled in debug builds so main-thread violations surface during development.
- [ ] Integrate LeakCanary in debug builds to confirm no Activity/Context/Service leaks (static state in `FluxNotificationListenerService` is a known suspect).
- [ ] Integrate JankStats (or `FrameMetricsAggregator`) to quantify dropped frames on home-screen load, app-drawer scroll, and search typing.
- [ ] Add AndroidX Macrobenchmark module covering cold start and a scroll benchmark for the app list, with a Baseline Profile generated from it.
- [ ] Add `androidx.tracing` `Trace.beginSection`/`endSection` markers around app loading, icon rendering, wallpaper blur, and widget host callbacks so Perfetto traces are attributable.
- [ ] Record a "before" performance report: cold-start time to first frame, peak PSS, frame drop %, and battery drain over a fixed home-screen idle session.

## 2. Cold Start & Launch Path

- [ ] Adopt AndroidX App Startup (or explicit initialization ordering) so heavyweight initializers are deferred and ordered deterministically.
- [ ] Migrate to the Android 12+ `SplashScreen` API with the launcher theme so the system splash hands off directly to first frame instead of a blank window.
- [ ] Ensure all non-essential initialization (calendar sync, wallpaper pre-fetch, weather, widget integrations) runs after first frame via idle/deferred dispatch rather than in `Application`/`onCreate`.
- [ ] Guarantee the home screen renders from cached app data before any disk or network work completes; verify no blocking `PackageManager` query or DataStore read on the main thread during start.
- [ ] Remove any `System.gc()` calls from hot/startup paths (currently in `LauncherPerformanceManager.performAggressiveMemoryCleanup`) and replace with cache eviction only.
- [ ] Make the startup path measurable and deterministic: first frame must not wait on the `LauncherPerformanceManager` 300 ms idle delay.

## 3. Main-Thread & I/O Hygiene

- [ ] Verify zero disk, network, or heavy bitmap work runs on the main thread; enforce via StrictMode in CI/debug.
- [ ] All `PackageManager` queries, DataStore reads/writes, and bitmap decodes run on `Dispatchers.IO` or a dedicated dispatcher.
- [ ] Database/DataStore writes are batched and debounced rather than emitted per keystroke or per broadcast.
- [ ] Replace empty `catch (_: Exception)` blocks in background loaders with logged, counted error handling so silent failures are observable in testing.
- [ ] Ensure no composable performs synchronous I/O during composition (the news RSS fetch in `LeftWidgetsScreen` composition is a known violation — see §7).

## 4. Icon & Bitmap Memory

- [ ] Keep icon caches bounded and keyed correctly; verify the `LruCache` in `AppIconRenderer` and `LauncherPerformanceManager` evict on `onTrimMemory`.
- [ ] Consolidate to a single icon cache with one eviction policy instead of two independent caches, so memory accounting is predictable.
- [ ] Downsample all wallpaper/bitmap decodes using `BitmapFactory.Options.inSampleSize` sized to the destination view instead of decoding at full resolution.
- [ ] Stop hard-coding `drawable.toBitmap(720, 1280, ARGB_8888)` in `LauncherViewModel.loadSystemWallpaper`; scale to the actual display/viewport size and prefer `RGB_565` for opaque wallpapers where quality allows.
- [ ] Reuse `BitmapFactory.Options.inPreferredConfig` and `inBitmap` recycling where decoding repeated wallpaper thumbnails.
- [ ] Ensure `clearIconBitmapCache()` is called on memory pressure and app stop, and that no other path holds strong references to evicted bitmaps.
- [ ] Verify `onTrimMemory` at `TRIM_MEMORY_BACKGROUND` releases every bitmap cache (icons, wallpaper backdrop, notification thumbnails) and does not re-cache until resumed.

## 5. Blur, Backdrop & Rendering Cost

- [ ] Replace the software blur in `LauncherWallpaperBackdrop` (manual `Bitmap.createBitmap` + pixel loop) with hardware `RenderEffect.createBlurEffect` on API 31+, with a cached, pre-blurred fallback for older APIs.
- [ ] Pre-blur the wallpaper once and cache it; do not re-blur per recomposition or per frame.
- [ ] Downscale the wallpaper to a small fraction of screen size before blurring (blur radius on a downsampled bitmap), then scale up — verify GPU path is used, not CPU software rendering.
- [ ] Confirm `FrostedGlassBackground`'s existing `RenderEffect` usage is applied only to visible layers and is removed/hidden off-screen.
- [ ] Avoid full-screen overdraw: verify backdrop layers are clipped to their visible bounds and do not draw behind opaque content.
- [ ] Reuse allocation-free buffers where bitmaps must be created (e.g. `SystemWidgetPickerSheet`, `AppPopupDrawer`, `FluxNotificationListenerService` artwork bitmaps) instead of allocating a new `ARGB_8888` bitmap per notification/artwork update.

## 6. Polling, Timers & Background Work

- [ ] Replace `while (true) { delay(1000) }` clock loops (`ClockHeader`, `CenterHomeScreen`, `Carousel4x2Widgets`, `LeftWidgetsScreen`) with a single shared ticker flow that only ticks when the displayed value changes (per-second only when seconds are visible; otherwise per-minute), so inactive clocks cost nothing.
- [ ] Ensure all polling loops are suspended when the launcher is not visible (onStop/onPause) and resumed on resume; verify no loop keeps running in the background.
- [ ] Replace the media polling loop in `FluxNotificationListenerService` with `NotificationListenerService.onNotificationPosted`/`onNotificationRemoved` event callbacks; keep at most a low-frequency fallback poll only while media is active.
- [ ] Add explicit backoff and jitter to any remaining polling so periodic wakeups do not align and hammer the CPU.
- [ ] Document and enforce a maximum background wake rate: no polling more frequent than once per second while foreground, and zero polling while backgrounded.
- [ ] Remove or gate the 1-second news/rss/media refresh; fetch only on explicit user refresh or a long (minutes-scale) interval.
- [ ] Ensure no coroutine `Job` outlives its owning lifecycle (collect all polling loops in `viewModelScope`/`lifecycleScope` and cancel on stop).

## 7. Network & Data Efficiency

- [ ] Move the Google News RSS fetch out of composition in `LeftWidgetsScreen` into a repository/ViewModel with a cached result; render from cache and refresh in the background.
- [ ] Add an HTTP cache or in-memory/disk response cache for RSS and any other remote fetches so unchanged content is not re-downloaded.
- [ ] Reuse a single `HttpURLConnection`/client with connection pooling and reasonable timeouts; close streams in `finally`/`use`.
- [ ] Gate all network activity (RSS, weather, wallpaper download) on network availability and user-visible state; never fetch while the launcher is backgrounded.
- [ ] Cache `AutoWallpaperManager` downloads to disk and avoid re-downloading the same wallpaper URL; apply the same downsampling rules as §4.
- [ ] Apply `BitmapFactory` downsampling to all remote/downloaded images before decode (no full-resolution decode for a thumbnail).

## 8. Broadcasts, Search & Event Handling

- [ ] Debounce search-query input and re-filter the app list from a debounced flow rather than re-running the filter on every keystroke.
- [ ] Debounce/throttle package-change broadcasts (`PACKAGE_ADDED/REMOVED/CHANGED`) so a bulk install/update triggers one reload, not one reload per package.
- [ ] Do full app-list reloads off the main thread and diff results so the UI updates incrementally instead of rebuilding the whole list.
- [ ] Register/unregister receivers and listeners strictly with lifecycle (no leaked static receivers); verify notification listener static state does not retain a destroyed service.
- [ ] Coalesce duplicate event emissions (settings writes, icon updates) within a frame/time window.

## 9. Widgets, Notifications & Services

- [ ] Keep `AppWidgetHost.startListening/stopListening` strictly lifecycle-tied (already present) and verify no widget updates are processed while stopped.
- [ ] Avoid recomposing widget/media/notification layers when their underlying data has not changed; key composables and memoize derived state.
- [ ] Ensure notification artwork bitmaps are downscaled to display size before decode and released when the notification is dismissed.
- [ ] Bound all static/companion caches in `FluxNotificationListenerService` and clear them on service destroy to prevent leak-driven GC churn.
- [ ] Keep `LifecycleEventEffect(ON_RESUME)` refresh in the Digital Wellbeing widget (already present) and avoid refreshing it more often than needed.

## 10. Scroll & Compose Rendering

- [ ] Provide stable `key` and `contentType` for every lazy list (already used in places — extend to all lists) so items are reused instead of recreated.
- [ ] Ensure list items are skippable: no unstable lambdas/`List` parameters that defeat recomposition skipping; use immutable state.
- [ ] Avoid reading state higher in the tree than needed; keep scroll-position and per-item state local to the item.
- [ ] Verify no heavy work (bitmap decode, blur, string formatting of large lists) runs in composition or `remember` without keys.
- [ ] Keep the app drawer/home scroll at a steady frame rate under JankStats with no frame >16 ms attributed to layout/bitmap work.

## 11. Battery Behavior & Power Policy

- [ ] Wire up the existing `batteryOptimizationMode` setting (persisted and exposed in `SettingsStudioSheet`, currently with no behavioral effect) to a real policy: when enabled, reduce/disable polling, background refresh, and non-essential syncs.
- [ ] Respect Doze/App Standby: no `while(true)` background loops while the device is idle; defer non-essential work to user-visible windows.
- [ ] Ensure zero WakeLocks are acquired (currently none — keep it that way) and document that no background work holds the CPU awake.
- [ ] Pause all non-essential work on `onStop`, resume on `onStart`; verify with a background current measurement that an idle home screen causes negligible drain.
- [ ] Gate weather/calendar/wallpaper refresh behind network + charger/foreground conditions where appropriate.
- [ ] Prefer `Dispatchers.Default`/`IO` over spawning new threads per task; reuse a bounded dispatcher for recurring work.
- [ ] Verify screen-off and background sessions do not wake the CPU for tickers that have no visible consumer.

## 12. Release Build & Verification

- [ ] Confirm release build is fully optimized (`isMinifyEnabled`, `isShrinkResources`, R8 full mode) with no debug-only tooling included.
- [ ] Ship a Baseline Profile generated by the Macrobenchmark module and verify measurable cold-start improvement.
- [ ] Add a release smoke test for cold start, app-drawer scroll, search, and widget host to catch regressions.
- [ ] Add a CI check (or debug assertion) that fails on main-thread I/O, new unbounded caches, or new `while(true)` polling without lifecycle cancellation.
- [ ] Re-run the "after" performance report from §1 and require: no cold-start regression, no increase in peak PSS, no frame-drop regression, and reduced idle drain versus the baseline.
