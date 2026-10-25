package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.ui.state.WidgetsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WidgetsViewModel @Inject constructor(
    application: Application,
    private val repository: LauncherRepository
) : AndroidViewModel(application) {

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
