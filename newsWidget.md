# News Widget — Currents API Implementation Plan

Acceptance criteria for replacing the existing Google News RSS placeholder with a real,
user-keyed news widget backed by [Currents API](https://currentsapi.services).

Each item is an implementation task. Items are grouped by area and ordered by dependency.

Legend: items noted as *already present* describe behavior that exists in the codebase today —
they are documented so they are not re-implemented, and only need to be preserved/verified.

---

## 1. Scope & Goals

Build a `NEWS_FEED` home-screen widget that:

- Prompts for a Currents API key the first time the widget is shown, before any network call.
- Runs a guided setup flow to capture the user's news preferences (language, region, categories,
  keywords, image preference, refresh cadence).
- Renders real headlines with publisher, category, thumbnail, and relative timestamp.
- Degrades gracefully on every failure mode: missing key, offline, invalid key, quota exhausted,
  empty result set.
- Respects the free plan's strict request budget so the widget does not burn the user's daily quota.

### Explicit non-goals

- No Retrofit / OkHttp / Mosi introduction. The app has **no** HTTP client dependency; the house
  pattern is `HttpURLConnection` + `org.json` (see §3.2).
- No shipping a key in `BuildConfig` / `.env`. The user supplies their own key at runtime by design.
- No background `Job`/`Service` that fetches while the launcher is off-screen.

### Out of scope but safe to add later

- `WorkManager` periodic refresh (would need a new dependency).
- `EncryptedSharedPreferences` (would need `androidx.security-crypto`).

---

## 2. Currents API — Research Summary

Base URL: `https://api.currentsapi.services/v1`
Auth: `Authorization: Bearer <key>` header (bare key also accepted). Legacy `?apiKey=` query param
also works but **must not be used** — it leaks the key into logs and proxy caches. Use the header.

Source of truth: `https://currentsapi.services/json/swagger.json` (v1.0.0).

### 2.1 Endpoints

| Endpoint | Purpose | Params we will use |
|---|---|---|
| `GET /latest-news` | Most recent articles | `language`, `category`, `country`, `page_number`, `page_size`, `domain`, `domain_not`, `author` |
| `GET /search` | Keyword/boolean + date range | all of the above **plus** `keywords`, `query`, `start_date`, `end_date`, `limit`, `has_image`, `has_description` |
| `GET /available/categories` | Category codes | — |
| `GET /available/languages` | Language name→code map | — |
| `GET /available/regions` | Region name→code map | — |

### 2.2 Endpoint selection rule (critical)

`/latest-news` **rejects** `keywords`, `query`, `start_date`, `end_date`, and `limit` with **HTTP 400**.

Therefore the client must pick the endpoint from the active preferences:

```
if (keywords.isBlank() && query.isBlank() && !useDateWindow) -> /latest-news
else                                                       -> /search
```

Never build one shared param map and send it to both. Maintain two builders and unit-test that
neither leaks a disallowed param.

### 2.3 Response shape (HTTP 200)

```json
{
  "status": "ok",
  "news": [
    {
      "id": "ac4062d1-5fbb-4784-bf1d-6b50f5f643a0",
      "title": "Investors are taking a gamble on Bitcoin",
      "description": "Cryptocurrency markets rallied again this week...",
      "url": "https://example.com/news/bitcoin-markets",
      "author": "example.com",
      "image": "https://example.com/img/bitcoin.jpg",
      "language": "en",
      "category": ["finance"],
      "source_category": ["finance"],
      "published": "2026-09-07 14:22:08 +0000"
    }
  ],
  "page": 1
}
```

`/search` may additionally return `query_matched`, `query_scope`, `reason`, `page_has_results`.

### 2.4 Field gotchas — all four will bite us

1. **`published` is NOT ISO-8601.** Format is `YYYY-MM-DD HH:MM:SS +0000` — space separator, no `T`,
   and a `+0000` offset with **no colon**. `java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME`
   will throw. Use `SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)` with `isLenient = false`,
   or normalize to `yyyy-MM-dd'T'HH:mm:ssXXX` and parse with `Instant`. Do **not** use `Instant.parse`.
2. **`image` can be the literal string `"None"`.** Not `null`, not `""`. `AsyncImage` will log a
   failure and try to resolve it as a URL. Filter with `image.takeIf { it.startsWith("http") }`.
3. **`description` is truncated to 300 characters on the free plan.** Do not assume full text.
4. **`author` frequently contains a bare domain** (e.g. `"example.com"`), not a person's name.
   Prefer deriving the publisher name from `url`'s host and fall back to `author` only if the host
   cannot be stripped of `www.`.

Also: `category` and `source_category` are arrays; an article can carry several. The widget shows one
badge — take `category.firstOrNull()` and fall back to `source_category.firstOrNull()`.

### 2.5 Free plan limits (the budget we design against)

| Constraint | Free | Builder | Professional | Scale |
|---|---|---|---|---|
| `page_size` max | **20** | 50 | 100 | 200 |
| Date range span | **7 days** | 31 days | 90 days | 180 days |
| Oldest retrievable article | **30 days** | ~6 months | 1 year | 1 year |
| `page_number` range | 1–180 | 1–180 | 1–180 | 1–180 |
| Description length | 300 chars | full | full | full |

Additional offset guardrail: `(page_number - 1) * page_size` must not exceed 5000 rows.

The daily request quota is **not** fixed in the docs. It is reported at runtime via response headers:

```
X-RateLimit-Limit      // total daily requests allowed for the key
X-RateLimit-Remaining  // requests left today (resets 00:00 UTC)
X-RateLimit-Reset      // seconds until 00:00 UTC reset
Retry-After            // seconds until reset, or until burst window clears (on 429)
```

**Design consequence:** do not hardcode a request budget. Read `X-RateLimit-Remaining` from every
response, cache it, and gate all fetching on it. If the header is absent, fall back to a conservative
assumed budget of 100/day until the first successful response reveals the real number.

### 2.6 Error handling matrix

| Code | Meaning | Widget behavior |
|---|---|---|
| 400 | Bad params: malformed date, span too large, unknown category, search-only param on `/latest-news`, malformed boolean query | Surface the `details.message`; treat as a **settings** error → deep-link into the setup sheet on the offending field |
| 401 | Missing/invalid key | Clear the stored key, render "key rejected" state with a Re-enter CTA |
| 429 | Quota or burst limit exceeded | Render "quota exhausted", show `Retry-After` countdown, **stop fetching** until it elapses. Never auto-retry. |
| 5xx / network | Transient | Silent fallback to cache; expose a manual retry affordance |

