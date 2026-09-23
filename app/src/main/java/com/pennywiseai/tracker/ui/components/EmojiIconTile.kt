package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EmojiTileSize = 48.dp
private val EmojiTileCorner = 16.dp
private val EmojiGlyphSize = 26.sp

/**
 * A category / merchant emoji rendered inside a soft, tinted rounded-square
 * (squircle) tile. Self-contained: no call-site changes required.
 */
@Composable
fun EmojiIconTile(
    emoji: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(EmojiTileSize)
            .clip(RoundedCornerShape(EmojiTileCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            fontSize = EmojiGlyphSize,
            textAlign = TextAlign.Center,
        )
    }
}
