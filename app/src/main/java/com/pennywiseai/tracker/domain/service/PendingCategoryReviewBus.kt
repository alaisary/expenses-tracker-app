package com.pennywiseai.tracker.domain.service

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * In-app event bus for transactions that were saved into the fallback
 * "Others" category because no keyword mapping, merchant mapping, or rule
 * claimed them (bank SMS never names a category — the merchant alone is often
 * ambiguous).
 *
 * The real-time ingest paths ([com.pennywiseai.tracker.data.manager.SmsTransactionProcessor.processAndSaveTransaction]
 * — the SMS receiver and the bank-notification listener) enqueue here; the
 * foreground UI (PennyWiseApp, via PendingCategoryReviewViewModel) pops the
 * shared QuickCategoryPickerSheet over whatever screen is open so the user can
 * pick a category in one tap.
 *
 * When the app is backgrounded at ingest time the event buffers in the
 * channel and the sheet resurfaces the next time the app is opened — the
 * transaction notification already carries quick-category actions meanwhile,
 * and a stale event is skipped if the transaction was already recategorized
 * from that notification.
 *
 * Bulk history scans (OptimizedSmsReaderWorker) intentionally never enqueue —
 * a re-scan would otherwise explode into a popup storm; they use the
 * processor's own save path, not processAndSaveTransaction.
 */
@Singleton
class PendingCategoryReviewBus @Inject constructor() {

    companion object {
        /** The fallback SharedCategoryMapping assigns when nothing matches. */
        const val UNCATEGORIZED = "Others"
    }

    private val channel = Channel<Long>(Channel.UNLIMITED)

    /** Transaction ids awaiting a category pick, in arrival order. */
    val events: Flow<Long> = channel.receiveAsFlow()

    fun enqueue(transactionId: Long) {
        channel.trySend(transactionId)
    }
}
