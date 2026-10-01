package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.data.LauncherSettings
import com.example.ui.state.SettingsUiState
import kotlinx.coroutines.flow.*

/**
 * Decomposed SettingsViewModel managing settings UI state adhering to Phase 3.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application)

    val uiState: StateFlow<SettingsUiState> = repository.settings.map { settings ->
        SettingsUiState(settings = settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateSettings(newSettings: LauncherSettings) {
        repository.updateSettings(newSettings)
    }
}
