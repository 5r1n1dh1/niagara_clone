package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import com.example.domain.NewsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "flux_launcher_settings")

/**
 * Why an automatic (non-forced) news refresh is being skipped.
 *
 * Kept as its own type so the decision is pure, deterministic and unit-testable without touching
 * DataStore, the network stack or a real device.
 */
enum class AutoRefreshBlock {
    /** Auto-refresh is not opted into. Cached content stays visible; no network call. */
    NOT_ENABLED,

    /** The device is in battery-optimization mode. Same contract: no network call. */
    BATTERY_OPTIMIZATION,

    /** The remaining quota is at or below the floor — spending more would 429 the user's key. */
    QUOTA_FLOOR_REACHED,

    /** The device is offline. */
    OFFLINE,

    /** A previous failure/backoff is still in force. */
    BACKOFF
}

/**
 * Single source of truth for "should this automatic refresh hit the network?".
 *
 * The user's news API quota is finite and paid for, so every automatic call must pass this gate.
 * `force == true` (an explicit user pull-to-refresh / "Refresh now") bypasses all of it except
 * offline/backoff, because a human asking for fresh data has knowingly chosen to spend quota.
 */
object NewsQuotaPolicy {

    /** Below this many remaining calls, automatic refreshes stop entirely. */
    const val QUOTA_FLOOR = 10

    /**
     * @param nowEpochSeconds  current wall clock in seconds.
     * @param force            user explicitly requested the refresh.
     * @param autoRefreshEnabled user opt-in for background refreshes (persisted, default false).
     * @param batteryOptimizationMode launcher-wide battery saver setting.
     * @param isOffline        no usable network.
     * @param rateLimitRemaining remaining calls reported by the API / a previous 429; `null` when unknown.
     * @param backoffUntilEpochSeconds retry-after deadline from the last failure; `null` when none.
     * @return `null` when the refresh may proceed, else the reason it is blocked.
     *
     * Note: `null` means "allowed" — it does not imply data will be fetched, because the caller
     * still applies the TTL check. Callers decide `Stale` vs `Failed` from their own cache state.
     */
    fun blockingReason(
        nowEpochSeconds: Long,
        force: Boolean,
        autoRefreshEnabled: Boolean,
        batteryOptimizationMode: Boolean,
        isOffline: Boolean,
        rateLimitRemaining: Int?,
        backoffUntilEpochSeconds: Long?
    ): AutoRefreshBlock? {
        if (isOffline) return AutoRefreshBlock.OFFLINE
        if (backoffUntilEpochSeconds != null && backoffUntilEpochSeconds > nowEpochSeconds) {
            return AutoRefreshBlock.BACKOFF
        }
        if (force) return null

        if (!autoRefreshEnabled) return AutoRefreshBlock.NOT_ENABLED
        if (batteryOptimizationMode) return AutoRefreshBlock.BATTERY_OPTIMIZATION
        // A null/quota-unknown value must never be treated as "plenty of quota left".
        val remaining = rateLimitRemaining
        if (remaining != null && remaining <= QUOTA_FLOOR) return AutoRefreshBlock.QUOTA_FLOOR_REACHED
        return null
    }

    /** Human/machine readable retry hint in seconds for a quota-blocked refresh, else `null`. */
    fun quotaRetryAfterSeconds(rateLimitResetEpochSeconds: Long?, nowEpochSeconds: Long): Long? {
        if (rateLimitResetEpochSeconds == null || rateLimitResetEpochSeconds <= 0L) return null
        val delta = rateLimitResetEpochSeconds - nowEpochSeconds
        return if (delta > 0) delta else null
    }
}

class NewsRepositoryImpl(private val context: Context) : NewsRepository {
    private val preferencesWrapper = NewsPreferences(context)
    private val apiClient = NewsApiClient(context, preferencesWrapper)
    private val mutex = Mutex()

    private val _preferences = MutableStateFlow(preferencesWrapper.load())
    override val preferences: StateFlow<NewsPreferencesState> = _preferences.asStateFlow()

    private val _newsState = MutableStateFlow<NewsFeedState>(NewsFeedState.NeedsApiKey)
    override val newsState: StateFlow<NewsFeedState> = _newsState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var cachedStories: List<NewsStory> = emptyList()
    private val cacheFile = File(context.cacheDir, "news_cache.json")