Error body:
```json
{ "status": "400", "msg": "...", "details": { "message": "...", "errors": { "field": "reason" } },
  "upgrade_url": "..." }
```

`upgrade_url` is present on 429 for free keys — surface it as an optional "Upgrade" link so the user
understands the widget is limited, not broken.

### 2.7 Quota discipline (the single most important design constraint)

The free tier is request-per-day, and a launcher home screen is re-composed constantly. Naïve
`LaunchedEffect(Unit)` fetching is what the current placeholder does and it will exhaust the quota
in minutes. Required guards:

- **Never fetch in composition.** Fetch in a repository against an explicit `refresh()` call.
- **Single-flight.** Concurrent callers join the in-flight job instead of starting new requests.
- **TTL gate.** Skip any refresh requested less than `minRefreshInterval` after the last success.
- **Quota gate.** Skip entirely when `X-RateLimit-Remaining <= reserve` (default reserve `1`).
- **Backoff on failure.** 429 → honor `Retry-After`, then exponential backoff 5m/15m/1h with jitter.
- **Lifecycle.** Refresh on `Lifecycle.Event.ON_RESUME` (house pattern from
  `DigitalWellbeing4x1Widget.kt:186`), never while backgrounded.
- **Default cadence: 60 minutes**, minimum selectable 15 minutes, optional 6 hours / manual-only.

This satisfies `batteryNperf.md` §6 ("fetch only on explicit user refresh or a long interval") and
§7 ("gate all network activity on network availability and user-visible state").

---

## 3. Target Architecture

### 3.1 Layer placement

```
domain/NewsRepository.kt          interface (pure, per ARCHITECTURE.md §1)
domain/RemoteConfig.kt            + CURRENTS_API_BASE, CURRENTS_* constants
data/NewsApiClient.kt             HttpURLConnection + org.json parsing (no DI, plain class)
data/NewsPreferences.kt           SharedPreferences("flux_news_prefs") — key, prefs, quota
data/NewsRepositoryImpl.kt        implements NewsRepository; owns flow + fetch policy
data/AppModel.kt                  + NewsStory, NewsPreferencesState, NewsUiState-ish models
ui/components/NewsWidget.kt       NEW — all size layouts + setup wizard + states
ui/LauncherViewModel.kt           + re-expose newsState; + open/close setup panel
ui/components/Carousel4x2Widgets.kt  replace NEWS_FEED stub (:111-120)
ui/components/LeftWidgetsScreen.kt   replace private NewsWidgetBody (:1433-1629)
MainActivity.kt                   collect flow, thread params, register panel + BackHandler
```

**No ViewModel per widget.** *Already present pattern:* `WidgetsViewModel.kt` exists but is unused;
`LauncherViewModel.kt` is the single god ViewModel (`LauncherViewModel.kt:26-41`) re-exposing
repository `StateFlow`s that `MainActivity.kt:156-173` collects once and passes down as parameters.
Widgets themselves never call `collectAsState`. Follow this.

### 3.2 Networking — reuse the house pattern

The only two network call sites in the app are `AutoWallpaperManager.kt` and `NewsWidgetBody`.
Copy `AutoWallpaperManager.openHttpConnection` (`AutoWallpaperManager.kt:23-38`) exactly:

```kotlin
private fun open(url: String, apiKey: String): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000
        readTimeout = 20000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "FluxLauncher/1.0 (Android)")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $apiKey")   // header only, never query param
    }
```

Then the canonical call shape (`AutoWallpaperManager.kt:229-240`):

```kotlin
val conn = open(url, key)
val body = try {
    val code = conn.responseCode
    if (code !in 200..299) throw NewsApiException(code, readError(conn, code))
    conn.inputStream.bufferedReader().use { it.readText() }
} catch (_: Exception) {
    throw NewsApiException(-1, "network error")     // do NOT swallow: the current code does (`LWS:1513`)
} finally {
    conn.disconnect()                                // always: current code leaks on throw (`LWS:1513`)
}
```

Improvements over the existing news code that are mandatory here:
- `finally { disconnect() }` — current code only disconnects on the happy path.
- 15s/20s timeouts, not 4s.
- Capture response **headers** on success too (needed for `X-RateLimit-*`), which requires reading
  `conn.headerFields` before `disconnect()`.
- Replace every `catch (_: Exception) {}` with a counted/logged failure so errors are observable
  (`batteryNperf.md` §3).

### 3.3 Key storage decision

Use a **dedicated prefs file**, not `LauncherSettings`.

Rationale: `LauncherSettings` has ~40 fields written through one `prefs.edit()…apply()` chain
(`LauncherRepository.kt:336-400`), and every unrelated settings change would rewrite the secret.
`WidgetIntegrationsManager.kt:34-36` already establishes the precedent of a feature-scoped prefs file
(`flux_widget_integrations`). We use `flux_news_prefs` holding:

| Key | Type | Default | Notes |
|---|---|---|---|
| `api_key` | String | `""` | empty ⇒ setup wizard must run |
| `configured` | Boolean | `false` | explicit completion flag, so an aborted wizard doesn't re-prompt forever |
| `language` | String | `"en"` | |
| `country` | String | `""` | empty ⇒ worldwide |
| `categories` | StringSet | `{"general"}` | `getStringSet`/`putStringSet`, matching `hidden_packages` |
| `keywords` | String | `""` | free-text; blank ⇒ latest-news path |
| `query` | String | `""` | advanced boolean; mutually exclusive with keywords |
| `useDateWindow` | Boolean | `false` | search-only |
| `window_hours` | Int | `24` | clamped to the free-plan 7-day span |
| `requireImages` | Boolean | `false` | maps to `has_image` |
| `page_size` | Int | `20` | clamp to 20 on free plan |
| `min_refresh_minutes` | Int | `60` | |
| `last_success_epoch` | Long | `0` | TTL gate |
| `rate_limit_remaining` | Int | `100` | conservative until a header tells us otherwise |
| `rate_limit_reset_epoch` | Long | `0` | |
| `backoff_until_epoch` | Long | `0` | |

