package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * AppRepository implementation — Agent C split.
 */
class AppRepositoryImpl(context: Context) : AppRepository {
    override val allApps: StateFlow<List<AppItem>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override val favorites: StateFlow<List<AppItem>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override val isAppsLoading: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
    override fun reloadApps() {}
}