    init {
        cachedStories = loadCache()
        val prefs = preferencesWrapper.load()
        if (prefs.apiKey.isBlank()) {
            _newsState.value = NewsFeedState.NeedsApiKey
        } else {
            if (cachedStories.isNotEmpty()) {
                _newsState.value = NewsFeedState.Ready(cachedStories, prefs.lastSuccessEpoch * 1000, prefs.rateLimitRemaining)
            } else {
                _newsState.value = NewsFeedState.Loading(emptyList())
            }
            // Quota gate: an automatic refresh is only ever scheduled when the user has explicitly
            // opted in (`autoRefreshEnabled`, default false). Otherwise the cache stays on screen and
            // nothing is spent — the user can still force a refresh themselves.
            if (prefs.autoRefreshEnabled) {
                repositoryScope.launch {
                    refresh(force = false)
                }
            }
        }
    }

    override suspend fun refresh(force: Boolean) {
        mutex.withLock {
            val prefs = preferencesWrapper.load()
            _preferences.value = prefs

            if (prefs.apiKey.isBlank()) {
                _newsState.value = NewsFeedState.NeedsApiKey
                return
            }

            val now = System.currentTimeMillis() / 1000
            val block = NewsQuotaPolicy.blockingReason(
                nowEpochSeconds = now,
                force = force,
                autoRefreshEnabled = prefs.autoRefreshEnabled,
                batteryOptimizationMode = isBatteryOptimizationEnabled(),
                isOffline = !isOnline(),
                rateLimitRemaining = prefs.rateLimitRemaining,
                backoffUntilEpochSeconds = prefs.backoffUntilEpoch
            )

            when (block) {
                AutoRefreshBlock.OFFLINE -> {
                    // Never clears the cache: the user keeps reading what's already on the device.
                    if (cachedStories.isNotEmpty()) {
                        _newsState.value = NewsFeedState.Stale(cachedStories, NewsError.Offline)
                    } else {
                        _newsState.value = NewsFeedState.Failed(NewsError.Offline)
                    }
                    return
                }

                AutoRefreshBlock.BACKOFF -> {
                    val retryAfter = (prefs.backoffUntilEpoch - now).coerceAtLeast(1L)
                    if (cachedStories.isNotEmpty()) {
                        _newsState.value = NewsFeedState.Stale(cachedStories, NewsError.QuotaExceeded(retryAfter))
                    } else {
                        _newsState.value = NewsFeedState.Failed(NewsError.QuotaExceeded(retryAfter))
                    }
                    return
                }

                AutoRefreshBlock.NOT_ENABLED, AutoRefreshBlock.BATTERY_OPTIMIZATION -> {
                    // Automatic refresh is not opted into / the device is in battery-saver mode.
                    // Leave whatever is on screen alone — the cache is the content here.
                    if (cachedStories.isNotEmpty()) {
                        _newsState.value = NewsFeedState.Ready(cachedStories, prefs.lastSuccessEpoch * 1000, prefs.rateLimitRemaining)
                    }
                    return
                }

                AutoRefreshBlock.QUOTA_FLOOR_REACHED -> {
                    // Precious quota: surface the cache with an explicit "quota ran out" reason and a
                    // retry-after hint instead of spending another call on a feed the user already has.
                    val retryAfter = NewsQuotaPolicy.quotaRetryAfterSeconds(
                        rateLimitResetEpochSeconds = prefs.rateLimitResetEpoch,
                        nowEpochSeconds = now
                    ) ?: (prefs.rateLimitResetEpoch - now)
                    val error = NewsError.QuotaExceeded(retryAfter.coerceAtLeast(1L))
                    if (cachedStories.isNotEmpty()) {
                        _newsState.value = NewsFeedState.Stale(cachedStories, error)
                    } else {
                        _newsState.value = NewsFeedState.Failed(error)
                    }
                    return
                }

                null -> Unit // allowed through to the TTL check below
            }

            if (!force) {
                val ttlSeconds = prefs.minRefreshMinutes * 60L
                if (prefs.lastSuccessEpoch + ttlSeconds > now && cachedStories.isNotEmpty()) {
                    _newsState.value = NewsFeedState.Ready(cachedStories, prefs.lastSuccessEpoch * 1000, prefs.rateLimitRemaining)
                    return
                }
            }

            try {
                if (cachedStories.isEmpty()) {
                    _newsState.value = NewsFeedState.Loading(emptyList())
                }
                val result = apiClient.fetch(page = 1)
                cachedStories = result.stories
                saveCache(cachedStories)
                val updatedPrefs = preferencesWrapper.load().copy(
                    lastSuccessEpoch = System.currentTimeMillis() / 1000,
                    rateLimitRemaining = result.rateLimitRemaining,
                    rateLimitResetEpoch = result.rateLimitResetEpoch
                )
                preferencesWrapper.save(updatedPrefs)
                _preferences.value = updatedPrefs
                _newsState.value = NewsFeedState.Ready(cachedStories, updatedPrefs.lastSuccessEpoch * 1000, result.rateLimitRemaining)
            } catch (e: NewsApiException) {
                handleError(e.newsError)
            } catch (t: Throwable) {
                handleError(NewsError.Unknown(t.message ?: "Unknown error"))
            }
        }
    }

