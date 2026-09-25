package com.pennywiseai.tracker.data.repository

import com.pennywiseai.shared.data.bootstrap.DefaultCategoryData
import com.pennywiseai.tracker.data.database.dao.CategoryDao
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.hierarchical
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories().map { it.hierarchical() }
    }
    
    fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getExpenseCategories().map { it.hierarchical() }
    }
    
    fun getIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getIncomeCategories().map { it.hierarchical() }
    }

    // Visible-only variants for pickers — hidden categories are excluded (#736).
    fun getVisibleCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getVisibleCategories().map { it.hierarchical() }
    }

    fun getVisibleExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getVisibleExpenseCategories().map { it.hierarchical() }
    }

    fun getVisibleIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getVisibleIncomeCategories().map { it.hierarchical() }
    }

    suspend fun setCategoryHidden(categoryId: Long, hidden: Boolean) {
        categoryDao.setCategoryHidden(categoryId, hidden)
    }

    /**
     * Moves a top-level category one step inside its section.
     *
     * [orderedIds] is that section's top-level rows in the order the Categories
     * screen renders them; a no-op when [categoryId] is at the corresponding end
     * or is missing from the list. Sections themselves are not reorderable — the
     * group a category belongs to is a code-level mapping, not data.
     */
    suspend fun moveCategoryWithinSection(orderedIds: List<Long>, categoryId: Long, up: Boolean) {
        val reordered = reorderedSection(orderedIds, categoryId, up) ?: return
        categoryDao.setSectionDisplayOrder(reordered)
    }

    /**
     * Flips a category's hidden flag and returns the row as it now stands.
     * See [CategoryDao.toggleCategoryHidden] for why this isn't a read,
     * flip and write from the caller.
     */
    suspend fun toggleCategoryHidden(categoryId: Long): CategoryEntity? =
        categoryDao.toggleCategoryHiddenCascading(categoryId)

    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return categoryDao.getCategoryById(categoryId)
    }
    
    suspend fun getCategoryByName(categoryName: String): CategoryEntity? {
        return categoryDao.getCategoryByName(categoryName)
    }
    
    suspend fun createCategory(
        name: String,
        color: String,
        isIncome: Boolean = false,
        icon: String? = null,
        parentId: Long? = null
    ): Long {
        val category = CategoryEntity(
            name = name,
            color = color,
            icon = icon,
            parentId = parentId,
            isSystem = false,
            isIncome = isIncome,
            displayOrder = 999
        )
        return categoryDao.insertCategory(category)
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(
            category.copy(updatedAt = LocalDateTime.now())
        )
    }
    
    suspend fun deleteCategory(categoryId: Long): Boolean {
        // Only delete non-system categories
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && !category.isSystem) {
            categoryDao.detachChildren(categoryId)
            categoryDao.deleteCategory(categoryId)
            return true
        }
        return false
    }
    
    suspend fun categoryExists(categoryName: String): Boolean {
        return categoryDao.categoryExists(categoryName)
    }
    
    suspend fun initializeDefaultCategories() {
        // Only initialize if no categories exist
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = DefaultCategoryData.ALL.map { seed ->
                CategoryEntity(
                    name = seed.name,
                    color = seed.colorHex,
                    isSystem = true,
                    isIncome = seed.isIncome,
                    displayOrder = DefaultCategoryData.ALL.indexOf(seed) + 1
                )
            }
            categoryDao.insertCategories(defaultCategories)
        }
    }

    /**
     * Adds any built-in category that shipped since the user's install, without
     * touching categories that already exist.
     *
     * Runs on every app start and after a backup import. Matching is by exact
     * canonical name, so a user who renamed or recoloured a built-in keeps their
     * edit — we never overwrite, rename or duplicate. New rows are appended after
     * the current highest display order. The insert itself is
     * `OnConflictStrategy.IGNORE`, so a race (or a name that exists but was
     * filtered out) can't produce duplicates.
     */
    suspend fun ensureDefaultCategories() {
        if (categoryDao.getCategoryCount() == 0) {
            // Fresh install: seed the full set in canonical order.
            initializeDefaultCategories()
            return
        }
        val existingNames = categoryDao.getAllCategoriesList().mapTo(HashSet()) { it.name }
        val missing = DefaultCategoryData.ALL.filterNot { it.name in existingNames }
        if (missing.isEmpty()) return
        val maxOrder = categoryDao.getAllCategoriesList().maxOfOrNull { it.displayOrder } ?: 0
        categoryDao.insertCategories(
            missing.mapIndexed { index, seed ->
                CategoryEntity(
                    name = seed.name,
                    color = seed.colorHex,
                    isSystem = true,
                    isIncome = seed.isIncome,
                    displayOrder = maxOrder + index + 1
                )
            }
        )
    }
}

/**
 * [orderedIds] with [categoryId] shifted one place up or down, or null when it is
 * already at that end of the list — which is also what a missing id returns, so
 * a stale row from the UI can't reorder anything.
 *
 * Pure, and separated from the write, because the interesting part of a reorder
 * is this arithmetic rather than the UPDATE that follows it.
 */
internal fun reorderedSection(orderedIds: List<Long>, categoryId: Long, up: Boolean): List<Long>? {
    val from = orderedIds.indexOf(categoryId)
    if (from < 0) return null
    val to = if (up) from - 1 else from + 1
    if (to !in orderedIds.indices) return null
    return orderedIds.toMutableList().apply { add(to, removeAt(from)) }
}