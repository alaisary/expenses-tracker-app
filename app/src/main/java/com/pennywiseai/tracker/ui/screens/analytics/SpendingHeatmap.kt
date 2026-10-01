package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.BalancePoint
import com.pennywiseai.tracker.ui.components.buildHeatmapMonthLabels
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

private data class AnalyticsHeatmapSelection(
    val date: LocalDate,
    val amount: Double,
    val currency: String
)

@Composable
fun SpendingHeatmap(
    data: List<BalancePoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxAmount = remember(data) { data.map { it.balance.toDouble() }.maxOrNull() ?: 1.0 }
    val groupedData = remember(data) {
        data.associate { it.timestamp.toLocalDate() to it.balance.toDouble() }
    }
    val currencyByDate = remember(data) {
        data.associate { it.timestamp.toLocalDate() to it.currency }
    }

    val sortedDates = remember(data) { data.map { it.timestamp.toLocalDate() }.distinct().sorted() }
    val startDate = sortedDates.first().with(DayOfWeek.MONDAY)
    val endDate = sortedDates.last()

    val totalWeeks = ChronoUnit.WEEKS.between(startDate, endDate.plusDays(1)).toInt() + 1

    val monthLabels = remember(startDate, endDate) {
        buildHeatmapMonthLabels(startDate, endDate)
    }

    val scrollState = rememberScrollState()

    var selectedCell by remember { mutableStateOf<AnalyticsHeatmapSelection?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val dayLabels = listOf(
        stringResource(R.string.anal_heatmap_day_mon),
        stringResource(R.string.anal_heatmap_day_tue),
        stringResource(R.string.anal_heatmap_day_wed),
        stringResource(R.string.anal_heatmap_day_thu),
        stringResource(R.string.anal_heatmap_day_fri),
        stringResource(R.string.anal_heatmap_day_sat),
        stringResource(R.string.anal_heatmap_day_sun)
    )
    val gapSize = 4.dp
    val dayLabelColumnWidth = 24.dp
    val minCellSize = 20.dp
    val maxCellSize = 28.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(16.dp)
        ) {
            val availableWidth = maxWidth - dayLabelColumnWidth
            val totalGaps = (totalWeeks - 1).coerceAtLeast(0) * gapSize.value
            val cellSize = ((availableWidth.value - totalGaps) / totalWeeks.coerceAtLeast(1))
                .coerceIn(minCellSize.value, maxCellSize.value).dp

            val gridWidth = totalWeeks * (cellSize + gapSize).value - gapSize.value
            val gridFits = gridWidth <= (availableWidth - gapSize).value

            if (!gridFits) {
                LaunchedEffect(data) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }

            val primary = MaterialTheme.colorScheme.primary
            val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            Column {
                // Tooltip for selected cell
                AnimatedVisibility(
                    visible = selectedCell != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    selectedCell?.let { selection ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xs),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.inverseSurface
                        ) {
                            Text(
                                text = if (selection.amount > 0.0) {
                                    stringResource(
                                        R.string.anal_heatmap_tooltip,
                                        selection.date.format(dateFormatter),
                                        CurrencyFormatter.formatCurrency(
                                            BigDecimal.valueOf(selection.amount),
                                            selection.currency
                                        )
                                    )
                                } else {
                                    "${selection.date.format(dateFormatter)} — ${stringResource(R.string.anal_heatmap_no_spending)}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.sm,
                                    vertical = Spacing.xs
                                )
                            )
                        }
                    }
                }

                Row {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(gapSize)
                    ) {
                        dayLabels.forEach { label ->
                            Box(
                                modifier = Modifier.size(cellSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(gapSize))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gapSize),
                        modifier = if (gridFits) {
                            Modifier.padding(bottom = 8.dp)
                        } else {
                            Modifier.horizontalScroll(scrollState).padding(bottom = 8.dp)
                        }
                    ) {
                        for (w in 0 until totalWeeks) {
                            Column(verticalArrangement = Arrangement.spacedBy(gapSize)) {
                                for (d in 0 until 7) {
                                    val date = startDate.plusWeeks(w.toLong()).plusDays(d.toLong())
                                    val amount = groupedData[date] ?: 0.0
                                    val intensity = if (maxAmount > 0) (amount / maxAmount).toFloat().coerceIn(0f, 1f) else 0f
                                    val isFuture = date > endDate
                                    val isSelected = selectedCell?.date == date

                                    val color = when {
                                        isFuture -> emptyColor
                                        amount == 0.0 -> emptyColor
                                        intensity < 0.25f -> primary.copy(alpha = 0.25f)
                                        intensity < 0.5f -> primary.copy(alpha = 0.5f)
                                        intensity < 0.75f -> primary.copy(alpha = 0.75f)
                                        else -> primary
                                    }

                                    val selectedBorderColor = MaterialTheme.colorScheme.outline

                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .then(
                                                if (isSelected) Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(selectedBorderColor)
                                                    .padding(1.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(color)
                                                else Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(color)
                                            )
                                            .then(
                                                if (!isFuture) Modifier.clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    selectedCell = if (isSelected) null
                                                    else AnalyticsHeatmapSelection(
                                                        date = date,
                                                        amount = amount,
                                                        currency = currencyByDate[date] ?: "INR"
                                                    )
                                                }
                                                else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dayLabelColumnWidth)
                ) {
                    monthLabels.forEach { (weekIndex, label) ->
                        val xOffset = (weekIndex * (cellSize + gapSize).value).dp
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.offset(x = xOffset)
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Legend: Less □□□□□ More
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.anal_heatmap_less),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    val legendColors = listOf(
                        emptyColor,
                        primary.copy(alpha = 0.25f),
                        primary.copy(alpha = 0.5f),
                        primary.copy(alpha = 0.75f),
                        primary
                    )
                    legendColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(c)
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Spacer(Modifier.width(Spacing.xxs))
                    Text(
                        text = stringResource(R.string.anal_heatmap_more),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
