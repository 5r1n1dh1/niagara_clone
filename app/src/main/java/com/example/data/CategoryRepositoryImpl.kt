package com.example.data

import kotlinx.coroutines.flow.StateFlow
import com.example.domain.CategoryRepository

/**
 * CategoryRepository implementation — Agent C split.
 */
class CategoryRepositoryImpl : CategoryRepository {
    override val userCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override fun addUserCategory(categoryName: String) {}
    override fun removeUserCategory(categoryName: String) {}
}
