package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.buildHeatmapMonthLabels
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private data class HeatmapSelection(
    val date: LocalDate,
    val count: Int
)

@Composable
fun HeatmapWidget(
    transactionHeatmap: Map<Long, Int>,
    modifier: Modifier = Modifier,
    blurEffects: Boolean = false,
    hazeState: HazeState? = null,
) {
    val weeksToShow = 26
    val today = LocalDate.now()
    val startDate = today.minusWeeks((weeksToShow - 1).toLong()).with(DayOfWeek.MONDAY)

    val monthLabels = remember(startDate, today) {
        buildHeatmapMonthLabels(startDate, today)
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    var selectedCell by remember { mutableStateOf<HeatmapSelection?>(null) }

    val containerColor = if (blurEffects)
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surfaceContainerLow

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    PennyWiseCardV2(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (blurEffects && hazeState != null) Modifier
                    .clip(RoundedCornerShape(Dimensions.CornerRadius.large))
                    .hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f,
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    )
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column {
            val cellSize = HEATMAP_CELL_SIZE
            val gapSize = HEATMAP_CELL_GAP
            val dayLabelWidth = HEATMAP_DAY_LABEL_WIDTH
            val primary = MaterialTheme.colorScheme.primary
            val emptyCellColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val selectedBorderColor = MaterialTheme.colorScheme.outline

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
                                text = if (selection.count > 0) {
                                    stringResource(
                                        R.string.cards_heatmap_tooltip,
                                        selection.date.format(dateFormatter),
                                        selection.count
                                    )
                                } else {
                                    "${selection.date.format(dateFormatter)} — ${stringResource(R.string.cards_heatmap_no_transactions)}"
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
                    // Day-of-week labels (M, W, F)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(gapSize),
                        modifier = Modifier.width(dayLabelWidth)
                    ) {
                        for (d in 0 until 7) {
                            val label = when (d) {
                                0 -> stringResource(R.string.cards_heatmap_day_m)
                                2 -> stringResource(R.string.cards_heatmap_day_w)
                                4 -> stringResource(R.string.cards_heatmap_day_f)
                                else -> null
                            }
                            Box(
                                modifier = Modifier.size(cellSize),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (label != null) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Heatmap grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gapSize),
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .padding(bottom = Spacing.sm)
                    ) {
                        for (w in 0 until weeksToShow) {
                            Column(verticalArrangement = Arrangement.spacedBy(gapSize)) {
                                for (d in 0 until 7) {
                                    val date = startDate.plusWeeks(w.toLong()).plusDays(d.toLong())
                                    val epochDay = date.toEpochDay()
                                    val count = transactionHeatmap[epochDay] ?: 0
                                    val isFuture = date > today
                                    val isSelected = selectedCell?.date == date

                                    val color = when {
                                        isFuture -> emptyCellColor
                                        count == 0 -> emptyCellColor
                                        count == 1 -> primary.copy(alpha = 0.25f)
                                        count == 2 -> primary.copy(alpha = 0.5f)
                                        count in 3..4 -> primary.copy(alpha = 0.75f)
                                        else -> primary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .then(
                                                if (isSelected) Modifier
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                                    .background(selectedBorderColor)
                                                    .padding(1.dp)
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                                    .background(color)
                                                else Modifier
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                                    .background(color)
                                            )
                                            .then(
                                                if (!isFuture) Modifier.clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    selectedCell = if (isSelected) null
                                                    else HeatmapSelection(date, count)
                                                }
                                                else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Month labels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dayLabelWidth)
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
                        text = stringResource(R.string.cards_heatmap_less),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    val legendColors = listOf(
                        emptyCellColor,
                        primary.copy(alpha = 0.25f),
                        primary.copy(alpha = 0.5f),
                        primary.copy(alpha = 0.75f),
                        primary
                    )
                    legendColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(HEATMAP_LEGEND_SIZE)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(c)
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Spacer(Modifier.width(Spacing.xxs))
                    Text(
                        text = stringResource(R.string.cards_heatmap_more),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val HEATMAP_CELL_SIZE = 14.dp
private val HEATMAP_CELL_GAP = Spacing.xs
private val HEATMAP_DAY_LABEL_WIDTH = Dimensions.Icon.inline
private val HEATMAP_LEGEND_SIZE = 10.dp
