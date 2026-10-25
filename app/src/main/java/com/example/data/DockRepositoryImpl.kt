package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * DockRepository implementation — Agent C split.
 */
class DockRepositoryImpl : DockRepository {
    override val dockPackageNames: StateFlow<List<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
}
