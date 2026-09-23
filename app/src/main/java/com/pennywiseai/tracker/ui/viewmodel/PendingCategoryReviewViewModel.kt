package com.pennywiseai.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.TagRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.di.ApplicationScope
import com.pennywiseai.tracker.domain.service.PendingCategoryReviewBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the in-app category prompt for transactions that were saved into the
 * fallback "Others" category (see [PendingCategoryReviewBus]).
 *
 * Exposes the queue of pending transaction ids; PennyWiseApp renders either the
 * shared QuickCategoryPickerSheet (a single pending transaction) or the batch
 * PendingCategoryReviewSheet (several) over the current screen. Events that
 * arrive while a prompt is already up queue behind it and surface as the list
 * shrinks — or on the next app open.
 *
 * Stale-event guard: if the transaction was already recategorized elsewhere
 * (e.g. via the notification's quick actions) or no longer exists, the event
 * is skipped instead of prompting.
 */
@HiltViewModel
class PendingCategoryReviewViewModel @Inject constructor(
    pendingCategoryReviewBus: PendingCategoryReviewBus,
    val transactionRepository: TransactionRepository,
    val categoryRepository: CategoryRepository,
    val tagRepository: TagRepository,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private val _pendingIds = MutableStateFlow<List<Long>>(emptyList())
    val pendingIds: StateFlow<List<Long>> = _pendingIds.asStateFlow()

    init {
        viewModelScope.launch {
            pendingCategoryReviewBus.events.collect { transactionId ->
                val transaction = transactionRepository.getTransactionById(transactionId)
                if (transaction != null &&
                    transaction.category == PendingCategoryReviewBus.UNCATEGORIZED &&
                    transactionId !in _pendingIds.value
                ) {
                    _pendingIds.value = _pendingIds.value + transactionId
                }
            }
        }
    }

    /** Called by the UI after a transaction is picked or dismissed. */
    fun removeFromQueue(transactionId: Long) {
        _pendingIds.value = _pendingIds.value - transactionId
    }

    /** Called by the UI when the whole batch is deferred ("Later"). */
    fun clearQueue() {
        _pendingIds.value = emptyList()
    }
}