The last four are a **cache**, not user settings: they make the widget render instantly on relaunch
with zero requests, which is what protects the daily quota.

### 3.4 API catalogue as static constants

Do **not** call `/available/categories`, `/available/languages`, `/available/regions` from the app.
Three reasons: they may count against quota, they add three failure modes, and the values are stable.
Hardcode them in `RemoteConfig` (`RemoteConfig.kt:6-9` already exists for exactly this purpose):

- Categories (17, ordered by source count): `regional, technology, lifestyle, business, general,
  programming, science, entertainment, world, sports, finance, academia, politics, health, opinion,
  food, game`
- Languages: `en, ar, zh, nl, fi, fr, de, hi, it, ja, ko, msa, pt, ru, es`
- Regions: `AU, CA, CN, FI, DE, IT, KR, TW, TH, VN, ZW, …` (full ISO-3166 alpha-2 set; the docs
  sample a subset)

Unknown category ⇒ 400. Since we control the list, this can only be triggered by a stale persisted
value — so also sanitize on read (drop anything not in the known set).

### 3.5 State model

One immutable state object drives the widget, so all rendering is a pure function of state:

```kotlin
sealed interface NewsFeedState {
    data object NeedsApiKey : NewsFeedState            // first-run CTA
    data class Loading(val cached: List<NewsStory>) : NewsFeedState
    data class Ready(val stories: List<NewsStory>, val lastUpdated: Long, val remainingQuota: Int) : NewsFeedState
    data class Stale(val stories: List<NewsStory>, val lastError: NewsError) : NewsFeedState  // cache + error
    data class Failed(val error: NewsError) : NewsFeedState
}

sealed interface NewsError {
    data object Offline
    data class InvalidKey(val msg: String)
    data class QuotaExceeded(val retryAfterSeconds: Long)
    data class BadRequest(val field: String?, val msg: String)     // 400 → reopen setup
    data class Server(val code: Int)
    data class Unknown(val msg: String)
}
```

`Ready` vs `Stale` distinction is what makes the widget feel solid: after the first success the
widget **always** shows content, even offline or quota-exhausted, with a subtle staleness marker.

---

## 4. Data Layer Tasks

### 4.1 Constants & models

- [ ] Add to `domain/RemoteConfig.kt`: `CURRENTS_API_BASE`, `CURRENTS_LATEST_NEWS_PATH`,
      `CURRENTS_SEARCH_PATH`, `CURRENTS_AVAILABLE_CATEGORIES/LANGUAGES/REGIONS`,
      `CURRENTS_FREE_MAX_PAGE_SIZE = 20`, `CURRENTS_FREE_MAX_WINDOW_HOURS = 168`,
      `CURRENTS_FREE_LOOKBACK_DAYS = 30`, `CURRENTS_MAX_PAGE_NUMBER = 180`,
      `CURRENTS_API_USER_AGENT`, and the three static catalogues from §3.4.
- [ ] Add `NewsCategory(val code, val label)` display mapping in `data/AppModel.kt` next to
      `WidgetType` so the setup UI has friendly labels ("Business", "Programming") rather than raw codes.
- [ ] Add `data class NewsStory(...)` to `data/AppModel.kt`, replacing the `private` copy currently
      trapped at `LeftWidgetsScreen.kt:1397-1404`. Fields: `id, title, description, url, publisher,
      imageUrl: String?, category, publishedAtEpochMillis: Long`. Derive `publisher` and
      `publishedAtEpochMillis` at parse time so the UI never parses.
- [ ] Add `NewsFeedState`, `NewsError`, `NewsPreferencesState` (the §3.3 table as a data class)
      and `NewsApiException(code: Int, error: NewsError)` to `data/AppModel.kt`.
- [ ] Delete `sampleNewsStories` (`LeftWidgetsScreen.kt:1406-1431`). Placeholder fake data must not
      ship — it is indistinguishable from real results and hides every error path.

### 4.2 `data/NewsPreferences.kt`

- [ ] New class wrapping `SharedPreferences("flux_news_prefs", MODE_PRIVATE)`.
- [ ] Implement `load(): NewsPreferencesState` reading every key in §3.3 with a default, sanitizing
      unknown categories against the known set and clamping `page_size` to 20 and `window_hours` to 168.
- [ ] Implement `save(state: NewsPreferencesState)` and `clearSecret()` (clears `api_key` +
      `configured`, preserves prefs).
- [ ] Implement `saveRateLimit(remaining: Int, resetEpochSeconds: Long)`.
- [ ] Do **not** read this from a composable. `DigitalWellbeing4x1Widget.kt:67-69` reads prefs
      directly inside a `remember(context)`; do not copy that — the repository owns it.

### 4.3 `data/NewsApiClient.kt`

- [ ] New class; constructor takes `NewsPreferences` (for the key) and a `Context` (only for user agent).
- [ ] `suspend fun fetchLatest(prefs, page): NewsFetchResult = withContext(Dispatchers.IO)`.
- [ ] `suspend fun fetchSearch(prefs, page): NewsFetchResult = withContext(Dispatchers.IO)`.
- [ ] Two distinct private URL builders. Assert (in debug) that `/latest-news` receives no
      `keywords`/`query`/`start_date`/`end_date`/`limit` param, and `/search` receives no
      `keywords`/`query` when both are blank.
- [ ] Emit `start_date`/`end_date` as RFC 3339 (`2026-09-01T00:00:00Z`) computed from
      `window_hours` and `now`, clamped into the free plan's 30-day lookback. Use a single `SimpleDateFormat`
      in UTC (house pattern: `remember { SimpleDateFormat(...) }`, see `LeftWidgetsScreen.kt:957-960`).
- [ ] `setRequestProperty("Authorization", "Bearer $key")`. Never append the key to the URL.
- [ ] Read `X-RateLimit-Remaining` / `X-RateLimit-Reset` / `Retry-After` from `conn.headerFields`
      **before** `disconnect()`, and return them alongside the payload.
- [ ] Parse with `org.json`: iterate `JSONObject(response).getJSONArray("news")` into `NewsStory` list.
- [ ] Apply the §2.4 field gotchas: `image.takeIf { it.startsWith("http") }`; `SimpleDateFormat`
      for `published` returning `null` epoch (not `0`) on parse failure so the UI can hide the
      timestamp instead of printing "56 years ago"; publisher from `URL(url).host.removePrefix("www.")`.
