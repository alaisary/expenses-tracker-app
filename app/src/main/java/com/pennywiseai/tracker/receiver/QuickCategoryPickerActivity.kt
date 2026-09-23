package com.pennywiseai.tracker.receiver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.TagRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.di.ApplicationScope
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerHost
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Translucent activity launched from the txn-alert notification's
 * "More…" action (#303). Hosts the shared [QuickCategoryPickerHost] so the
 * user can pick any category — not just the quick-picks rendered as inline
 * notification buttons — without navigating into the full transaction-detail
 * screen.
 *
 * Declared `singleTask` in the manifest so re-launches don't stack; that
 * means a second notification tap routes through [onNewIntent] rather than
 * a fresh [onCreate], which is why the picker args live in a mutableState
 * the Compose tree observes.
 *
 * On pick: update the transaction's category, dismiss the originating
 * notification, finish. On dismiss without picking: finish.
 */
@AndroidEntryPoint
class QuickCategoryPickerActivity : ComponentActivity() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var tagRepository: TagRepository

    // App-lifetime scope so the DB write survives finish() — the activity
    // closes immediately after the user picks.
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope


    // Holds the args from the latest intent (initial or via onNewIntent), so
    // a second notification tap re-targets the picker at the new transaction.
    private val currentArgs = mutableStateOf<PickerArgs?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.pennywiseai.tracker.utils.AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initial = PickerArgs.from(intent)
        if (initial == null) {
            finish()
            return
        }
        currentArgs.value = initial

        setContent {
            PennyWiseTheme {
                val args by currentArgs
                args?.let { PickerHost(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask routes second-tap intents here. Update the args so the
        // Compose tree re-keys produceState onto the new transactionId; the
        // bottom-sheet keeps its surface and just refreshes its content.
        setIntent(intent)
        PickerArgs.from(intent)?.let { currentArgs.value = it }
    }

    @Composable
    private fun PickerHost(args: PickerArgs) {
        QuickCategoryPickerHost(
            transactionId = args.transactionId,
            notificationId = args.notificationId,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            tagRepository = tagRepository,
            appScope = appScope,
            onFinished = { finish() }
        )
    }

    private data class PickerArgs(val transactionId: Long, val notificationId: Int) {
        companion object {
            fun from(intent: Intent?): PickerArgs? {
                intent ?: return null
                val txnId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
                if (txnId == -1L) return null
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                return PickerArgs(txnId, notifId)
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
