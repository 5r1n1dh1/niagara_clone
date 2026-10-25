package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.data.LauncherSettings
import com.example.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: LauncherRepository
) : AndroidViewModel(application) {

    val uiState: StateFlow<SettingsUiState> = repository.settings.map { settings ->
        SettingsUiState(settings = settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateSettings(newSettings: LauncherSettings) {
        val current = repository.settings.value
        if (current != newSettings) {
            repository.updateSettings(newSettings)
        }
    }
}
