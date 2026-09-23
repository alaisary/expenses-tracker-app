package com.pennywiseai.tracker.presentation.categories

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.repository.BudgetGroupRepository
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetGroupRepository: BudgetGroupRepository
) : ViewModel() {

    val categoryName: String = savedStateHandle.get<String>("categoryName") ?: ""

    private val period = MutableStateFlow(DetailPeriod.THIS_MONTH)
    private val selectedCurrencyOverride = MutableStateFlow<String?>(null)
    private val categoryColorHex = MutableStateFlow<String?>(null)

    private val rawTransactions = period.map { selected ->
        transactionRepository.getCategoryTransactionsSince(categoryName, selected.since())
    }

    val uiState: StateFlow<CategoryDetailUiState> = combine(
        categoryColorHex,
        period,
        rawTransactions,
        selectedCurrencyOverride
    ) { colorHex, selectedPeriod, raw, currencyOverride ->
        buildState(colorHex, selectedPeriod, raw, currencyOverride)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryDetailUiState(categoryName = categoryName)
    )

    init {
        viewModelScope.launch {
            categoryRepository.getCategoryByName(categoryName)?.let { category ->
                categoryColorHex.value = category.color
            }
        }
    }

    fun selectPeriod(newPeriod: DetailPeriod) {
        period.value = newPeriod
    }

    fun selectCurrency(currency: String) {
        selectedCurrencyOverride.value = currency
    }

    private suspend fun buildState(
        colorHex: String?,
        selectedPeriod: DetailPeriod,
        raw: List<TransactionEntity>,
        currencyOverride: String?
    ): CategoryDetailUiState {
        if (raw.isEmpty()) {
            return CategoryDetailUiState(
                categoryName = categoryName,
                categoryColorHex = colorHex,
                period = selectedPeriod,
                budget = loadBudget(selectedCurrency = null),
                isLoading = false
            )
        }

        val availableCurrencies = raw.map { it.currency }.distinct().sorted()
        val selectedCurrency = currencyOverride
            ?.takeIf { it in availableCurrencies }
            ?: dominantCurrency(raw)

        // Everything below is computed from a single currency's rows. Summing
        // across currencies is never valid here.
        val filtered = raw.filter { it.currency == selectedCurrency }

        val thisMonth = YearMonth.now()
        val trend = lastSixMonths(thisMonth).map { month ->
            MonthTotal(
                label = month.format(MONTH_LABEL),
                amount = filtered.filter { YearMonth.from(it.dateTime) == month }.categoryTotal()
            )
        }
        val nonEmptyMonths = trend.filter { it.amount > BigDecimal.ZERO }
        val monthlyAverage = if (nonEmptyMonths.isEmpty()) {
            BigDecimal.ZERO
        } else {
            nonEmptyMonths.fold(BigDecimal.ZERO) { acc, m -> acc + m.amount }
                .divide(BigDecimal(nonEmptyMonths.size), 2, RoundingMode.HALF_UP)
        }

        val topMerchants = filtered
            .groupBy { it.merchantName }
            .map { (name, rows) ->
                MerchantTotal(name = name, amount = rows.categoryTotal(), count = rows.size)
            }
            .sortedByDescending { it.amount }
            .take(5)

        return CategoryDetailUiState(
            categoryName = categoryName,
            categoryColorHex = colorHex,
            period = selectedPeriod,
            total = filtered.categoryTotal(),
            thisMonthTotal = filtered.filter { YearMonth.from(it.dateTime) == thisMonth }.categoryTotal(),
            monthlyAverage = monthlyAverage,
            transactionCount = filtered.size,
            monthlyTrend = trend,
            topMerchants = topMerchants,
            recentTransactions = filtered.take(30),
            availableCurrencies = availableCurrencies,
            selectedCurrency = selectedCurrency,
            budget = loadBudget(selectedCurrency),
            isLoading = false
        )
    }

    /** The currency with the most rows; ties break alphabetically by code. */
    private fun dominantCurrency(rows: List<TransactionEntity>): String =
        rows.groupingBy { it.currency }.eachCount().entries
            .minWithOrNull(
                compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
            )!!
            .key

    private fun lastSixMonths(current: YearMonth): List<YearMonth> =
        (5 downTo 0).map { current.minusMonths(it.toLong()) }

    private suspend fun loadBudget(selectedCurrency: String?): CategoryDetailBudget? {
        val budget = budgetGroupRepository.getActiveBudgetForCategory(categoryName) ?: return null
        val currency = selectedCurrency ?: budget.currency
        val windowEnd = budget.endDate.atTime(LocalTime.MAX)
        val spent = transactionRepository
            .getCategoryTransactionsSince(categoryName, budget.startDate.atStartOfDay())
            .filter { it.currency == currency && !it.dateTime.isAfter(windowEnd) }
            .categoryTotal()
        val remaining = budget.limitAmount - spent
        return CategoryDetailBudget(
            groupName = budget.name,
            limitAmount = budget.limitAmount,
            spent = spent,
            currency = currency,
            remaining = remaining,
            isOver = remaining < BigDecimal.ZERO
        )
    }

    /**
     * Gross activity in a category: every amount except TRANSFER (self-moves
     * aren't spending) and loan-linked rows (the app excludes loans from
     * category spend elsewhere). Works for both expense and income categories.
     */
    private fun List<TransactionEntity>.categoryTotal(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, tx ->
            if (tx.transactionType == TransactionType.TRANSFER || tx.loanId != null) acc
            else acc + tx.amount
        }

    private companion object {
        val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
    }
}

enum class DetailPeriod(@StringRes val labelRes: Int) {
    THIS_MONTH(R.string.cat_detail_period_this_month),
    DAYS_30(R.string.cat_detail_period_30d),
    MONTHS_3(R.string.cat_detail_period_3m),
    MONTHS_6(R.string.cat_detail_period_6m),
    YEAR(R.string.cat_detail_period_year),
    ALL(R.string.cat_detail_period_all);

    fun since(): LocalDateTime = when (this) {
        THIS_MONTH -> LocalDate.now().withDayOfMonth(1).atStartOfDay()
        DAYS_30 -> LocalDateTime.now().minusDays(30)
        MONTHS_3 -> LocalDateTime.now().minusMonths(3)
        MONTHS_6 -> LocalDateTime.now().minusMonths(6)
        YEAR -> LocalDateTime.now().minusYears(1)
        ALL -> LocalDate.of(1970, 1, 1).atStartOfDay()
    }
}

data class MonthTotal(
    val label: String,
    val amount: BigDecimal
)

data class MerchantTotal(
    val name: String,
    val amount: BigDecimal,
    val count: Int
)

data class CategoryDetailBudget(
    val groupName: String,
    val limitAmount: BigDecimal,
    val spent: BigDecimal,
    val currency: String,
    val remaining: BigDecimal,
    val isOver: Boolean
)

data class CategoryDetailUiState(
    val categoryName: String = "",
    val categoryColorHex: String? = null,
    val period: DetailPeriod = DetailPeriod.THIS_MONTH,
    val total: BigDecimal = BigDecimal.ZERO,
    val thisMonthTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyAverage: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0,
    val monthlyTrend: List<MonthTotal> = emptyList(),
    val topMerchants: List<MerchantTotal> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val availableCurrencies: List<String> = emptyList(),
    val selectedCurrency: String? = null,
    val budget: CategoryDetailBudget? = null,
    val isLoading: Boolean = true
)
