package com.example.data

import kotlinx.coroutines.flow.StateFlow
import com.example.domain.FolderRepository

/**
 * FolderRepository implementation — Agent C split.
 */
class FolderRepositoryImpl : FolderRepository {
    override val folders: StateFlow<List<FolderItem>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
}
