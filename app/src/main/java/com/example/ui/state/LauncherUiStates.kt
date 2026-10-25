package com.example.ui.state

import com.example.data.*

/**
 * Normalized UI States adhering to Phase 5 refactoring guidelines.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val favorites: List<AppItem> = emptyList(),
        val dockApps: List<AppItem> = emptyList(),
        val carouselWidgets: List<WidgetType> = emptyList(),
        val weather: WeatherInfo = WeatherInfo(),
        val media: MediaPlaybackState = MediaPlaybackState(),
        val calendarEvents: List<CalendarEvent> = emptyList(),
        val settings: LauncherSettings = LauncherSettings(),
        val isSearchOpen: Boolean = false,
        val isSettingsOpen: Boolean = false,
        val isNotificationCenterOpen: Boolean = false
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface AllAppsUiState {
    data object Loading : AllAppsUiState
    data class Content(
        val allApps: List<AppItem> = emptyList(),
        val filteredApps: List<AppItem> = emptyList(),
        val searchQuery: String = "",
        val userCategories: List<String> = emptyList(),
        val folders: List<FolderItem> = emptyList(),
        val waveSelectedLetter: String? = null
    ) : AllAppsUiState
    data class Error(val message: String) : AllAppsUiState
}

sealed interface WidgetsUiState {
    data object Loading : WidgetsUiState
    data class Content(
        val widgets: List<CustomWidgetConfig> = emptyList(),
        val availableWidgetTypes: List<WidgetType> = emptyList(),
        val isEditMode: Boolean = false,
        val editingNote: CustomWidgetConfig? = null
    ) : WidgetsUiState
    data class Error(val message: String) : WidgetsUiState
}

data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val isStudioOpen: Boolean = false,
    val selectedTab: Int = 0
)

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Content(
        val notifications: Map<String, NotificationItem> = emptyMap(),
        val allNotifications: Map<String, List<NotificationItem>> = emptyMap()
    ) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}

data class SearchUiState(
    val query: String = "",
    val filteredApps: List<AppItem> = emptyList(),
    val mathResult: String = ""
)
