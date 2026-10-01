package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.ui.state.AllAppsUiState
import kotlinx.coroutines.flow.*

/**
 * Decomposed AllAppsViewModel adhering to Phase 3 & Phase 5 refactoring guidelines.
 */
class AllAppsViewModel(application: Application) : AndroidViewModel(application) {
    val repository = LauncherRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<AllAppsUiState> = combine(
        repository.allApps,
        repository.userCategories,
        repository.folders,
        _searchQuery
    ) { apps, categories, folders, query ->
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
        }
        AllAppsUiState.Content(
            allApps = apps,
            filteredApps = filtered,
            searchQuery = query,
            userCategories = categories,
            folders = folders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AllAppsUiState.Loading
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
