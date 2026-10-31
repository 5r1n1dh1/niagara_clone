package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * SINGLE canonical Preferences DataStore for launcher settings.
 *
 * DataStore enforces one active instance per file per process. Every consumer
 * (SettingsRepositoryImpl, NewsRepositoryImpl, FluxNotificationListenerService)
 * MUST reference this delegate — declaring another `preferencesDataStore` on the
 * same file name throws `IllegalStateException: There are multiple DataStores
 * active for the same file` at first access (the v2.0.0 launch crash).
 */
val Context.launcherSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "flux_launcher_settings")
