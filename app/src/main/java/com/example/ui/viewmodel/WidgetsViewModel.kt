package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.ui.state.WidgetsUiState
import kotlinx.coroutines.flow.*

/**
 * Decomposed WidgetsViewModel managing left-screen UI state adhering to Phase 3.
 */
class WidgetsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application)

    private val _isEditMode = MutableStateFlow(false)

    val uiState: StateFlow<WidgetsUiState> = combine(
        repository.leftWidgets,
        _isEditMode
    ) { widgets, editMode ->
        WidgetsUiState.Content(
            widgets = widgets,
            isEditMode = editMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WidgetsUiState.Loading
    )

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }
}