- [ ] Skip items with a blank `title` or non-`http` `url` — they are unopenable.
- [ ] De-duplicate by `id`, and cap the returned list at `page_size`.
- [ ] Map every non-2xx and thrown exception into `NewsError` per §2.6.
- [ ] Set `instanceFollowRedirects = true` but reject a final URL whose host is not
      `api.currentsapi.services`, so a redirect cannot exfiltrate the `Authorization` header.

### 4.4 `domain/NewsRepository.kt` + `data/NewsRepositoryImpl.kt`

- [ ] Interface: `val newsState: StateFlow<NewsFeedState>`, `val preferences: StateFlow<NewsPreferencesState>`,
      `suspend fun refresh(force: Boolean = false)`, `fun savePreferences(state)`, `fun clearApiKey()`,
      `fun apiKeyOrNull(): String?`, `fun isConfigured(): Boolean`.
- [ ] Implement with a private `CoroutineScope(Dispatchers.Default + SupervisorJob())` mirroring
      `LauncherRepository.kt:47`.
- [ ] Load cached stories from disk at construction and publish them immediately as `Ready`/`Stale`
      so first composition paints content with no request.
- [ ] `refresh(force)` implements the §2.7 policy: single-flight via a `Mutex` + in-flight `Deferred`,
      TTL gate, quota gate, backoff gate, offline gate.
- [ ] Offline gate: use `ConnectivityManager.getActiveNetwork()`/`getNetworkCapabilities()`;
      `ACCESS_NETWORK_STATE` is *already present* (`AndroidManifest.xml:6`) and `INTERNET` at line 5 —
      no manifest change needed for either.
- [ ] Emit `NeedsApiKey` when `api_key` is blank, and **return without any network call**.
- [ ] Invalidate cached stories after `MAX_CACHE_AGE_HOURS = 6`; keep them (as `Stale`) indefinitely
      otherwise — a launcher should never show an empty widget when it once had content.
- [ ] Cache the fetched payload as JSON to a file in `context.cacheDir` (`news_cache.json`) so it
      survives process death. `batteryNperf.md` §7 asks for exactly this.
- [ ] On 401 → call `clearApiKey()` and emit `Failed(InvalidKey)`.
- [ ] On 429 → write `backoff_until_epoch` from `Retry-After` and emit `Failed(QuotaExceeded)`; no retry job.
- [ ] On 400 → emit `Failed(BadRequest)` so the UI can offer "Fix settings".
- [ ] Instantiate once in `LauncherViewModel.kt` next to the existing
      `val widgetIntegrations = WidgetIntegrationsManager(application)` (line 72) and re-expose
      `newsState` / `preferences` alongside the existing block at lines 30-41.

### 4.5 Cache pruning

- [ ] Bound the news cache to a single file (overwrite, never append) and cap it at ~256 KB.
- [ ] Register cache deletion under the existing memory-pressure path alongside
      `clearIconBitmapCache()` (`batteryNperf.md` §4).

---

## 5. Setup / Onboarding Tasks

The widget must ask for the API key on initial load, before any request. There are two entry
points, and both must exist:

- **Inline CTA** — the widget body itself renders a "Connect Currents API" card when unconfigured.
  This is what satisfies "ask on initial load of the widget".
- **Re-entry** — a "Tune feed" affordance on a configured widget reopens the same wizard.

### 5.1 Wizard container — `NewsWidgetSetupSheet.kt` (new file)

Follow the exact house sheet shape (`AddWidgetDialog`, `LeftWidgetsScreen.kt:1247-1342`):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsWidgetSetupSheet(
    initial: NewsPreferencesState,
    blurRadius: Float, translucency: Float, glassEffect: Float,
    onSave: (NewsPreferencesState) -> Unit,
    onDismiss: () -> Unit
)
```

- [ ] `ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.Transparent, scrimColor = Color.Black.copy(alpha = 0.45f))`.
- [ ] Body wrapped in `FrostedGlassPanel(cornerRadius = 28.dp, blurRadius = blurRadius.dp, translucency, glassEffect, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 20.dp))`.
- [ ] Inner `Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp).navigationBarsPadding())`.
- [ ] Header: title `18.sp`/`Bold`, subtitle `12.sp`/`onSurfaceVariant`, trailing
      `IconButton { Icon(Icons.Filled.Close) }`.
- [ ] Body: `LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`.
- [ ] Footer: `Button("Save preferences")` full width, `shape = RoundedCornerShape(12.dp)`, plus a
      `TextButton("Remove API key")` shown only when already configured.
- [ ] `testTag = "news_widget_setup_sheet"`.
- [ ] Internal `var step by remember { mutableIntStateOf(0) }` with `val totalSteps = 6`, mirroring
      `SetupGuideScreen.kt:48-56`.

### 5.2 Step 0 — Welcome / explain the key

- [ ] Body copy: what Currents API is, that the user must create a **free** account at
      `https://currentsapi.services`, copy the key, and that the key is stored only on this device.
- [ ] `TextButton("Open currentsapi.services")` → `Intent(Intent.ACTION_VIEW, Uri.parse(...))`.
      Use the same `LocalContext.current.startActivity` pattern as other external launches
      (`WidgetIntegrationsManager.launch`).
- [ ] Set `configured = false` until the wizard completes — do not persist partial edits on dismiss.

### 5.3 Step 1 — API key entry + validation

- [ ] `OutlinedTextField(value, onValueChange, label = { Text("Currents API key") }, singleLine = true,
      visualTransformation = PasswordVisualTransformation(), ...)`.
      No masked field exists in the app today — this is the first. Add an eye toggle bound to
      `LocalSoftwareKeyboardController`-safe local state.
- [ ] A `TextButton("Paste")` wired to `ClipboardManager` via `LocalClipboardManager.current`.
- [ ] Trim whitespace and strip a pasted `"Bearer "` prefix if the user copied the whole curl line.
      Users paste the full `curl -H "Authorization: Bearer xxx" ...` command constantly.
