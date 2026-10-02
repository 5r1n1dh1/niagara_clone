package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.domain.NewsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    init {
        val prefs = preferencesWrapper.load()
        if (prefs.apiKey.isBlank()) {
            _newsState.value = NewsFeedState.NeedsApiKey
        } else {
            _newsState.value = NewsFeedState.Loading(emptyList())
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

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
