package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.NewsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
            repositoryScope.launch {
                refresh(force = false)
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

            if (!isOnline()) {
                if (cachedStories.isNotEmpty()) {
                    _newsState.value = NewsFeedState.Stale(cachedStories, NewsError.Offline)
                } else {
                    _newsState.value = NewsFeedState.Failed(NewsError.Offline)
                }
                return
            }

            val now = System.currentTimeMillis() / 1000
            if (prefs.backoffUntilEpoch > now) {
                if (cachedStories.isNotEmpty()) {
                    _newsState.value = NewsFeedState.Stale(cachedStories, NewsError.QuotaExceeded(prefs.backoffUntilEpoch - now))
                }
                return
            }

            if (!force && isBatteryOptimizationEnabled()) {
                // Battery optimization mode: skip automatic refresh but keep cached content visible
                if (cachedStories.isNotEmpty()) {
                    _newsState.value = NewsFeedState.Ready(cachedStories, prefs.lastSuccessEpoch * 1000, prefs.rateLimitRemaining)
                }
                return
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
                val Context.settingsStore by preferencesDataStore("flux_launcher_settings")
                val prefs = context.settingsStore.data.first()
                prefs[androidx.datastore.preferences.core.booleanPreferencesKey("batteryOptimizationMode")] ?: true
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
