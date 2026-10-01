package com.example.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Category Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface CategoryRepository {
    val userCategories: StateFlow<List<String>>
    fun addUserCategory(name: String)
    fun removeUserCategory(name: String)
}
