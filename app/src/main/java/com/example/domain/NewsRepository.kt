package com.example.domain

import com.example.data.NewsFeedState
import com.example.data.NewsPreferencesState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for the News Feed feature.
 */
interface NewsRepository {
    val newsState: StateFlow<NewsFeedState>
    val preferences: StateFlow<NewsPreferencesState>

    suspend fun refresh(force: Boolean = false)
    fun savePreferences(state: NewsPreferencesState)
    fun clearApiKey()
    fun apiKeyOrNull(): String?
    fun isConfigured(): Boolean
}