- [ ] Reject obviously malformed input client-side before any request (empty, < 8 chars, whitespace inside).
- [ ] **Validate with a real, minimal request**: `GET /v1/available/categories` (smallest payload).
      Show a `CircularProgressIndicator` inline while validating.
- [ ] On 200 → show `X-RateLimit-Remaining` as *"Daily quota: N requests left"* and enable Next.
- [ ] On 401 → `error` on the field: *"This key was rejected by Currents."*
- [ ] On 429 → non-blocking warning: *"Quota already exhausted today; the widget will retry at HH:MM UTC."*
      Still allow continuing.
- [ ] Debounce validation: do not fire on every keystroke. Validate on explicit "Validate key"
      button press or via a 600 ms-debounced `LaunchedEffect` on a trimmed key ≥ 8 chars.

### 5.4 Step 2 — Language & region

- [ ] Two `LazyRow`s of `FilterChip`s (§5.5 has the chip recipe). Language default `en`; region
      default "Worldwide" (empty string).
- [ ] Language codes come from the static catalogue; labels are language names.

### 5.5 Step 3 — Categories

- [ ] `FlowRow` (or `LazyRow` of rows) of `FilterChip` over the 17 categories, multi-select.
      `FlowRow` is in `compose.foundation.layout` — verify availability at the BOM version in
      `libs.versions.toml` before relying on it; fall back to a chunked `LazyColumn` otherwise.
- [ ] Chip recipe matching `SystemWidgetPickerSheet.kt:260-282`:
      ```kotlin
      FilterChip(
          selected = code in selected,
          onClick = { toggle(code) },
          label = { Text(label, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor   = MaterialTheme.colorScheme.onPrimaryContainer
          )
      )
      ```
- [ ] Top row of quick presets: "Top stories" (`general, world`), "Tech" (`technology, programming`),
      "Money" (`finance, business`), "Science" (`science, health`), "Life" (`food, lifestyle, entertainment`).
- [ ] Enforce **at least one** selection; disable Next and show a hint when empty. An empty
      `category` param is ambiguous server-side and we should never send it.
- [ ] Show category counts/ordering from the API's source-count ordering so popular categories appear first.

### 5.6 Step 4 — Keywords (optional)

- [ ] `OutlinedTextField` labeled "Keywords (optional)" with helper text explaining websearch syntax:
      quoted phrases, `OR`, and `-exclusion`.
- [ ] A collapsible "Advanced" section exposing the boolean `query` field separately, with an inline
      note that `keywords` and `query` should not both be set.
- [ ] A `Switch` "Only show articles with images" → `has_image`.
- [ ] When keywords/query are non-empty, display an inline notice: *"Using Currents search —
  free plan searches at most the last 7 days."*
- [ ] When the switch is off, `Step 4` collapses into a single optional row so the common path is 3 taps.

### 5.7 Step 5 — Cadence & review

- [ ] `FilterChip` row for refresh cadence: `Manual only`, `Every 6 hours`, `Every 1 hour` (default),
      `Every 15 minutes`. Include a hint: *"The free plan allows a limited number of requests per day."*
- [ ] `FilterChip`/stepper for items per refresh: 5 / 10 / 20 (max 20 on free plan). Label it
      *"max 20 on the free plan"*.
- [ ] Review block showing the resolved request, e.g.
      `GET /v1/latest-news?language=en&category=technology,science&page_size=10`
      — teaching the user what will be fetched, and making a 400 diagnosable before it happens.
- [ ] `Button("Save preferences")` → `onSave(prefs)` then dismiss.

### 5.8 Optional first-run hook in `SetupGuideScreen`

- [ ] Add a `Step 6` to the onboarding `when (currentStep)` at `SetupGuideScreen.kt:186-298`,
      offered only when `WidgetType.NEWS_FEED` was selected in step 3. Reuse `NewsWidgetSetupSheet`
      as an embedded step rather than duplicating the wizard.
- [ ] Bump `val totalSteps = 6` to `7` (`SetupGuideScreen.kt:49`) and extend the step-dots row.
- [ ] Step 6 must be **skippable** — a launcher that blocks on an API key is a launcher people delete.

### 5.9 Re-entry from a configured widget

- [ ] A "Tune" icon button in the widget header (and long-press already opens the customize sheet
      via `combinedClickable`, `Carousel4x2Widgets.kt:72-75`) that opens `NewsWidgetSetupSheet`
      pre-filled with current prefs.
- [ ] Level-2 local state (`var showSetup by remember { mutableStateOf(false) }` inside the widget,
      per `NotificationSummaryWidget.kt:69` + `:298-303`) if the sheet is rendered by the widget;
      or a ViewModel flag if it must be hosted by `MainActivity`. **Prefer the ViewModel flag** so the
      back button and the background blur behave correctly — see §6.4.

---

## 6. Widget UI Tasks

New file `app/src/main/java/com/example/ui/components/NewsWidget.kt`.

### 6.1 Signature

Follow `NotificationSummaryWidget.kt:55-65` (stateless, data pushed down):

```kotlin
@Composable
fun NewsWidget(
    state: NewsFeedState,
    prefs: NewsPreferencesState,
    size: String = "4x2",                       // "4x1" | "4x2" | "4x3" | "4x4"
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    isEmbeddedInCard: Boolean = false,          // YouTubeMusicWidget.kt:248 convention
    onOpenArticle: (String) -> Unit,
    onOpenSetup: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
)
```

### 6.2 Container

- [ ] When `isEmbeddedInCard`, render content only (the host supplies `FrostedGlassPanel`).
- [ ] Otherwise draw a standalone card mirroring `YouTubeMusicWidget.kt:360-386`:
      `Surface(RoundedCornerShape(24.dp), color = surfaceVariant.copy(alpha = 0.40f), border = BorderStroke(1.dp, Brush.verticalGradient(listOf(primary.copy(alpha = 0.28f), outlineVariant.copy(alpha = 0.12f)))), tonalElevation = 2.dp)`.
- [ ] `testTag = "news_widget_$size"`; sub-elements tagged `news_widget_header`, `news_widget_row_$index`,
      `news_widget_image_$index`, `news_widget_state`.

### 6.3 Size model

Sizes are **plain strings**, not an enum — `"4x1".."4x4"`, duplicated in four hardcoded lists today
(`WidgetEditControls.kt:29`, `LeftWidgetsScreen.kt:351`, `LeftWidgetsScreen.kt:1826-1831`, and the
`bodyModifier` when at `LeftWidgetsScreen.kt:409-414`). Do not add a fifth copy; read the existing value.

