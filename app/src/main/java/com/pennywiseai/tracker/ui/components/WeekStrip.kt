package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.theme.Spacing

private val WeekStripBarArea = 56.dp
private val WeekStripBarWidth = 8.dp
private val WeekStripBarMinHeight = 6.dp
private val WeekStripDot = 6.dp

/**
 * Seven evenly spaced day columns with a bar scaled to the largest value in
 * [days]. [todayIndex] gets a stronger fill and a bold label; zero-value days
 * collapse to a small dot. Order is start-to-end, so it follows RTL.
 */
@Composable
fun WeekStrip(
    days: List<Float>,
    labels: List<String>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val maxValue = days.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    val barColor = MaterialTheme.colorScheme.primary
    val mutedBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val dotColor = MaterialTheme.colorScheme.outlineVariant
    val todayDotColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(7) { index ->
            val value = days.getOrNull(index)?.coerceAtLeast(0f) ?: 0f
            val isToday = index == todayIndex
            val fraction = if (maxValue > 0f) (value / maxValue).coerceIn(0f, 1f) else 0f
            val barHeight = (WeekStripBarArea * fraction).coerceAtLeast(WeekStripBarMinHeight)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WeekStripBarArea),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .width(WeekStripBarWidth)
                                .height(barHeight)
                                .clip(RoundedCornerShape(50))
                                .background(if (isToday) barColor else mutedBarColor)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(WeekStripDot)
                                .height(WeekStripDot)
                                .clip(CircleShape)
                                .background(if (isToday) todayDotColor else dotColor)
                        )
                    }
                }
                Text(
                    text = labels.getOrNull(index).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
