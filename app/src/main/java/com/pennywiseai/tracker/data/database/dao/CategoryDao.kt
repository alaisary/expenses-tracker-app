package com.pennywiseai.tracker.data.database.dao

import androidx.room.*
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY display_order ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_income = 0 ORDER BY display_order ASC, name ASC")
    fun getExpenseCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_income = 1 ORDER BY display_order ASC, name ASC")
    fun getIncomeCategories(): Flow<List<CategoryEntity>>

    // Visible-only variants for the category PICKERS — hidden categories are kept
    // in the DB (so existing transactions keep their category and still show in
    // analytics) but excluded from selection (#736).
    @Query("SELECT * FROM categories WHERE is_hidden = 0 ORDER BY display_order ASC, name ASC")
    fun getVisibleCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_income = 0 AND is_hidden = 0 ORDER BY display_order ASC, name ASC")
    fun getVisibleExpenseCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_income = 1 AND is_hidden = 0 ORDER BY display_order ASC, name ASC")
    fun getVisibleIncomeCategories(): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET is_hidden = :hidden WHERE id = :categoryId")
    suspend fun setCategoryHidden(categoryId: Long, hidden: Boolean)

    /**
     * Flips the flag in the database rather than writing a value the caller
     * worked out beforehand.
     *
     * The UI can only ever hold a snapshot of the row: between a write landing
     * and the Room Flow reaching Compose, a second tap would compute its "next"
     * value from the pre-write state and write the same thing again, so a
     * quick hide-then-show left the category hidden. Flipping in SQL has no such
     * window.
     */
    @Query("UPDATE categories SET is_hidden = NOT is_hidden WHERE id = :categoryId")
    suspend fun toggleCategoryHidden(categoryId: Long)

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY display_order ASC, name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("UPDATE categories SET display_order = :displayOrder, updated_at = :updatedAt WHERE id = :categoryId")
    suspend fun setDisplayOrder(categoryId: Long, displayOrder: Int, updatedAt: LocalDateTime)

    /**
     * Rewrites a section's top-level order in one transaction.
     *
     * A move is one step, but what gets stored is the section's whole sequence:
     * every user-created category seeds `display_order` 999, so exchanging the
     * two values of a move would write the same number twice and nothing would
     * move. Sub-categories are not in [orderedIds] — they render under their
     * parent, so their own value is not what positions them.
     */
    @Transaction
    suspend fun setSectionDisplayOrder(orderedIds: List<Long>) {
        val now = LocalDateTime.now()
        orderedIds.forEachIndexed { index, id -> setDisplayOrder(id, index, now) }
    }

    @Query("UPDATE categories SET is_hidden = :hidden WHERE parent_id = :parentId")
    suspend fun setChildrenHidden(parentId: Long, hidden: Boolean)

    /**
     * Flips a category's hidden flag and cascades in one transaction (#374):
     * a hidden parent hides its children; un-hiding a child restores its
     * parent — so the hierarchy can never be committed half-way.
     */
    @Transaction
    suspend fun toggleCategoryHiddenCascading(categoryId: Long): CategoryEntity? {
        toggleCategoryHidden(categoryId)
        val updated = getCategoryById(categoryId) ?: return null
        setChildrenHidden(categoryId, updated.isHidden)
        if (!updated.isHidden) updated.parentId?.let { setCategoryHidden(it, false) }
        return updated
    }

    /** Deleting a parent promotes its children to top level (#374). */
    @Query("UPDATE categories SET parent_id = NULL WHERE parent_id = :parentId")
    suspend fun detachChildren(parentId: Long)
    
    @Query("SELECT * FROM categories WHERE name = :categoryName LIMIT 1")
    suspend fun getCategoryByName(categoryName: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :categoryId AND is_system = 0")
    suspend fun deleteCategory(categoryId: Long)
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :categoryName)")
    suspend fun categoryExists(categoryName: String): Boolean
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}