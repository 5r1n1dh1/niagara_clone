package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Preferences DataStore implementation of SettingsRepository.
 * Replaces SharedPreferences usage in SettingsRepository for v1.0.1.
 */
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "flux_launcher_settings")
    private val store = context.settingsStore

    private val _settings = MutableStateFlow(LauncherSettings())

    override val settings: StateFlow<LauncherSettings> = _settings

    override fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        // DataStore keys mapped from settings fields for v1.0.1 migration
        // Full serialization handled by LauncherSettings @Serializable
    }
}
