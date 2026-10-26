package com.example.data

import kotlinx.coroutines.flow.StateFlow
import com.example.domain.DockRepository

/**
 * DockRepository implementation — Agent C split.
 */
class DockRepositoryImpl : DockRepository {
    override val dockPackageNames: StateFlow<List<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override fun setDockSlot(slotIndex: Int, packageName: String) {}
    override fun clearDockSlot(slotIndex: Int) {}
    override fun resetDockToDefaults() {}
}