- [ ] Item budget per size, matching `NotificationSummaryWidget.kt:223-229`:

      | size | height range (from `LeftWidgetsScreen.kt:409-414`) | items | layout |
      |---|---|---|---|
      | `4x1` | 70–100 dp | 1 | thumbnail left, headline right, 2 lines |
      | `4x2` | 150–185 dp | 2–3 | list of rows, 1 line each |
      | `4x3` | 230–275 dp | 4 | list of rows, 2 lines each |
      | `4x4` | 310–365 dp | 6 | list of rows, 2 lines each |

- [ ] Carousel cards are fixed at `168.dp` (`Carousel4x2Widgets.kt:71`) — render the `4x2` layout there
      with a single dominant story + thumbnail, since only ~168 dp is available.

### 6.4 State screens (every one of these must exist and be reachable)

- [ ] `NeedsApiKey` → `FrostedGlassPanel` with `Icons.Filled.Newspaper`-style header, title
      *"Connect your news feed"*, body *"Add a free Currents API key to personalise your headlines."*,
      and a `Button("Set up")` → `onOpenSetup()`. **This is the initial-load API-key prompt.**
- [ ] `Loading` with empty cache → centered `CircularProgressIndicator` + *"Fetching headlines…"*.
- [ ] `Loading` with cache → render the cached stories plus a thin indeterminate `LinearProgressIndicator`
      at the top. Never replace content with a spinner.
- [ ] `Ready` → the story list.
- [ ] `Stale` → the story list with a small warning icon + *"Updated 3h ago"* / *"Offline"*.
- [ ] `Failed(InvalidKey)` → "API key rejected" + `Button("Re-enter key")` → `onOpenSetup()`.
- [ ] `Failed(QuotaExceeded)` → *"Daily quota reached. Resets in 2h 14m"* + `upgrade_url` as an
      optional `TextButton("Upgrade plan")` + the cached list if any.
