package com.pennywiseai.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.TagRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hosts the shared [QuickCategoryPickerSheet] for a transaction, loading its
 * current state (category, tags) and persisting the pick. Shared by:
 * - QuickCategoryPickerActivity — the txn-alert notification's "More…" action
 * - PennyWiseApp — the in-app popup for transactions that landed in the
 *   fallback "Others" category (PendingCategoryReviewBus)
 *
 * [notificationId] cancels the originating transaction notification when a
 * category is picked (`-1` = no notification to clear). [onFinished] is the
 * caller's dismissal: the activity finishes, the in-app host clears its state.
 * Finishes silently (via [onFinished]) when the transaction or categories
 * resolve to nothing renderable.
 */
@Composable
fun QuickCategoryPickerHost(
    transactionId: Long,
    notificationId: Int,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    appScope: CoroutineScope,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    // Explicit "loaded" flags so we can tell "still resolving" apart from
    // "resolved as null/empty" — important because the translucent picker
    // activity otherwise leaves a blank transparent window on screen when
    // data is missing.
    var transaction by remember(transactionId) { mutableStateOf<TransactionEntity?>(null) }
    var transactionLoaded by remember(transactionId) { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var categoriesLoaded by remember { mutableStateOf(false) }
    // Tags for the inline tag section (#696): the txn's current tags seed
    // the field, and every known tag name feeds autocomplete.
    var initialTags by remember(transactionId) { mutableStateOf<List<String>>(emptyList()) }
    var tagsLoaded by remember(transactionId) { mutableStateOf(false) }
    var tagSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(transactionId) {
        transaction = transactionRepository.getTransactionById(transactionId)
        transactionLoaded = true
        // Load existing tags BEFORE the sheet renders (gated by tagsLoaded below).
        // Otherwise the sheet's remember{} captures an empty list, and the first
        // add/remove would replace-all with an incomplete set, deleting existing
        // tag associations. (#710 Greptile)
        initialTags = tagRepository.getTagNamesForTransaction(transactionId)
        tagsLoaded = true
    }
    LaunchedEffect(Unit) {
        categories = categoryRepository.getVisibleCategories().first()
        categoriesLoaded = true
        tagSuggestions = tagRepository.observeAllTagNames().first()
    }

    // Nothing renderable → finish silently.
    LaunchedEffect(transactionLoaded, categoriesLoaded) {
        if (transactionLoaded && categoriesLoaded &&
            (transaction == null || categories.isEmpty())
        ) {
            onFinished()
        }
    }

    val txn = transaction
    if (txn != null && categories.isNotEmpty() && tagsLoaded) {
        QuickCategoryPickerSheet(
            currentCategory = txn.category,
            categories = categories,
            onCategorySelected = { newCategory ->
                if (newCategory != txn.category) {
                    // App-lifetime scope so the DB write survives the host
                    // disappearing — the activity finish()es right after a pick.
                    appScope.launch {
                        transactionRepository.updateCategory(txn.id, newCategory)
                    }
                }
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context.applicationContext)
                        .cancel(notificationId)
                }
                onFinished()
            },
            onDismiss = { onFinished() },
            tagSuggestions = tagSuggestions,
            initialTags = initialTags,
            onTagsChanged = { newTags ->
                // Persist immediately so tagging works even if the user
                // dismisses without picking a category (#696). Enqueue rather
                // than write directly so rapid edits apply FIFO on a single
                // consumer — no reorder, and no loss when the picker retargets
                // to another transaction. (#710)
                tagRepository.enqueueSetTags(txn.id, newTags)
            }
        )
    }
}
