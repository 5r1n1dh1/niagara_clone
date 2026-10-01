package com.example.domain

import com.example.data.FolderItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Folder Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface FolderRepository {
    val folders: StateFlow<List<FolderItem>>
}
