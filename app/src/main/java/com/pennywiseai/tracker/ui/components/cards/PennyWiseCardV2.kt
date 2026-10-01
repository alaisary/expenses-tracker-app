package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.theme.Dimensions

/**
 * The app's standard card container.
 *
 * One card style, everywhere: `shapes.large` corners, `surfaceContainerLow`
 * fill, no elevation, and — in dark mode only — a 0.5dp hairline so the card
 * separates from an AMOLED-black background where a tonal fill alone barely
 * registers. Light mode needs no border because the tonal step is visible.
 *
 * Pass [contentPadding] rather than padding the content yourself, so the
 * ripple on a clickable card covers the whole surface.
 */
@Composable
fun PennyWiseCardV2(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = MaterialTheme.shapes.large,
    /**
     * Default-only convenience for callers that just want to swap the
     * container colour (e.g. a selected-state tint) without constructing a
     * full [CardColors]. Wired into the default value of [colors]; if a
     * caller passes [colors] explicitly, that wins and this value is unused.
     */
    containerColor: androidx.compose.ui.graphics.Color? = null,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = Dimensions.Elevation.card
    ),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    /**
     * Optional long-press handler. Routes the card through
     * [Modifier.combinedClickable] so tap and long-press resolve on the same
     * gesture surface — important when the card lives inside a scrolling
     * container or another drag-aware parent (e.g. SwipeToDismissBox) that
     * would otherwise race with a child pointerInput.
     *
     * **Requires [onClick]** to also be non-null. A long-press-only card
     * would still announce as a button to accessibility but no-op on tap;
     * we fail fast rather than ship that affordance.
     */
    onLongClick: (() -> Unit)? = null,
    contentPadding: Dp = Dimensions.Padding.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val effectiveBorder = border ?: if (isSystemInDarkTheme()) {
        BorderStroke(
            width = Dimensions.Component.hairline,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
    } else {
        null
    }

    when {
        onLongClick != null -> {
            val tap = requireNotNull(onClick) {
                "PennyWiseCardV2: onLongClick requires onClick to also be non-null."
            }
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale = remember { Animatable(1f) }
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    scale.animateTo(0.97f, spring(stiffness = Spring.StiffnessHigh))
                } else {
                    scale.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
            Card(
                modifier = modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = tap,
                        onLongClick = onLongClick
                    ),
                colors = colors,
                shape = shape,
                elevation = elevation,
                border = effectiveBorder
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
        onClick != null -> {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale = remember { Animatable(1f) }
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    scale.animateTo(0.97f, spring(stiffness = Spring.StiffnessHigh))
                } else {
                    scale.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
            Card(
                modifier = modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
                onClick = onClick,
                interactionSource = interactionSource,
                colors = colors,
                shape = shape,
                elevation = elevation,
                border = effectiveBorder
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
        else -> {
            Card(
                modifier = modifier,
                colors = colors,
                shape = shape,
                elevation = elevation,
                border = effectiveBorder
            ) {
                Column(modifier = Modifier.padding(contentPadding)) { content() }
            }
        }
    }
}
