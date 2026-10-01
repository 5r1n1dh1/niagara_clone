package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Settings Repository Interface - Clean Architecture Separation of Concerns
 */
interface SettingsRepository {
    val settings: StateFlow<LauncherSettings>
    fun updateSettings(newSettings: LauncherSettings)
}
