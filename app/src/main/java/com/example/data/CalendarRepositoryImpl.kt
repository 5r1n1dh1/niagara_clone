package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * CalendarRepository implementation — Agent C split.
 */
class CalendarRepositoryImpl : com.example.domain.CalendarRepository {
    override val calendarEvents: StateFlow<List<CalendarEvent>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override fun refreshCalendarEvents() {}
}
