package com.pennywiseai.tracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter

@StringRes
fun TransactionTypeFilter.shortLabelRes(): Int = when (this) {
    TransactionTypeFilter.ALL -> R.string.filter_type_all
    TransactionTypeFilter.INCOME -> R.string.filter_type_income
    TransactionTypeFilter.EXPENSE -> R.string.filter_type_expense
    TransactionTypeFilter.CREDIT -> R.string.filter_type_credit
    TransactionTypeFilter.TRANSFER -> R.string.filter_type_transfer
    TransactionTypeFilter.INVESTMENT -> R.string.filter_type_short_invest
}

fun TransactionTypeFilter.filterIcon(): ImageVector = when (this) {
    TransactionTypeFilter.ALL -> Icons.AutoMirrored.Filled.ReceiptLong
    TransactionTypeFilter.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionTypeFilter.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionTypeFilter.CREDIT -> Icons.Default.CreditCard
    TransactionTypeFilter.TRANSFER -> Icons.Default.SwapHoriz
    TransactionTypeFilter.INVESTMENT -> Icons.AutoMirrored.Filled.ShowChart
}
