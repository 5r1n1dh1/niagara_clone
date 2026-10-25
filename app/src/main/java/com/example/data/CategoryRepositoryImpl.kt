package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * CategoryRepository implementation — Agent C split.
 */
class CategoryRepositoryImpl : CategoryRepository {
    override val userCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override fun addUserCategory(categoryName: String) {}
    override fun updateUserCategoryName(oldName: String, newName: String) {}
    override fun removeUserCategory(categoryName: String) {}
}
