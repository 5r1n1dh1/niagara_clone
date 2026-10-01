package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppItem
import com.example.data.LauncherRepository
import com.example.util.MathEvaluator
import kotlinx.coroutines.flow.*

data class SearchUiState(
    val query: String = "",
    val filteredApps: List<AppItem> = emptyList(),
    val mathResult: String = ""
)

/**
 * Decomposed SearchViewModel managing debounced search query, math evaluation, and filtered results.
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<SearchUiState> = combine(
        _query.debounce(200),
        repository.allApps
    ) { q, apps ->
        val trimmed = q.trim()
        val filtered = if (trimmed.isBlank()) {
            emptyList()
        } else {
            apps.filter { it.label.contains(trimmed, true) || it.packageName.contains(trimmed, true) }
        }
        val math = if (trimmed.any { it.isDigit() } && trimmed.any { it in "+-*/^" }) {
            MathEvaluator.evaluate(trimmed)
        } else {
            ""
        }
        SearchUiState(query = q, filteredApps = filtered, mathResult = math)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }
}
