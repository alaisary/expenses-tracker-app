package com.pennywiseai.tracker.ui.components

import androidx.annotation.StringRes
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.TransactionType

/**
 * The label for a stored [TransactionType] — the chip on a transaction's detail
 * screen, the type selector in the add sheet.
 *
 * The strings are the `filter_type_*` resources: those are the app's canonical
 * names for the five types (the rule editor already reads them outside the
 * filter bar), so every surface shows one translation per type instead of a
 * copy that can drift. The short variants are only for the narrow filter chips;
 * anywhere there is room, "Investment" is spelled out.
 */
@StringRes
fun TransactionType.typeLabelRes(): Int = when (this) {
    TransactionType.INCOME -> R.string.filter_type_income
    TransactionType.EXPENSE -> R.string.filter_type_expense
    TransactionType.CREDIT -> R.string.filter_type_credit
    TransactionType.TRANSFER -> R.string.filter_type_transfer
    TransactionType.INVESTMENT -> R.string.filter_type_investment
}
