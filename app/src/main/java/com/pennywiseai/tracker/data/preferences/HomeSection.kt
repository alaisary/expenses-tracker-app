package com.pennywiseai.tracker.data.preferences

import androidx.annotation.StringRes
import com.pennywiseai.tracker.R

/**
 * The toggleable / reorderable sections of the Home screen (#770). Declaration
 * order is the default display order. The fixed header (balance, share prompt,
 * cash-flow) is deliberately not here.
 */
enum class HomeSection(@StringRes val labelRes: Int) {
    BUDGETS(R.string.filter_home_budgets),
    LOANS(R.string.filter_home_loans),
    GROUPS(R.string.filter_home_groups),
    RECENT_TRANSACTIONS(R.string.filter_home_recent_transactions),
    ACCOUNTS(R.string.filter_home_accounts),
    SUBSCRIPTIONS(R.string.filter_home_subscriptions),
    ACTIVITY(R.string.filter_home_activity),
}

/**
 * Codec for the `home_sections` preference: comma-separated section names in
 * display order, a leading `!` marking a hidden one, e.g. `BUDGETS,!GROUPS,…`.
 * Pure so it is unit-testable without DataStore.
 */
object HomeSectionLayout {
    val DEFAULT: List<Pair<HomeSection, Boolean>> = HomeSection.entries.map { it to true }

    fun encode(layout: List<Pair<HomeSection, Boolean>>): String =
        layout.joinToString(",") { (section, visible) -> (if (visible) "" else "!") + section.name }

    /**
     * Unknown names are dropped; enum values absent from the string are appended
     * visible so a section added in a later version shows up instead of vanishing.
     */
    fun decode(raw: String?): List<Pair<HomeSection, Boolean>> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = raw.split(',').mapNotNull { token ->
            val t = token.trim()
            val hidden = t.startsWith("!")
            HomeSection.entries.firstOrNull { it.name == t.removePrefix("!") }?.let { it to !hidden }
        }.distinctBy { it.first }
        val seen = parsed.mapTo(HashSet()) { it.first }
        return parsed + HomeSection.entries.filter { it !in seen }.map { it to true }
    }
}
