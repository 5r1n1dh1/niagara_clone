package com.example.domain

/**
 * Centralized RemoteConfig for URLs and API endpoints adhering to Phase 8 hardening and Currents API spec.
 */
object RemoteConfig {
    const val DEFAULT_GIT_WALLPAPER_REPO = "https://github.com/dharmx/walls"
    const val DEFAULT_GIT_BRANCH = "main"

    const val CURRENTS_API_BASE = "https://api.currentsapi.services/v1"
    const val CURRENTS_LATEST_NEWS_PATH = "/latest-news"
    const val CURRENTS_SEARCH_PATH = "/search"
    const val CURRENTS_FREE_MAX_PAGE_SIZE = 20
    const val CURRENTS_FREE_MAX_WINDOW_HOURS = 168
    const val CURRENTS_FREE_LOOKBACK_DAYS = 30
    const val CURRENTS_MAX_PAGE_NUMBER = 180
    const val CURRENTS_API_USER_AGENT = "FluxLauncher/1.0 (Android)"

    val CURRENTS_CATEGORIES = listOf(
        "regional", "technology", "lifestyle", "business", "general",
        "programming", "science", "entertainment", "world", "sports",
        "finance", "academia", "politics", "health", "opinion", "food", "game"
    )

    val CURRENTS_LANGUAGES = listOf(
        "en", "ar", "zh", "nl", "fi", "fr", "de", "hi", "it", "ja", "ko", "msa", "pt", "ru", "es"
    )

    val CURRENTS_REGIONS = listOf(
        "US", "GB", "CA", "AU", "DE", "FR", "IN", "JP", "BR", "MX", "IT", "ES", "NL", "RU", "CN"
    )
}