    private fun handleError(error: NewsError) {
        val now = System.currentTimeMillis() / 1000
        val prefs = preferencesWrapper.load()
        when (error) {
            is NewsError.InvalidKey -> {
                clearApiKey()
                _newsState.value = NewsFeedState.NeedsApiKey
            }
            is NewsError.QuotaExceeded -> {
                val backoff = now + error.retryAfterSeconds
                val prefs = preferencesWrapper.load().copy(backoffUntilEpoch = backoff)
                preferencesWrapper.save(prefs)
                _preferences.value = prefs
                if (cachedStories.isNotEmpty()) {
                    _newsState.value = NewsFeedState.Stale(cachedStories, error)
                } else {
                    _newsState.value = NewsFeedState.Failed(error)
                }
            }
            else -> {
                val existingBackoff = prefs.backoffUntilEpoch
                val nextBackoff = if (existingBackoff > now) {
                    minOf((existingBackoff - now) * 3, 3600L)
                } else {
                    300L
                }
                val backoff = now + nextBackoff
                val updatedPrefs = prefs.copy(backoffUntilEpoch = backoff)
                preferencesWrapper.save(updatedPrefs)
                _preferences.value = updatedPrefs
                if (cachedStories.isNotEmpty()) {
                    _newsState.value = NewsFeedState.Stale(cachedStories, error)
                } else {
                    _newsState.value = NewsFeedState.Failed(error)
                }
            }
        }
    }

    override fun savePreferences(state: NewsPreferencesState) {
        preferencesWrapper.save(state)
        _preferences.value = state
        repositoryScope.launch {
            refresh(force = true)
        }
    }

    override fun clearApiKey() {
        preferencesWrapper.clearSecret()
        _preferences.value = preferencesWrapper.load()
        _newsState.value = NewsFeedState.NeedsApiKey
    }

    override fun apiKeyOrNull(): String? {
        val key = preferencesWrapper.load().apiKey
        return key.takeIf { it.isNotBlank() }
    }

    override fun isConfigured(): Boolean {
        return preferencesWrapper.load().configured
    }

    private fun loadCache(): List<NewsStory> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            val jsonStr = cacheFile.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<NewsStory>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(NewsStory(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    url = obj.getString("url"),
                    publisher = obj.getString("publisher"),
                    imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() },
                    category = obj.getString("category"),
                    publishedAtEpochMillis = obj.optLong("publishedAtEpochMillis", -1L).takeIf { it != -1L }
                ))
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveCache(stories: List<NewsStory>) {
        try {
            val arr = JSONArray()
            for (s in stories) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("title", s.title)
                obj.put("description", s.description)
                obj.put("url", s.url)
                obj.put("publisher", s.publisher)
                obj.put("imageUrl", s.imageUrl ?: "")
                obj.put("category", s.category)
                obj.put("publishedAtEpochMillis", s.publishedAtEpochMillis ?: -1L)
                arr.put(obj)
            }
            cacheFile.writeText(arr.toString())
        } catch (_: Throwable) {}
    }

    private fun isBatteryOptimizationEnabled(): Boolean {
        return try {
            runBlocking {
                val prefs = context.settingsStore.data.first()
                prefs[booleanPreferencesKey("batteryOptimizationMode")] ?: true
            }
        } catch (_: Throwable) { true }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
