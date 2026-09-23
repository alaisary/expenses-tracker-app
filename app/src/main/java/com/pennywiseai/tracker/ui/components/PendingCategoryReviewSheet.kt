package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.ui.icons.localizedCategoryName
import com.pennywiseai.tracker.utils.formatAmount
import java.time.format.DateTimeFormatter

private val REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")

private fun typeEmoji(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "💸"
    TransactionType.INCOME -> "💰"
    TransactionType.CREDIT -> "💳"
    TransactionType.TRANSFER -> "🔄"
    TransactionType.INVESTMENT -> "📈"
    else -> "💵"
}

/**
 * Batch review sheet for transactions that were auto-saved into the fallback
 * "Others" category (PendingCategoryReviewBus). Shows one row per pending
 * transaction — merchant, bank, date, and the currency-tagged amount — and
 * hands each row to the full [QuickCategoryPickerHost] via [onPick].
 *
 * Rows are per-currency formatted with the entity extension (never summed —
 * the batch may mix currencies). "Later" (and a swipe dismissal) defers the
 * whole batch for this session: the transactions stay "Others" in the list,
 * where swipe-to-categorize still works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingCategoryReviewSheet(
    transactionIds: List<Long>,
    transactionRepository: TransactionRepository,
    onPick: (Long) -> Unit,
    onLater: () -> Unit
) {
    var transactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(transactionIds) {
        transactions = transactionIds.mapNotNull { id ->
            transactionRepository.getTransactionById(id)
        }
        loaded = true
    }

    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.comp_transactions_need_category, transactionIds.size),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.comp_pending_review_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        if (loaded && transactions.isEmpty()) {
            // Everything already resolved elsewhere (e.g. notification picks) —
            // nothing left to show.
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.comp_nothing_to_review),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(transactions, key = { it.id }) { txn ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(txn.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = typeEmoji(txn.transactionType),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = txn.merchantName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${txn.bankName ?: stringResource(R.string.comp_bank)} • ${txn.dateTime.format(REVIEW_DATE_FORMAT)} • ${localizedCategoryName(txn.category)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(
                            text = txn.formatAmount(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.comp_later))
            }
        }
    }
}
