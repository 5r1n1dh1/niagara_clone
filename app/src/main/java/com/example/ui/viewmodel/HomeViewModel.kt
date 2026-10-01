package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.ui.state.HomeUiState
import kotlinx.coroutines.flow.*

/**
 * Decomposed HomeViewModel adhering to Phase 3 & Phase 5 refactoring guidelines.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LauncherRepository(application)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.favorites,
        repository.dockPackageNames,
        repository.allApps,
        repository.settings
    ) { favs, dockPkgs, apps, settings ->
        val dockApps = dockPkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
        HomeUiState.Content(
            favorites = favs,
            dockApps = dockApps,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    fun reloadData() {
        repository.reloadApps()
    }
}
