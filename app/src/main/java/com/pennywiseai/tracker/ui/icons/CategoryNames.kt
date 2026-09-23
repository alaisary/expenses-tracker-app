package com.pennywiseai.tracker.ui.icons

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R

/**
 * Display-time translation for the built-in (default) category names.
 *
 * Category names are canonical English values: they are the stored value, the
 * budget/rule match key, and what repositories compare against. Translating
 * them at the source would corrupt matching, so this map only changes how a
 * name is *rendered*. Unknown / user-created categories pass through unchanged.
 *
 * Keys are trimmed + lowercased so lookups are case-insensitive and tolerate
 * stray whitespace.
 */
private val CATEGORY_NAME_RES: Map<String, Int> = mapOf(
    "food & dining" to R.string.cat_food_dining,
    "groceries" to R.string.cat_groceries,
    "transportation" to R.string.cat_transportation,
    "shopping" to R.string.cat_shopping,
    "bills & utilities" to R.string.cat_bills_utilities,
    "entertainment" to R.string.cat_entertainment,
    "healthcare" to R.string.cat_healthcare,
    "investments" to R.string.cat_investments,
    "banking" to R.string.cat_banking,
    "personal care" to R.string.cat_personal_care,
    "education" to R.string.cat_education,
    "mobile" to R.string.cat_mobile,
    "mobile & recharge" to R.string.cat_mobile_recharge,
    "fitness" to R.string.cat_fitness,
    "insurance" to R.string.cat_insurance,
    "travel" to R.string.cat_travel,
    "salary" to R.string.cat_salary,
    "income" to R.string.cat_income,
    "others" to R.string.cat_others,
    // Canonical names produced by SharedCategoryMapping / CategoryMapping.
    "food" to R.string.cat_food,
    "cashback" to R.string.cat_cashback,
    "housing" to R.string.cat_housing,
    "emi" to R.string.cat_emi,
    "transfer" to R.string.cat_transfer,
    "tax" to R.string.cat_tax,
    "bank charges" to R.string.cat_bank_charges,
    "credit card payment" to R.string.cat_credit_card_payment,
    "refunds" to R.string.cat_refunds,
    "interest" to R.string.cat_interest,
    "dividends" to R.string.cat_dividends,
    "subscriptions" to R.string.cat_subscriptions,
    // Local (GCC/Arabic-market) categories added in the grouping release.
    "zakat & sadaqah" to R.string.cat_zakat_sadaqah,
    "remittances" to R.string.cat_remittances,
    "gold & jewellery" to R.string.cat_gold_jewellery,
    "gifts & eidiya" to R.string.cat_gifts_eidiya,
    "hospitality & majlis" to R.string.cat_hospitality_majlis,
    "domestic help & driver" to R.string.cat_domestic_help,
    "government fees & fines" to R.string.cat_government_fees,
    "rent & housing" to R.string.cat_rent_housing,
    "loans & emis" to R.string.cat_loans_emis,
    "fuel" to R.string.cat_fuel
)

/**
 * Returns the string resource for a known default category name (matched
 * case-insensitively after trimming), or `null` for custom/unknown names.
 */
@StringRes
fun categoryNameRes(name: String?): Int? {
    val key = name?.trim()?.lowercase() ?: return null
    if (key.isEmpty()) return null
    return CATEGORY_NAME_RES[key]
}

/**
 * Non-Compose variant for call sites that have a [Context] but no Compose
 * scope (notifications, widgets, workers). Returns the localized default or the
 * original name unchanged.
 */
fun localizedCategoryName(context: Context, name: String?): String {
    val res = categoryNameRes(name) ?: return name ?: ""
    return context.getString(res)
}

/**
 * Compose variant: returns the localized default or the original name
 * unchanged. Use at any `Text` site that renders a category name.
 */
@Composable
fun localizedCategoryName(name: String?): String {
    val res = categoryNameRes(name) ?: return name ?: ""
    return stringResource(res)
}
