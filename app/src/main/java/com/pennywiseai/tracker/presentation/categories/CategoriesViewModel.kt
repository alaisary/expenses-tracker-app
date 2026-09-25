package com.pennywiseai.tracker.presentation.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.ui.icons.CategoryMoveState
import com.pennywiseai.tracker.ui.icons.groupedForDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import com.pennywiseai.tracker.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    // The hide/unhide write outlives this screen on purpose — see
    // [toggleCategoryHidden].
    @ApplicationScope private val applicationScope: CoroutineScope,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
    
    // Categories list
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Which rows can move one step — see [CategoryMoveState]. */
    val moveState: StateFlow<CategoryMoveState> = categories
        .map { CategoryMoveState.from(groupedForDisplay(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryMoveState.Empty
        )

    /**
     * Moves [category] one step within its section — the top-level rows between
     * its group header and the next. Sub-categories follow their parent, so they
     * have no move of their own.
     */
    fun moveCategory(category: CategoryEntity, up: Boolean) {
        if (category.parentId != null) return
        val section = groupedForDisplay(categories.value)
            .firstOrNull { (_, rows) -> rows.any { it.id == category.id } }
            ?.second
            ?.filter { it.parentId == null }
            ?.map { it.id }
            ?: return
        viewModelScope.launch {
            categoryRepository.moveCategoryWithinSection(section, category.id, up)
        }
    }
    
    // Dialog states
    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()
    
    private val _editingCategory = MutableStateFlow<CategoryEntity?>(null)
    val editingCategory: StateFlow<CategoryEntity?> = _editingCategory.asStateFlow()
    
    // Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    
    fun showAddDialog() {
        _editingCategory.value = null
        _showAddEditDialog.value = true
    }
    
    fun showEditDialog(category: CategoryEntity) {
        if (!category.isSystem) {
            _editingCategory.value = category
            _showAddEditDialog.value = true
        } else {
            _snackbarMessage.value = context.getString(R.string.vm_category_system_edit)
        }
    }
    
    fun hideDialog() {
        _showAddEditDialog.value = false
        _editingCategory.value = null
    }
    
    fun saveCategory(
        name: String,
        color: String,
        isIncome: Boolean,
        icon: String? = null,
        parentId: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val editingCat = _editingCategory.value
                
                if (editingCat != null) {
                    // Update existing category
                    categoryRepository.updateCategory(
                        editingCat.copy(
                            name = name,
                            color = color,
                            isIncome = isIncome,
                            icon = icon,
                            parentId = parentId
                        )
                    )
                    _snackbarMessage.value = context.getString(R.string.vm_category_updated)
                } else {
                    // Check if category already exists
                    if (categoryRepository.categoryExists(name)) {
                        _snackbarMessage.value = context.getString(R.string.vm_category_exists, name)
                        return@launch
                    }
                    
                    // Create new category
                    categoryRepository.createCategory(
                        name = name,
                        color = color,
                        isIncome = isIncome,
                        icon = icon,
                        parentId = parentId
                    )
                    _snackbarMessage.value = context.getString(R.string.vm_category_created)
                }
                
                hideDialog()
            } catch (e: Exception) {
                _snackbarMessage.value = context.getString(R.string.vm_category_save_error, e.message)
            }
        }
    }
    
    fun deleteCategory(category: CategoryEntity) {
        if (category.isSystem) {
            _snackbarMessage.value = context.getString(R.string.vm_category_system_delete)
            return
        }
        
        viewModelScope.launch {
            try {
                val deleted = categoryRepository.deleteCategory(category.id)
                if (deleted) {
                    _snackbarMessage.value = context.getString(R.string.vm_category_deleted)
                } else {
                    _snackbarMessage.value = context.getString(R.string.vm_category_cannot_delete)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = context.getString(R.string.vm_category_delete_error, e.message)
            }
        }
    }
    
    // One lock per category, so taps on the same row are applied one after
    // another instead of overlapping. Dropping a tap instead would lose it: two
    // quick taps have to land as two flips, or the row ends up in the opposite
    // state from what the user last asked for. Touched only from the main thread.
    private val toggleLocks = mutableMapOf<Long, Mutex>()

    /**
     * Hide or unhide a category (#736). Unlike delete, this is allowed for system
     * (default) categories — hiding is the safe way to tuck away an unused default:
     * the row stays in the DB so existing transactions keep their category, it's just
     * removed from the pickers.
     *
     * Takes only the id: the new value is worked out by the database, not by the
     * caller. The screen can only hold a snapshot of the row, and between a write
     * landing and the Room Flow reaching Compose that snapshot is stale — a
     * second tap in that window would ask for the same value again, so a quick
     * hide-then-show left the category hidden.
     *
     * Every tap flips the row exactly once. Taps on the same category queue
     * behind each other rather than being discarded, so two quick taps land as
     * two flips and the row ends up where the user's last tap asked for.
     *
     * Runs on the application scope, not [viewModelScope]: tapping twice and
     * immediately leaving the screen would otherwise clear the ViewModel and
     * cancel the queued second flip, persisting the opposite of what the user
     * last asked for.
     */
    fun toggleCategoryHidden(categoryId: Long) {
        val lock = toggleLocks.getOrPut(categoryId) { Mutex() }
        applicationScope.launch {
            lock.withLock {
                try {
                    val updated = categoryRepository.toggleCategoryHidden(categoryId)
                    if (updated != null) {
                        _snackbarMessage.value =
                            if (updated.isHidden) context.getString(R.string.vm_category_hidden, updated.name)
                            else context.getString(R.string.vm_category_shown, updated.name)
                    }
                } catch (e: Exception) {
                    _snackbarMessage.value = context.getString(R.string.vm_category_update_error, e.message)
                }
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)