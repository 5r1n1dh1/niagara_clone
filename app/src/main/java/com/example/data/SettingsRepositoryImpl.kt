package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Preferences DataStore implementation of SettingsRepository.
 *
 * Persists the full [LauncherSettings] object as a single JSON string
 * (kotlinx.serialization) under one key — one write per change, one read at
 * startup. Uses the SINGLE canonical [launcherSettingsStore] delegate.
 */
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val store = context.launcherSettingsStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("launcher_settings_json")

    private val _settings = MutableStateFlow(LauncherSettings())
    override val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            val persisted = runCatching {
                json.decodeFromString<LauncherSettings>(store.data.first()[key] ?: "")
            }.getOrNull()
            if (persisted != null) _settings.value = persisted
        }
    }

    override fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        scope.launch {
            runCatching {
                store.edit { it[key] = json.encodeToString(LauncherSettings.serializer(), newSettings) }
            }
        }
    }
}
