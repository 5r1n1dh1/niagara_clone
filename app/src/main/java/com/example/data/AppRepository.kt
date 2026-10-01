package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * App Repository Interface - Clean Architecture Separation of Concerns
 */
interface AppRepository {
    val allApps: StateFlow<List<AppItem>>
    val favorites: StateFlow<List<AppItem>>
    val isAppsLoading: StateFlow<Boolean>
    fun reloadApps()
}