- [ ] `Failed(BadRequest)` → the API's `details.message`, plus `Button("Fix preferences")`.
- [ ] `Failed(Offline)` / `Server` / `Unknown` → message + `IconButton` retry → `onRefresh()`.
- [ ] `Ready`/`Stale` with `stories.isEmpty()` → explicit empty state (*"No articles matched your
      filters"*), never a blank card.

### 6.5 Row rendering

- [ ] Header strip: category eyebrow (`11.sp`, `FontWeight.Bold`, `letterSpacing = 1.sp`,
      `colorScheme.primary`), then a right-aligned `remainingQuota` chip
      (`primaryContainer` + `onPrimaryContainer`) rendered only when `remainingQuota <= 20`.
- [ ] Headline: `maxLines` per size, `overflow = TextOverflow.Ellipsis`, `colorScheme.onSurface`.
- [ ] Meta line: `publisher` • relative time • category, `11.sp`, `colorScheme.onSurfaceVariant`.
- [ ] Relative time from `publishedAtEpochMillis`: `< 60s` "now", `< 60m` "Nm ago", `< 24h` "Nh ago",
      `< 7d` "Nd ago", else short date. Compute with a `remember { SimpleDateFormat(...) }` formatter
      and never allocate per frame.
- [ ] Thumbnail only when `imageUrl != null`: a `Box` pre-filled with
      `Modifier.background(colorScheme.surfaceVariant)` containing
      `coil.compose.AsyncImage(model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())`.
      Copy `LeftWidgetsScreen.kt:1536-1548` exactly. Note the codebase's convention of calling it
      fully-qualified with **no import**.
- [ ] Add an explicit error/placeholder slot so a failed thumbnail shows the category glyph instead of
      an empty box. The single existing `AsyncImage` has none.
- [ ] Whole row is `Modifier.clickable { onOpenArticle(story.url) }` →
      `Intent(Intent.ACTION_VIEW, Uri.parse(url))`. Add `Intent.FLAG_ACTIVITY_NEW_TASK` when the
      context is not an Activity.
- [ ] Optional swipe/arrow paging: the current body pages via `IconButton` next/prev
      (`LeftWidgetsScreen.kt:1618-1627`). For a list-based layout prefer vertical scroll; keep arrows
      only for the `4x1` variant.

### 6.6 Refresh triggers

- [ ] Manual: pull-to-refresh or a `Refresh` icon button in the header.
- [ ] Automatic: `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onRefresh() }` — the repository's
      TTL/quota gates make this a no-op when it is not due, so this is safe. Copy
      `DigitalWellbeing4x1Widget.kt:186-188`.
- [ ] Never `LaunchedEffect(Unit)` a fetch inside the widget. The current placeholder does exactly
      that at `LeftWidgetsScreen.kt:1439`; that is the violation `batteryNperf.md` §3/§7 flag.
- [ ] Throttle tap-to-refresh to once per 10 s so an impatient user cannot burn the quota.

---

## 7. Wiring & Integration Tasks

### 7.1 Replace the placeholder bodies

- [ ] `Carousel4x2Widgets.kt:111-120` — replace the `"📰 Top News Highlights"` stub with a call to
      `NewsWidget(state = …, prefs = …, size = "4x2", isEmbeddedInCard = true, …)`. Thread the new
      params through `Carousel4x2WidgetCard`'s signature and its caller.
- [ ] `LeftWidgetsScreen.kt:434-436` — replace `NewsWidgetBody(size = widget.heightCategory)` with
      `NewsWidget(state = …, prefs = …, size = widget.heightCategory, isEmbeddedInCard = true, …)`.
- [ ] Delete the old private body `LeftWidgetsScreen.kt:1433-1629`, the private `NewsStory`
      (`:1397-1404`), and `sampleNewsStories` (`:1406-1431`). There must be no `HttpURLConnection`
      left in a composable file.

### 7.2 Thread the state down from the top

- [ ] `LauncherViewModel.kt` — construct the repository next to line 72's `widgetIntegrations`, expose
      `val newsState`, `val preferences`, `fun refreshNews(force: Boolean = false)`,
      `fun saveNewsPreferences(…)`, `fun clearNewsApiKey()`.
- [ ] `MainActivity.kt:156-173` — add
      `val newsState by viewModel.newsState.collectAsState()` and `val newsPrefs by viewModel.newsPreferences.collectAsState()`.
- [ ] Pass both down through `LeftWidgetsScreen(...)` and the carousel composables as plain parameters.

### 7.3 Panel plumbing for the setup sheet

If the sheet is hosted by `MainActivity` (recommended), three registration sites must be updated —
this is the house convention and missing any one is a known class of bug:

- [ ] `LauncherViewModel.kt` — `private val _isNewsSetupOpen = MutableStateFlow(false)` /
      `val isNewsSetupOpen = _isNewsSetupOpen.asStateFlow()` + `openNewsSetup()` / `closeNewsSetup()`,
      declared alongside the other panel flags (lines 168-175).
- [ ] `MainActivity.kt:304-307` — `val isNewsSetupOpen by viewModel.isNewsSetupOpen.collectAsState()`.
- [ ] `MainActivity.kt:665-675` region — `if (isNewsSetupOpen) { NewsWidgetSetupSheet(...) }`,
      passing `blurRadius = settings.frostedBlurRadius, translucency = settings.frostedTranslucency,
      glassEffect = settings.frostedGlassEffect`.
- [ ] `MainActivity.kt:364-367` — add `isNewsSetupOpen` to the `isAnyPanelOpen` aggregate.
- [ ] `MainActivity.kt:370-393` — add an `isNewsSetupOpen -> viewModel.closeNewsSetup()` branch to the
      `BackHandler` `when`, positioned to respect LIFO order.
- [ ] `LauncherViewModel.kt:287-302` — add the flag to `closeAllPanelsAndPopups()`.

### 7.4 Availability & defaults

- [ ] `LauncherRepository.kt:462` — `WidgetType.NEWS_FEED -> true` stays, but confirm
      `MainActivity.kt:197-199` (`WidgetType.entries.filter { repository.isWidgetAvailable(it) }`)
      still surfaces it. The widget must remain *addable but unconfigured*, never hidden for
      lacking a key.
- [ ] `LauncherRepository.kt:1365` — the default left-page instance `CustomWidgetConfig("w_news", …)`.
      Keep it, and confirm first-run users land on the `NeedsApiKey` CTA rather than a broken card.
- [ ] `SetupGuideScreen.kt:73-78` — the seed set
      `setOf(WEATHER_FORECAST, QUICK_NOTE, NEWS_FEED)` means most users hit an unconfigured news
      widget immediately. Acceptable **only** because §6.4's `NeedsApiKey` state is polished; otherwise
      remove `NEWS_FEED` from the seed.

### 7.5 Dead code to retire

- [ ] `WidgetIntegrationsManager.kt` `IntegrationCategory.NEWS` branch (around line 200) launched a
      third-party news app. Decide whether to keep it (it is an "open a news app" integration, not a
      feed) or remove it now that the launcher renders its own feed. Keep it if it is user-facing in
      `SettingsStudioSheet`'s integrations page; do not leave two competing news concepts undocumented.

---

## 8. Security Tasks

- [ ] **Never** place the key in `BuildConfig`, `.env`, `strings.xml`, or any resource — R8/resource
      shrinking ships all of them and the key would be extractable. The `secrets` Gradle plugin
      (`app/build.gradle.kts:9`, 140-143) and `.env.example` are for CI only and must stay unused here.
- [ ] Send the key only in the `Authorization` header, never as `?apiKey=`.
- [ ] Redact the key in all logs and error messages. Never log the full request URL *and* headers
      together. Log at most `key.takeLast(4)` prefixed with `***`.
- [ ] Confirm `release` and `debug` both set `isMinifyEnabled = true`
      (`app/build.gradle.kts:87-101`), so no debug-only code path can be shipped that prints the key.
- [ ] Verify the redirect-host guard (§4.3) — `HttpURLConnection` forwards the `Authorization` header
      across redirects on some Android versions.
- [ ] Document in the wizard that the key is stored in app-private `SharedPreferences` on this device
      only, and never synced or transmitted anywhere but `api.currentsapi.services`.
- [ ] Offer "Remove API key" in the wizard footer (§5.1) and confirm it clears
      `api_key`/`configured` while leaving the user's category/cadence prefs intact.
- [ ] Planned follow-up (not this change): migrate to `EncryptedSharedPreferences` once
      `androidx.security-crypto` is added. Note it as a known limitation in a code comment so it is
      not mistaken for an oversight.

---

## 9. Testing Tasks

Testing infra is present: JUnit4, Robolectric (`ExampleRobolectricTest.kt:13`), kotlinx-coroutines-test,
Compose UI test, and **Roborazzi** for screenshot tests.

### 9.1 Pure unit tests (no network) — highest value

- [ ] `NewsUrlBuilderTest` — assert `/latest-news` never emits `keywords`/`query`/`start_date`/`end_date`/`limit`
      under any preference combination; assert `/search` never emits them when both text fields are blank.
- [ ] `NewsPublishedParserTest` — assert `2026-09-07 14:22:08 +0000` parses to the correct instant;
      assert `null`/garbage yields a `null` epoch (not 0, not an exception).
- [ ] `NewsArticleMapperTest` — `image = "None"` ⇒ `imageUrl == null`; `image = ""` ⇒ null;
      `url = "https://www.reuters.com/x"` ⇒ publisher `reuters.com`; blank `title` dropped;
      duplicate `id` de-duplicated.
- [ ] `NewsApiErrorMapperTest` — 400/401/429/500 and thrown `UnknownHostException` map to the right `NewsError`.
- [ ] `NewsQuotaPolicyTest` — refresh is skipped when `remaining <= reserve`, when inside TTL, when
      inside backoff, and when offline; `force = true` bypasses TTL but **not** quota or backoff.
- [ ] `NewsPreferencesSanitizeTest` — a persisted unknown category is dropped; `page_size = 100`
      clamps to 20; `window_hours = 500` clamps to 168.
- [ ] `NewsSetupWizardReducerTest` — extract wizard step transitions into a pure reducer so the
      step logic is testable without Compose.

### 9.2 Coroutine / integration tests

- [ ] `NewsRepositoryTest` with a fake client and a `TestScope`: concurrent `refresh()` calls result
      in exactly one fetch; a 429 sets `backoff_until_epoch` and a subsequent refresh is a no-op.
- [ ] `NewsCacheTest` — write, reload, corrupt-file tolerance (fall back to empty, do not crash).

### 9.3 Compose / Robolectric tests

- [ ] Render `NewsWidget` for each of `NewsFeedState` × `{"4x1","4x2","4x3","4x4"}`; assert no crash and
      that `news_widget_state` is present in error states.
- [ ] Assert `NeedsApiKey` renders the setup CTA and that tapping it invokes `onOpenSetup` — this is
      the regression test for "asks for the API on initial load".
- [ ] Roborazzi golden captures for every state × size, following the existing pattern in
      `app/src/test/screenshots/`.

### 9.4 Manual verification (device, free key)

- [ ] Paste the full `curl` command into the key field; confirm it is parsed and validated.
- [ ] Reach the widget with the device in airplane mode; confirm cached stories still render.
- [ ] Exhaust the daily quota; confirm the 429 path shows a countdown and stops fetching.
- [ ] Rotate between all four sizes; confirm no clipping at 70 dp and 365 dp.
- [ ] Confirm the key never appears in `adb logcat`.
- [ ] Confirm back-to-home → background → home triggers at most one request.

---

## 10. Build, ProGuard & Manifest

- [ ] `AndroidManifest.xml` — **no change required.** `INTERNET` (line 5) and `ACCESS_NETWORK_STATE`
      (line 6) are already declared.
- [ ] `app/proguard-rules.pro` — no new rules needed: `org.json` is a platform class, and Coil is
      already kept (lines 21-22). Verify with a minified release build that parsing still works, since
      `news`/`category` are accessed reflectively via `JSONObject`.
- [ ] `app/build.gradle.kts` — **no new dependency.** If a client is ever introduced, `retrofit`,
      `okhttp`, and `moshi` are already declared-but-commented at lines 186-190; prefer staying on
      `HttpURLConnection` to match the existing code.
- [ ] Confirm the release build shrinks without breaking the `AsyncImage` model resolution
      (`isShrinkResources = true`, `app/build.gradle.kts:93`).

---

## 11. Known Risks & Open Questions

- [ ] **Comma-separated `category` semantics are undocumented** — OR or AND? Verify empirically on a
      real key with `category=technology,science` and adjust the setup copy if the result is an
      intersection. If AND, multi-select must become single-select or be split into separate requests
      (which would change the quota model — decide before shipping).
- [ ] **Exact free-tier daily request count is not in the docs.** Confirm it against a real key and
      record it in `RemoteConfig` as a documented constant, while keeping the header-driven
      `X-RateLimit-Remaining` logic as the source of truth.
- [ ] Whether `/available/*` counts against the daily quota is undocumented. §3.4 sidesteps this by
      hardcoding the catalogues — keep it that way.
- [ ] `published` timestamps appear to be UTC-only. If a non-UTC offset ever appears, the
      `SimpleDateFormat(..., "Z")` parser already handles it — add a test case with `+0530`.
- [ ] **Pre-existing stray closing braces** were reported at `LeftWidgetsScreen.kt:267,1242,1343,1920`,
      `Carousel4x2Widgets.kt:757`, and `SystemWidgetPickerSheet.kt:378`. Confirm these compile (the
      project presumably builds) before assuming anything is broken, and avoid editing those exact
      lines during this change.
- [ ] `batteryNperf.md` §6/§7 and `refactor.md` Phase 1 both call out the very code this change
      replaces. Land this widget such that both documents can be marked satisfied: the repository-scoped
      fetch, TTL gating, and lifecycle-bound refresh satisfy §6/§7; introducing a dedicated
      `NewsRepositoryImpl` alongside (not inside) `LauncherRepository` is consistent with the
      decomposition Phase 1 wants.

---

## 12. File Change Map

| File | Change | Type |
|---|---|---|
| `domain/RemoteConfig.kt` | Currents endpoints, limits, catalogues | edit |
| `domain/NewsRepository.kt` | new interface | **new** |
| `data/AppModel.kt` | `NewsStory`, `NewsFeedState`, `NewsError`, `NewsPreferencesState`, `NewsCategory` | edit |
| `data/NewsPreferences.kt` | new prefs wrapper | **new** |
| `data/NewsApiClient.kt` | new HTTP + JSON layer | **new** |
| `data/NewsRepositoryImpl.kt` | new repository | **new** |
| `ui/components/NewsWidget.kt` | widget layouts + all states | **new** |
| `ui/components/NewsWidgetSetupSheet.kt` | 6-step wizard | **new** |
| `ui/LauncherViewModel.kt` | repo construction, flows, panel flag, refresh/save/clear | edit |
| `ui/components/Carousel4x2Widgets.kt` | replace stub `:111-120`; thread params | edit |
| `ui/components/LeftWidgetsScreen.kt` | replace body `:434-436`; delete `:1397-1629`; thread params | edit |
| `ui/components/SetupGuideScreen.kt` | optional first-run step | edit |
| `MainActivity.kt` | collect flows, host sheet, `isAnyPanelOpen`, `BackHandler` | edit |
| `data/LauncherRepository.kt` | verify `:462`, `:1365` | verify |
| `app/src/test/...` | unit + Compose tests | **new** |
| `AndroidManifest.xml`, `app/build.gradle.kts`, `proguard-rules.pro` | none required | verify |

---

## 13. Suggested Implementation Order

1. **§3 + §4.1** — constants and models. Nothing else compiles without these.
2. **§4.2 + §4.3** — prefs + API client, with the §9.1 unit tests written alongside.
3. **§4.4** — repository with the full quota/TTL/single-flight policy, plus §9.2 tests.
4. **§7.2** — thread the flow from `LauncherViewModel` → `MainActivity` → the two hosts.
5. **§6** — widget UI with every state screen, wired to the placeholder slots.
6. **§5** — the wizard; the widget is already usable in its `NeedsApiKey` state from step 5.
7. **§7.3 + §8 + §9** — panel plumbing, key hygiene, tests, release verification.
8. **§5.8** — the optional onboarding step, last, since it is a nice-to-have.

Each stage leaves the app in a working state: stages 1-3 are invisible, stage 4 shows cached
content, stage 5 makes it fully interactive, stage 6 unlocks it.
