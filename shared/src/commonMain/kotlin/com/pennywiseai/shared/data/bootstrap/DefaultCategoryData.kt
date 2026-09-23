package com.pennywiseai.shared.data.bootstrap

object DefaultCategoryData {
    data class CategorySeed(val name: String, val colorHex: String, val isIncome: Boolean)

    val ALL: List<CategorySeed> = listOf(
        CategorySeed("Food & Dining",      "#FC8019", false),
        CategorySeed("Groceries",          "#5AC85A", false),
        CategorySeed("Transportation",     "#000000", false),
        CategorySeed("Shopping",           "#FF9900", false),
        CategorySeed("Bills & Utilities",  "#4CAF50", false),
        CategorySeed("Entertainment",      "#E50914", false),
        CategorySeed("Healthcare",         "#10847E", false),
        CategorySeed("Investments",        "#00D09C", false),
        CategorySeed("Banking",            "#004C8F", false),
        CategorySeed("Personal Care",      "#6A4C93", false),
        CategorySeed("Education",          "#673AB7", false),
        CategorySeed("Mobile",             "#2A3890", false),
        CategorySeed("Fitness",            "#FF3278", false),
        CategorySeed("Insurance",          "#0066CC", false),
        CategorySeed("Travel",             "#00BCD4", false),
        CategorySeed("Salary",             "#4CAF50", true),
        CategorySeed("Income",             "#4CAF50", true),
        CategorySeed("Others",             "#757575", false),

        // --- Local (GCC/Arabic-market) additions -----------------------------
        CategorySeed("Zakat & Sadaqah",         "#2E7D32", false),
        CategorySeed("Remittances",             "#00838F", false),
        CategorySeed("Gold & Jewellery",        "#FFB300", false),
        CategorySeed("Gifts & Eidiya",          "#E91E63", false),
        CategorySeed("Hospitality & Majlis",    "#8D6E63", false),
        CategorySeed("Domestic Help & Driver",  "#546E7A", false),
        CategorySeed("Government Fees & Fines", "#455A64", false),
        CategorySeed("Rent & Housing",          "#5D4037", false),
        CategorySeed("Loans & EMIs",            "#C62828", false),
        CategorySeed("Subscriptions",           "#7B1FA2", false),
        CategorySeed("Fuel",                    "#F57C00", false),

        // Income-side canonical names already produced by SharedCategoryMapping
        // but never seeded — now surfaced to existing and new users alike.
        CategorySeed("Refunds",                 "#4CAF50", true),
        CategorySeed("Cashback",                "#66BB6A", true),

        // Already mapped by SharedCategoryMapping; seeded here so the grouping +
        // detail surfaces can resolve them for existing installs.
        CategorySeed("Tax",                     "#795548", false),
        CategorySeed("Bank Charges",            "#9E9E9E", false),
        CategorySeed("Interest",                "#00ACC1", false),
        CategorySeed("Dividends",               "#26A69A", false)
    )
}
