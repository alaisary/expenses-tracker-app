package com.pennywiseai.tracker.ui.icons

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.pennywiseai.tracker.R

/**
 * High-level bucket a category rolls into for the grouped Categories screen and
 * the Analytics "by group" totals.
 *
 * The grouping is a *static-code* mapping (v1): no DB column, no migration, no
 * backup change. Unknown / user-created categories fall back to [OTHER], and the
 * transfer-like buckets ([EXCLUDED]) are kept out of group totals because moving
 * money between your own accounts is not spending.
 */
enum class CategoryGroup(
    @StringRes val labelRes: Int,
    val accentColor: Color
) {
    NEEDS(R.string.group_needs, Color(0xFF1565C0)),
    WANTS(R.string.group_wants, Color(0xFFFF9900)),
    SAVINGS(R.string.group_savings, Color(0xFF00D09C)),
    INCOME(R.string.group_income, Color(0xFF4CAF50)),
    EXCLUDED(R.string.group_excluded, Color(0xFF9E9E9E)),
    OTHER(R.string.group_other, Color(0xFF757575));

    /** Groups the Categories screen renders as sections, in display order. */
    companion object {
        val DISPLAY_ORDER: List<CategoryGroup> =
            listOf(NEEDS, WANTS, SAVINGS, INCOME, OTHER)
    }
}

/**
 * Canonical category name (trimmed, case-insensitive) → group. Keys include both
 * the local additions and the older canonical aliases that
 * [com.pennywiseai.shared.domain.mapping.SharedCategoryMapping] can still emit
 * (e.g. "Housing" and "EMI").
 */
private val GROUP_BY_NAME: Map<String, CategoryGroup> = buildMap {
    fun group(group: CategoryGroup, vararg names: String) {
        names.forEach { put(it.lowercase(), group) }
    }

    group(
        CategoryGroup.NEEDS,
        "Groceries", "Bills & Utilities", "Healthcare", "Insurance", "Education",
        "Mobile", "Transportation", "Fuel", "Rent & Housing", "Housing",
        "Government Fees & Fines", "Domestic Help & Driver", "Loans & EMIs", "EMI",
        "Zakat & Sadaqah", "Tax", "Remittances"
    )
    group(
        CategoryGroup.WANTS,
        "Food & Dining", "Food", "Shopping", "Entertainment", "Travel", "Fitness",
        "Personal Care", "Gifts & Eidiya", "Hospitality & Majlis", "Subscriptions",
        "Bank Charges"
    )
    group(CategoryGroup.SAVINGS, "Investments", "Gold & Jewellery", "Interest", "Dividends")
    group(CategoryGroup.INCOME, "Salary", "Income", "Cashback", "Refunds")
    group(CategoryGroup.EXCLUDED, "Transfer", "Credit Card Payment")
}

/** Resolves [name] to its [CategoryGroup]; unknown / user categories are [CategoryGroup.OTHER]. */
fun groupOf(name: String?): CategoryGroup {
    val key = name?.trim()?.lowercase() ?: return CategoryGroup.OTHER
    if (key.isEmpty()) return CategoryGroup.OTHER
    return GROUP_BY_NAME[key] ?: CategoryGroup.OTHER
}
