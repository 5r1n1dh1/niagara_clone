package com.example.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.example.domain.RemoteConfig

class NewsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("flux_news_prefs", MODE_PRIVATE)

    fun load(): NewsPreferencesState {
        val apiKey = prefs.getString("api_key", "") ?: ""
        val configured = prefs.getBoolean("configured", false)
        val language = prefs.getString("language", "en") ?: "en"
        val country = prefs.getString("country", "") ?: ""
        val rawCategories = prefs.getStringSet("categories", setOf("general")) ?: setOf("general")
        val sanitizedCategories = rawCategories.filter { it in RemoteConfig.CURRENTS_CATEGORIES }.toSet().ifEmpty { setOf("general") }
        val keywords = prefs.getString("keywords", "") ?: ""
        val query = prefs.getString("query", "") ?: ""
        val useDateWindow = prefs.getBoolean("use_date_window", false)
        val windowHours = (prefs.getInt("window_hours", 24)).coerceIn(1, RemoteConfig.CURRENTS_FREE_MAX_WINDOW_HOURS)
        val requireImages = prefs.getBoolean("require_images", false)
        val pageSize = (prefs.getInt("page_size", 20)).coerceIn(1, RemoteConfig.CURRENTS_FREE_MAX_PAGE_SIZE)
        val minRefreshMinutes = prefs.getInt("min_refresh_minutes", 60)
        val lastSuccessEpoch = prefs.getLong("last_success_epoch", 0L)
        val rateLimitRemaining = prefs.getInt("rate_limit_remaining", 100)
        val rateLimitResetEpoch = prefs.getLong("rate_limit_reset_epoch", 0L)
        val backoffUntilEpoch = prefs.getLong("backoff_until_epoch", 0L)

        return NewsPreferencesState(
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
            backoffUntilEpoch = backoffUntilEpoch
        )
    }

    fun save(state: NewsPreferencesState) {
        prefs.edit()
            .putString("api_key", state.apiKey)
            .putBoolean("configured", state.configured)
            .putString("language", state.language)
            .putString("country", state.country)
            .putStringSet("categories", state.categories)
            .putString("keywords", state.keywords)
            .putString("query", state.query)
            .putBoolean("use_date_window", state.useDateWindow)
            .putInt("window_hours", state.windowHours)
            .putBoolean("require_images", state.requireImages)
            .putInt("page_size", state.pageSize)
            .putInt("min_refresh_minutes", state.minRefreshMinutes)
            .putLong("last_success_epoch", state.lastSuccessEpoch)
            .putInt("rate_limit_remaining", state.rateLimitRemaining)
            .putLong("rate_limit_reset_epoch", state.rateLimitResetEpoch)
            .putLong("backoff_until_epoch", state.backoffUntilEpoch)
            .apply()
    }

    fun clearSecret() {
        prefs.edit()
            .putString("api_key", "")
            .putBoolean("configured", false)
            .apply()
    }

    fun saveRateLimit(remaining: Int, resetEpochSeconds: Long) {
        prefs.edit()
            .putInt("rate_limit_remaining", remaining)
            .putLong("rate_limit_reset_epoch", resetEpochSeconds)
            .apply()
    }
}
