package com.pennywiseai.tracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CurrencyText
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.TransactionItem
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.localizedCategoryName
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = uiState.selectedCurrency ?: "INR"
    val categoryColor = CategoryMapping.colorFor(categoryName, uiState.categoryColorHex)

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = localizedCategoryName(categoryName),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feat_categories_back)
                        )
                    }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = Spacing.Layout.scrollBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            item {
                HeaderCard(
                    categoryName = categoryName,
                    total = uiState.total,
                    currency = currency,
                    categoryColor = categoryColor
                )
            }

            item {
                PeriodFilterRow(
                    selected = uiState.period,
                    onSelect = viewModel::selectPeriod
                )
            }

            if (uiState.availableCurrencies.size > 1) {
                item {
                    CurrencyFilterRow(
                        currencies = uiState.availableCurrencies,
                        selected = uiState.selectedCurrency,
                        onSelect = viewModel::selectCurrency
                    )
                }
            }

            item {
                StatsCard(uiState = uiState, currency = currency)
            }

            if (uiState.transactionCount == 0 && !uiState.isLoading) {
                item {
                    PennyWiseEmptyState(
                        icon = CategoryMapping.categories[categoryName]?.icon ?: Icons.Default.Category,
                        headline = stringResource(R.string.cat_detail_empty),
                        description = ""
                    )
                }
            } else {
                item {
                    MonthlyTrendCard(trend = uiState.monthlyTrend, currency = currency, barColor = categoryColor)
                }

                if (uiState.topMerchants.isNotEmpty()) {
                    item {
                        SectionHeaderV2(title = stringResource(R.string.cat_detail_top_merchants))
                    }
                    items(items = uiState.topMerchants, key = { it.name }) { merchant ->
                        ListItemCardV2(
                            title = merchant.name,
                            subtitle = "${merchant.count} ${stringResource(R.string.cat_detail_count)}",
                            amount = CurrencyFormatter.formatCurrency(merchant.amount, currency)
                        )
                    }
                }

                uiState.budget?.let { budget ->
                    item {
                        BudgetCard(budget = budget)
                    }
                }

                if (uiState.recentTransactions.isNotEmpty()) {
                    item {
                        SectionHeaderV2(title = stringResource(R.string.cat_detail_recent))
                    }
                    items(items = uiState.recentTransactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    categoryName: String,
    total: BigDecimal,
    currency: String,
    categoryColor: Color
) {
    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            CategoryIcon(
                category = categoryName,
                size = Dimensions.Icon.avatarLarge,
                tint = categoryColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedCategoryName(categoryName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                CurrencyText(
                    text = CurrencyFormatter.formatCurrency(total, currency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodFilterRow(
    selected: DetailPeriod,
    onSelect: (DetailPeriod) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items = DetailPeriod.values().toList()) { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(stringResource(period.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyFilterRow(
    currencies: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items = currencies) { code ->
            FilterChip(
                selected = selected == code,
                onClick = { onSelect(code) },
                label = { Text(code) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
private fun StatsCard(uiState: CategoryDetailUiState, currency: String) {
    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBlock(
                label = stringResource(R.string.cat_detail_this_month),
                value = CurrencyFormatter.formatCurrency(uiState.thisMonthTotal, currency),
                isCurrency = true
            )
            StatBlock(
                label = stringResource(R.string.cat_detail_monthly_avg),
                value = CurrencyFormatter.formatCurrency(uiState.monthlyAverage, currency),
                isCurrency = true
            )
            StatBlock(
                label = stringResource(R.string.cat_detail_count),
                value = uiState.transactionCount.toString(),
                isCurrency = false
            )
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, isCurrency: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        if (isCurrency) {
            CurrencyText(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthlyTrendCard(
    trend: List<MonthTotal>,
    currency: String,
    barColor: Color
) {
    val maxAmount = trend.maxOfOrNull { it.amount } ?: BigDecimal.ZERO

    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.card)) {
            SectionHeaderV2(
                title = stringResource(R.string.cat_detail_trend),
                topSpacing = Spacing.none
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth().height(TREND_CHART_HEIGHT),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                trend.forEach { month ->
                    val fraction = if (maxAmount > BigDecimal.ZERO) {
                        (month.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        CurrencyText(
                            text = CurrencyFormatter.formatCurrency(month.amount, currency),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height((TREND_BAR_MAX_HEIGHT * fraction).coerceAtLeast(Spacing.xxs))
                                .clip(RoundedCornerShape(Dimensions.CornerRadius.small))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = month.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: CategoryDetailBudget) {
    val fraction = if (budget.limitAmount > BigDecimal.ZERO) {
        (budget.spent / budget.limitAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val statusColor = if (budget.isOver) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.card)) {
            Text(
                text = stringResource(R.string.cat_detail_budget),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.cat_detail_budget_group, budget.groupName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Component.progressBarHeight)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            CurrencyText(
                text = stringResource(
                    R.string.cat_detail_budget_spent,
                    CurrencyFormatter.formatCurrency(budget.spent, budget.currency),
                    CurrencyFormatter.formatCurrency(budget.limitAmount, budget.currency)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (budget.isOver) {
                    stringResource(
                        R.string.cat_detail_budget_over,
                        CurrencyFormatter.formatCurrency(budget.remaining.abs(), budget.currency)
                    )
                } else {
                    stringResource(
                        R.string.cat_detail_budget_remaining,
                        CurrencyFormatter.formatCurrency(budget.remaining, budget.currency)
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}

private val TREND_CHART_HEIGHT = 140.dp
private val TREND_BAR_MAX_HEIGHT = 96.dp
