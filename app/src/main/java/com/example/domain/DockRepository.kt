package com.example.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Dock Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface DockRepository {
    val dockPackageNames: StateFlow<List<String>>
    fun setDockSlot(slotIndex: Int, packageName: String)
    fun clearDockSlot(slotIndex: Int)
    fun resetDockToDefaults()
}
