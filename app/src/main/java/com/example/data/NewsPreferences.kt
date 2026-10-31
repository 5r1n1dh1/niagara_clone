package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.domain.RemoteConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NewsPreferences(context: Context) {
    private val Context.newsStore: DataStore<Preferences> by preferencesDataStore(name = "flux_news_prefs")
    private val store = context.newsStore

    fun load(): NewsPreferencesState = runBlocking {
        val prefs = store.data.first()
        val apiKey = stringPreferencesKey("api_key").let { prefs[it] } ?: ""
        val configured = booleanPreferencesKey("configured").let { prefs[it] } ?: false
        val language = stringPreferencesKey("language").let { prefs[it] } ?: "en"
        val country = stringPreferencesKey("country").let { prefs[it] } ?: ""
        val rawCategories = prefs[stringSetPreferencesKey("categories")]?.toSet() ?: setOf("general")
        val sanitizedCategories = rawCategories.filter { it in RemoteConfig.CURRENTS_CATEGORIES }.toSet().ifEmpty { setOf("general") }
        val keywords = stringPreferencesKey("keywords").let { prefs[it] } ?: ""
        val query = stringPreferencesKey("query").let { prefs[it] } ?: ""
        val useDateWindow = booleanPreferencesKey("use_date_window").let { prefs[it] } ?: false
        val windowHours = (intPreferencesKey("window_hours").let { prefs[it] } ?: 24).coerceIn(1, RemoteConfig.CURRENTS_FREE_MAX_WINDOW_HOURS)
        val requireImages = booleanPreferencesKey("require_images").let { prefs[it] } ?: false
        val pageSize = (intPreferencesKey("page_size").let { prefs[it] } ?: 20).coerceIn(1, RemoteConfig.CURRENTS_FREE_MAX_PAGE_SIZE)
        val minRefreshMinutes = intPreferencesKey("min_refresh_minutes").let { prefs[it] } ?: 60
        val lastSuccessEpoch = longPreferencesKey("last_success_epoch").let { prefs[it] } ?: 0L
        val rateLimitRemaining = intPreferencesKey("rate_limit_remaining").let { prefs[it] } ?: 100
        val rateLimitResetEpoch = longPreferencesKey("rate_limit_reset_epoch").let { prefs[it] } ?: 0L
        val backoffUntilEpoch = longPreferencesKey("backoff_until_epoch").let { prefs[it] } ?: 0L
        // Quota is a paid-for resource: background auto-refresh is opt-in and defaults to OFF.
        val autoRefreshEnabled = booleanPreferencesKey("auto_refresh_enabled").let { prefs[it] } ?: false

        NewsPreferencesState(
            apiKey = apiKey,
            configured = configured,
            language = language,
            country = country,
            categories = sanitizedCategories,
            keywords = keywords,
            query = query,
            useDateWindow = useDateWindow,
            windowHours = windowHours,
            requireImages = requireImages,
            pageSize = pageSize,
            minRefreshMinutes = minRefreshMinutes,
            lastSuccessEpoch = lastSuccessEpoch,
            rateLimitRemaining = rateLimitRemaining,
            rateLimitResetEpoch = rateLimitResetEpoch,
            backoffUntilEpoch = backoffUntilEpoch,
            autoRefreshEnabled = autoRefreshEnabled
        )
    }

    fun save(state: NewsPreferencesState) {
        runBlocking {
            store.edit { mutablePrefs ->
                mutablePrefs[stringPreferencesKey("api_key")] = state.apiKey
                mutablePrefs[booleanPreferencesKey("configured")] = state.configured
                mutablePrefs[stringPreferencesKey("language")] = state.language
                mutablePrefs[stringPreferencesKey("country")] = state.country
                mutablePrefs[stringSetPreferencesKey("categories")] = state.categories
                mutablePrefs[stringPreferencesKey("keywords")] = state.keywords
                mutablePrefs[stringPreferencesKey("query")] = state.query
                mutablePrefs[booleanPreferencesKey("use_date_window")] = state.useDateWindow
                mutablePrefs[intPreferencesKey("window_hours")] = state.windowHours
                mutablePrefs[booleanPreferencesKey("require_images")] = state.requireImages
                mutablePrefs[intPreferencesKey("page_size")] = state.pageSize
                mutablePrefs[intPreferencesKey("min_refresh_minutes")] = state.minRefreshMinutes
                mutablePrefs[longPreferencesKey("last_success_epoch")] = state.lastSuccessEpoch
                mutablePrefs[intPreferencesKey("rate_limit_remaining")] = state.rateLimitRemaining
                mutablePrefs[longPreferencesKey("rate_limit_reset_epoch")] = state.rateLimitResetEpoch
                mutablePrefs[longPreferencesKey("backoff_until_epoch")] = state.backoffUntilEpoch
                mutablePrefs[booleanPreferencesKey("auto_refresh_enabled")] = state.autoRefreshEnabled
            }
        }
    }

    fun clearSecret() {
        runBlocking {
            store.edit { mutablePrefs ->
                mutablePrefs[stringPreferencesKey("api_key")] = ""
                mutablePrefs[booleanPreferencesKey("configured")] = false
            }
        }
    }

    fun saveRateLimit(remaining: Int, resetEpochSeconds: Long) {
        runBlocking {
            store.edit { mutablePrefs ->
                mutablePrefs[intPreferencesKey("rate_limit_remaining")] = remaining
                mutablePrefs[longPreferencesKey("rate_limit_reset_epoch")] = resetEpochSeconds
            }
        }
    }
}
