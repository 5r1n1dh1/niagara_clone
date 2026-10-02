package com.example.data

/**
 * Data models and state definitions for the Currents API news feed widget.
 */
data class NewsStory(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val publisher: String,
    val imageUrl: String?,
    val category: String,
    val publishedAtEpochMillis: Long
)

data class NewsPreferencesState(
    val apiKey: String = "",
    val configured: Boolean = false,
    val language: String = "en",
    val country: String = "",
    val categories: Set<String> = setOf("general"),
    val keywords: String = "",
    val query: String = "",
    val useDateWindow: Boolean = false,
    val windowHours: Int = 24,
    val requireImages: Boolean = false,
    val pageSize: Int = 20,
    val minRefreshMinutes: Int = 60,
    val lastSuccessEpoch: Long = 0L,
    val rateLimitRemaining: Int = 100,
    val rateLimitResetEpoch: Long = 0L,
    val backoffUntilEpoch: Long = 0L
)

sealed interface NewsError {
    data object Offline : NewsError
    data class InvalidKey(val msg: String) : NewsError
    data class QuotaExceeded(val retryAfterSeconds: Long) : NewsError
    data class BadRequest(val field: String?, val msg: String) : NewsError
    data class Server(val code: Int) : NewsError
    data class Unknown(val msg: String) : NewsError
}

sealed interface NewsFeedState {
    data object NeedsApiKey : NewsFeedState
    data class Loading(val cached: List<NewsStory>) : NewsFeedState
    data class Ready(val stories: List<NewsStory>, val lastUpdated: Long, val remainingQuota: Int) : NewsFeedState
    data class Stale(val stories: List<NewsStory>, val lastError: NewsError) : NewsFeedState
    data class Failed(val error: NewsError) : NewsFeedState
}

data class NewsCategory(val code: String, val label: String)
