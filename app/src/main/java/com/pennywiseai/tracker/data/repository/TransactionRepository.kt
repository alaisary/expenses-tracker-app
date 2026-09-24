package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.dao.TransactionSplitDao
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionSplitEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.database.entity.TransactionWithSplits
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.manager.TransactionDeduplication
import com.pennywiseai.tracker.domain.model.BudgetCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
open class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionSplitDao: TransactionSplitDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private companion object {
        const val PAIRING_WINDOW_MINUTES = 10L
    }

    fun getAllTransactions(): Flow<List<TransactionEntity>> = 
        transactionDao.getAllTransactions()
    
    open suspend fun getTransactionById(id: Long): TransactionEntity? = 
        transactionDao.getTransactionById(id)
    
    fun getTransactionsBetweenDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsBetweenDates(startDate, endDate)
    
    fun getTransactionsBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        )

    /**
     * Gets transactions filtered at the database level for better performance.
     * Combines date range, currency, and transaction type filters to reduce memory usage.
     *
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @param currency Currency code to filter by (e.g., "INR", "USD")
     * @param transactionType Optional transaction type filter (null means all types)
     * @return Flow of filtered transactions
     */
    fun getTransactionsFiltered(
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String,
        transactionType: TransactionType? = null
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsFiltered(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59),
            currency,
            transactionType
        )
    
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByType(type)
    
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByCategory(category)

    /** See [TransactionDao.getCategoryTransactionsSince]. */
    suspend fun getCategoryTransactionsSince(
        category: String,
        since: LocalDateTime
    ): List<TransactionEntity> =
        transactionDao.getCategoryTransactionsSince(category, since)
    
    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    fun getAllCurrencies(): Flow<List<String>> =
        transactionDao.getAllCurrencies()

    fun getCurrenciesForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<String>> =
        transactionDao.getCurrenciesForPeriod(startDate, endDate)
    
    fun getAllCategories(): Flow<List<String>> =
        transactionDao.getAllCategories()

    /**
     * Gets the top N categories by usage count (number of transactions).
     * Useful for showing user's most frequently used categories in notifications.
     *
     * @param limit Maximum number of categories to return (default: 3)
     * @return List of category names ordered by usage count (most used first)
     */
    suspend fun getTopCategoriesByUsage(limit: Int = 3): List<String> =
        transactionDao.getTopCategoriesByUsage(limit)

    fun getAllMerchants(): Flow<List<String>> =
        transactionDao.getAllMerchants()
    
    suspend fun getTotalAmountByTypeAndPeriod(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double? = transactionDao.getTotalAmountByTypeAndPeriod(type, startDate, endDate)
    
    suspend fun insertTransaction(transaction: TransactionEntity): Long = 
        transactionDao.insertTransaction(transaction)
    
    suspend fun insertTransactions(transactions: List<TransactionEntity>) = 
        transactionDao.insertTransactions(transactions)
    
    suspend fun updateTransaction(transaction: TransactionEntity) = 
        transactionDao.updateTransaction(transaction)
    
    open suspend fun deleteTransaction(transaction: TransactionEntity, hardDelete: Boolean = false) {
        if (hardDelete) {
            transactionDao.deleteTransaction(transaction)
        } else {
            transactionDao.softDeleteTransaction(transaction.id)
        }
    }

    suspend fun deleteTransactionById(id: Long, hardDelete: Boolean = false) {
        if (hardDelete) {
            transactionDao.deleteTransactionById(id)
        } else {
            transactionDao.softDeleteTransaction(id)
        }
    }

    /** @return the number of rows actually removed. */
    suspend fun deleteAllTransactions(): Int =
        transactionDao.deleteAllTransactions()

    /** See [TransactionDao.observeAllTransactionCount]. */
    fun observeAllTransactionCount(): Flow<Int> =
        transactionDao.observeAllTransactionCount()

    /** See [TransactionDao.countAllTransactions]. */
    suspend fun countAllTransactions(): Int =
        transactionDao.countAllTransactions()

    /** See [TransactionDao.deleteUncuratedTransactions]. */
    suspend fun deleteUncuratedTransactions() =
        transactionDao.deleteUncuratedTransactions()

    // Helper method to check if transaction exists by hash
    suspend fun getTransactionByHash(transactionHash: String): TransactionEntity? =
        transactionDao.getTransactionByHash(transactionHash)

    // ─── Internal-transfer pairing ───────────────────────────────────────────
    // A merchantless debit on one account and an equal credit on another of
    // the same bank minutes apart are the two SMS halves of an internal
    // transfer. Left as EXPENSE + INCOME they inflate both totals (e.g. moving
    // 100 OMR between own Alizz accounts looked like 100 OMR of spending).
    // Pairing converts both rows to TRANSFER (excluded from spending/income
    // analytics) with from/to accounts recorded.

    /**
     * Attempts to pair a just-saved merchantless transaction with its
     * counterpart (opposite type, same bank/amount/currency, different
     * account, within [PAIRING_WINDOW_MINUTES]). Returns true when paired —
     * both rows become TRANSFER.
     */
    open suspend fun pairInternalTransferIfMatch(entity: TransactionEntity, rowId: Long): Boolean {
        val type = entity.transactionType
        if (type != TransactionType.EXPENSE && type != TransactionType.INCOME) return false
        val account = entity.accountNumber ?: return false
        val merchant = entity.merchantName
        // Only merchantless alerts pair — real merchant spends and payer-named
        // credits have their own identity. ("your account …" rows are the
        // legacy account-leak merchant, i.e. merchantless in disguise.)
        if (!merchant.isNullOrBlank() && !merchant.startsWith("your account")) return false

        val counterpart = transactionDao.findInternalTransferCounterpart(
            bankName = entity.bankName ?: return false,
            amount = entity.amount.toString(),
            currency = entity.currency,
            oppositeType = if (type == TransactionType.EXPENSE) TransactionType.INCOME else TransactionType.EXPENSE,
            accountNumber = account,
            from = entity.dateTime.minusMinutes(PAIRING_WINDOW_MINUTES),
            to = entity.dateTime.plusMinutes(PAIRING_WINDOW_MINUTES)
        ) ?: return false

        val fromAccount: String
        val toAccount: String
        if (type == TransactionType.EXPENSE) {
            fromAccount = account
            toAccount = counterpart.accountNumber ?: return false
        } else {
            fromAccount = counterpart.accountNumber ?: return false
            toAccount = account
        }
        val debitId = if (type == TransactionType.EXPENSE) rowId else counterpart.id
        val creditId = if (type == TransactionType.EXPENSE) counterpart.id else rowId
        transactionDao.markInternalTransfers(
            ids = listOf(debitId, creditId),
            fromAccount = fromAccount,
            toAccount = toAccount,
            updatedAt = LocalDateTime.now()
        )
        return true
    }

    /**
     * Retroactive pass over all un-paired merchantless candidates — repairs
     * history already in the DB (a re-scan dedup-skips old rows, so
     * [pairInternalTransferIfMatch] never sees them again). One-to-one greedy
     * matching within (bank, currency, amount) groups, nearest in time. Runs
     * at the end of every bulk scan; idempotent (paired rows drop out of the
     * candidate query). @return the number of pairs made.
     */
    open suspend fun pairHistoricInternalTransfers(): Int {
        val candidates = transactionDao.getInternalTransferCandidates()
        if (candidates.isEmpty()) return 0

        data class Key(val bank: String, val currency: String, val amount: Double)
        val groups = HashMap<Key, MutableList<TransactionEntity>>()
        for (c in candidates) {
            val bank = c.bankName ?: continue
            groups.getOrPut(Key(bank, c.currency, c.amount.toDouble())) { mutableListOf() }.add(c)
        }

        val now = LocalDateTime.now()
        var pairs = 0
        for ((_, rows) in groups) {
            val debits = rows.filter { it.transactionType == TransactionType.EXPENSE }.sortedBy { it.dateTime }
            val credits = rows.filter { it.transactionType == TransactionType.INCOME }.sortedBy { it.dateTime }
            if (debits.isEmpty() || credits.isEmpty()) continue

            val used = BooleanArray(credits.size)
            var ci = 0
            for (debit in debits) {
                // Credits sorted by time: anything older than (debit - window)
                // can never match this or any later debit.
                while (ci < credits.size &&
                    credits[ci].dateTime < debit.dateTime.minusMinutes(PAIRING_WINDOW_MINUTES)
                ) ci++
                var j = ci
                while (j < credits.size) {
                    val credit = credits[j]
                    if (credit.dateTime > debit.dateTime.plusMinutes(PAIRING_WINDOW_MINUTES)) break
                    if (!used[j] && credit.accountNumber != debit.accountNumber) {
                        used[j] = true
                        transactionDao.markInternalTransfers(
                            ids = listOf(debit.id, credit.id),
                            fromAccount = debit.accountNumber ?: break,
                            toAccount = credit.accountNumber ?: break,
                            updatedAt = now
                        )
                        pairs++
                        break
                    }
                    j++
                }
            }
        }
        return pairs
    }

    /** A soft-deleted transaction from the same raw SMS, if any (#703). */
    suspend fun getDeletedBySms(smsBody: String, smsSender: String?): TransactionEntity? =
        transactionDao.getDeletedBySms(smsBody, smsSender)

    /**
     * The subset of [hashes] already present. Chunked under SQLite's 999
     * bind-variable limit (Room does not auto-chunk `IN (:list)`), so a large
     * CSV import can't crash with "too many SQL variables".
     */
    suspend fun getExistingHashes(hashes: List<String>): List<String> =
        hashes.chunked(900).flatMap { transactionDao.getExistingHashes(it) }

    /** See [TransactionDao.findRecentExpensesByMerchantAndAmount]. */
    suspend fun findRecentExpensesByMerchantAndAmount(
        merchant: String,
        amount: java.math.BigDecimal,
        since: java.time.LocalDateTime,
        limit: Int = 5,
    ): List<TransactionEntity> =
        transactionDao.findRecentExpensesByMerchantAndAmount(merchant, amount, since, limit)

    suspend fun getTransactionByReference(reference: String): TransactionEntity? =
        transactionDao.getTransactionByReference(reference)

    suspend fun findPotentialDuplicates(transaction: TransactionEntity): List<TransactionEntity> {
        if (!TransactionDeduplication.hasUpiReference(transaction)) return emptyList()

        return transactionDao.findPotentialDuplicatesByReference(
            reference = transaction.reference.orEmpty(),
            amount = transaction.amount,
            transactionType = transaction.transactionType,
            currency = transaction.currency,
            accountNumber = transaction.accountNumber,
            startDate = transaction.dateTime.minus(TransactionDeduplication.UPI_DUPLICATE_WINDOW),
            endDate = transaction.dateTime.plus(TransactionDeduplication.UPI_DUPLICATE_WINDOW)
        ).filter { candidate ->
            candidate.id != transaction.id &&
                    TransactionDeduplication.isSameUpiTransaction(candidate, transaction)
        }
    }

    suspend fun getTransactionByAmountAndDate(
        amount: BigDecimal,
        dateStart: LocalDateTime,
        dateEnd: LocalDateTime
    ): List<TransactionEntity> =
        transactionDao.getTransactionByAmountAndDate(amount, dateStart, dateEnd)

    suspend fun findGPayDuplicateIdsForCleanup(): List<Long> {
        val candidates = transactionDao.findPotentialDuplicatesByReference()
        return TransactionDeduplication.duplicateIdsToDelete(candidates)
    }

    open suspend fun undoDeleteTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction.copy(isDeleted = false))
    }
    
    suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String) {
        transactionDao.updateCategoryForMerchant(merchantName, newCategory)
    }

    suspend fun updateCategory(transactionId: Long, category: String) {
        transactionDao.updateCategoryById(transactionId, category, LocalDateTime.now())
    }
    
    suspend fun getOtherTransactionCountForMerchant(merchantName: String, excludeId: Long): Int {
        return transactionDao.getTransactionCountForMerchant(merchantName, excludeId)
    }

    suspend fun countExplicitProfileMismatchForAccount(
        bankName: String,
        accountLast4: String,
        profileId: Long
    ): Int {
        return transactionDao.countExplicitProfileMismatchForAccount(bankName, accountLast4, profileId)
    }

    suspend fun setProfileForAccountTransactions(
        bankName: String,
        accountLast4: String,
        profileId: Long
    ): Int {
        return transactionDao.setProfileForAccountTransactions(
            bankName,
            accountLast4,
            profileId,
            LocalDateTime.now()
        )
    }
    
    // Additional methods for Home screen
    data class MonthlyBreakdown(
        val total: BigDecimal,
        val income: BigDecimal,
        val expenses: BigDecimal
    )
    
    fun getCurrentMonthBreakdown(): Flow<MonthlyBreakdown> {
        return getTransactionsForCurrentMonth()
            .map { transactions ->
                transactions.toMonthlyBreakdown()
            }
    }
    
    fun getCurrentMonthTotal(): Flow<BigDecimal> {
        return getCurrentMonthBreakdown().map { it.total }
    }
    
    fun getLastMonthBreakdown(): Flow<MonthlyBreakdown> {
        return getTransactionsForComparableLastMonth()
            .map { transactions ->
                transactions.toMonthlyBreakdown()
            }
    }
    
    fun getLastMonthTotal(): Flow<BigDecimal> {
        return getLastMonthBreakdown().map { it.total }
    }

    // Currency-grouped breakdown methods
    fun getCurrentMonthBreakdownByCurrency(): Flow<Map<String, MonthlyBreakdown>> {
        return getTransactionsForCurrentMonth()
            .map { transactions ->
                transactions.toMonthlyBreakdownByCurrency()
            }
    }

    fun getLastMonthBreakdownByCurrency(): Flow<Map<String, MonthlyBreakdown>> {
        return getTransactionsForComparableLastMonth()
            .map { transactions ->
                transactions.toMonthlyBreakdownByCurrency()
            }
    }

    private fun getTransactionsForCurrentMonth(): Flow<List<TransactionEntity>> {
        val now = LocalDate.now()
        return userPreferencesRepository.budgetCycleStartDay.flatMapLatest { startDay ->
            val (cycleStart, cycleEnd) = BudgetCycle.currentCycle(now, startDay)
            val startDate = cycleStart.atStartOfDay()
            val endDate = cycleEnd.atTime(LocalTime.MAX)
            transactionDao.getTransactionsBetweenDates(startDate, endDate)
                // Monthly spending summary ignores analytics-excluded transactions (#451).
                .map { txns -> txns.filter { !it.excludedFromAnalytics } }
        }
    }

    private fun getTransactionsForComparableLastMonth(): Flow<List<TransactionEntity>> {
        val now = LocalDate.now()
        return userPreferencesRepository.budgetCycleStartDay.flatMapLatest { startDay ->
            val current = BudgetCycle.currentCycle(now, startDay)
            val (prevStart, prevEnd) = BudgetCycle.previousCycle(current, startDay)
            val startDate = prevStart.atStartOfDay()
            val endDate = prevEnd.atTime(LocalTime.MAX)
            transactionDao.getTransactionsBetweenDates(startDate, endDate)
                // Monthly spending summary ignores analytics-excluded transactions (#451).
                .map { txns -> txns.filter { !it.excludedFromAnalytics } }
        }
    }

    private fun List<TransactionEntity>.toMonthlyBreakdown(): MonthlyBreakdown {
        val income = sumTransactionType(TransactionType.INCOME)
        val expenses = sumTransactionType(TransactionType.EXPENSE)
        return MonthlyBreakdown(
            total = income - expenses,
            income = income,
            expenses = expenses
        )
    }

    private fun List<TransactionEntity>.toMonthlyBreakdownByCurrency(): Map<String, MonthlyBreakdown> {
        return filter { it.loanId == null }
            .groupBy { it.currency }
            .mapValues { (_, transactions) -> transactions.toMonthlyBreakdown() }
    }

    private fun List<TransactionEntity>.sumTransactionType(type: TransactionType): BigDecimal {
        return filter { it.loanId == null && it.transactionType == type }
            .fold(BigDecimal.ZERO) { acc, transaction -> acc + transaction.amount }
    }
    
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
            .map { transactions ->
                transactions.take(limit)
            }
    }

    fun getTransactionsByAccount(bankName: String, accountLast4: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccount(bankName, accountLast4)
    }

    suspend fun countByAccount(bankName: String, accountLast4: String): Int =
        transactionDao.countByAccount(bankName, accountLast4)

    /**
     * Bulk re-target every transaction on the source account to the target
     * account. Used by the account-merge feature (#368). Returns the row
     * count actually updated. Caller cleans up the source account's balance
     * rows afterwards via [AccountBalanceRepository.deleteAccount].
     */
    suspend fun mergeAccountTransactions(
        sourceBankName: String,
        sourceAccountLast4: String,
        targetBankName: String,
        targetAccountLast4: String
    ): Int = transactionDao.mergeAccountTransactions(
        sourceBankName = sourceBankName,
        sourceAccountLast4 = sourceAccountLast4,
        targetBankName = targetBankName,
        targetAccountLast4 = targetAccountLast4,
        updatedAt = LocalDateTime.now()
    )

    /** Re-target TRANSFER from/to-account refs after an account merge (#368). */
    suspend fun retargetTransferLegRefs(
        sourceAccountLast4: String,
        targetAccountLast4: String
    ): Int = transactionDao.retargetTransferLegRefs(
        sourceAccountLast4 = sourceAccountLast4,
        targetAccountLast4 = targetAccountLast4,
        updatedAt = LocalDateTime.now()
    )

    fun getTransactionsByAccountAndDateRange(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccountAndDateRange(bankName, accountLast4, startDate, endDate)
    }

    // Methods for batch rule application
    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        // Get all non-deleted transactions as a list (not Flow) for batch processing
        // Use a large date range to get all transactions
        val startDate = LocalDateTime.of(2000, 1, 1, 0, 0)
        val endDate = LocalDateTime.now().plusYears(10)
        return transactionDao.getTransactionsBetweenDatesList(startDate, endDate)
    }

    suspend fun getUncategorizedTransactions(): List<TransactionEntity> {
        // Get all transactions without a category or with "Others" category
        return getAllTransactionsList().filter { transaction ->
            transaction.category.isNullOrBlank() || transaction.category == "Others"
        }
    }

    // ========== Transaction Split Methods ==========

    /**
     * Gets a transaction with its splits.
     */
    fun getTransactionWithSplits(transactionId: Long): Flow<TransactionWithSplits?> =
        transactionSplitDao.getTransactionWithSplits(transactionId)

    /**
     * Gets transactions with their splits for a date range and currency.
     * Useful for analytics that need to consider split amounts by category.
     */
    fun getTransactionsWithSplitsFiltered(
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String
    ): Flow<List<TransactionWithSplits>> =
        transactionSplitDao.getTransactionsWithSplitsFiltered(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59),
            currency
        )

    /**
     * Gets transactions with their splits for a date range across all currencies.
     * Used for unified currency mode where all currencies are loaded and converted.
     */
    fun getTransactionsWithSplitsFiltered(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TransactionWithSplits>> =
        transactionSplitDao.getTransactionsWithSplitsAllCurrencies(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        )

    /**
     * Gets a transaction with its splits synchronously.
     */
    suspend fun getTransactionWithSplitsSync(transactionId: Long): TransactionWithSplits? =
        transactionSplitDao.getTransactionWithSplitsSync(transactionId)

    /**
     * Gets splits for a specific transaction.
     */
    fun getSplitsForTransaction(transactionId: Long): Flow<List<TransactionSplitEntity>> =
        transactionSplitDao.getSplitsForTransaction(transactionId)

    /**
     * Checks if a transaction has splits.
     */
    suspend fun hasSplits(transactionId: Long): Boolean =
        transactionSplitDao.hasSplits(transactionId)

    /**
     * Saves splits for a transaction, replacing any existing splits.
     */
    suspend fun saveSplits(transactionId: Long, splits: List<TransactionSplitEntity>) {
        // Delete existing splits
        transactionSplitDao.deleteSplitsForTransaction(transactionId)
        // Insert new splits
        if (splits.isNotEmpty()) {
            transactionSplitDao.insertSplits(splits.map { it.copy(transactionId = transactionId) })
        }
    }

    /**
     * Removes all splits from a transaction.
     */
    suspend fun removeSplits(transactionId: Long) {
        transactionSplitDao.deleteSplitsForTransaction(transactionId)
    }

    /**
     * Inserts a single split.
     */
    suspend fun insertSplit(split: TransactionSplitEntity): Long =
        transactionSplitDao.insertSplit(split)

    /**
     * Updates a split.
     */
    suspend fun updateSplit(split: TransactionSplitEntity) =
        transactionSplitDao.updateSplit(split)

    /**
     * Deletes a single split.
     */
    suspend fun deleteSplit(split: TransactionSplitEntity) =
        transactionSplitDao.deleteSplit(split)
}
