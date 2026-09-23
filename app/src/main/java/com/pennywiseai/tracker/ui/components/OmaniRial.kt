package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

/** Aspect ratio (w / h) of the official Omani Rial sign vector. */
private const val OMANI_RIAL_ASPECT = 355.0f / 233.2f

/**
 * The official Omani Rial sign (CBO, 2025), drawn from the bundled vector so it
 * renders on every device — system fonts don't carry U+20C4 yet.
 */
@Composable
fun OmaniRialSymbol(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Image(
        painter = painterResource(R.drawable.ic_omani_rial),
        contentDescription = stringResource(R.string.cards_omr_symbol),
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

/**
 * Renders a money amount, using the official Omani Rial sign inline when
 * [currency] is OMR (symbol precedes the value, per CBO usage rules). Every
 * other currency falls back to the plain formatted string.
 */
@Composable
fun CurrencyAmount(
    amount: BigDecimal,
    currency: String,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isHidden: Boolean = false,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    if (isHidden || !currency.equals("OMR", ignoreCase = true)) {
        CurrencyText(
            text = if (isHidden) "••••••" else CurrencyFormatter.formatCurrency(amount, currency),
            style = style,
            color = color,
            maxLines = maxLines,
            modifier = modifier,
        )
        return
    }

    val density = LocalDensity.current
    val fontSize = if (style.fontSize.isSpecified) style.fontSize else 16.sp
    val symbolHeight = with(density) { (fontSize * 0.72f).toDp() }
    val symbolWidth = symbolHeight * OMANI_RIAL_ASPECT
    val amountText = CurrencyFormatter.formatAmountOnly(amount, currency)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(symbolHeight * 0.22f),
    ) {
        // Arabic (RTL): amount first, so the mark lands on its left.
        // LTR: mark precedes the amount (left as well).
        if (isRtl) {
            AmountLabel(amountText, style, color, maxLines)
            OmaniRialSymbol(
                tint = color,
                modifier = Modifier.height(symbolHeight).width(symbolWidth),
            )
        } else {
            OmaniRialSymbol(
                tint = color,
                modifier = Modifier.height(symbolHeight).width(symbolWidth),
            )
            AmountLabel(amountText, style, color, maxLines)
        }
    }
}

@Composable
private fun AmountLabel(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
    )
}

/**
 * The textual OMR mark that [CurrencyFormatter] emits. [CurrencyText] swaps it
 * for the official vector so list rows and subtitle strings show the real sign
 * instead of the fallback abbreviation.
 */
private const val OMR_TEXT_MARKER = "ر.ع."
private const val OMR_INLINE_ID = "omr_sign"

/**
 * Drop-in replacement for [Text] for strings that may contain a formatted money
 * amount. Any OMR mark inside [text] is drawn as the official Omani Rial sign
 * inline; all other text (and other currencies) render exactly as before.
 */
@Composable
fun CurrencyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    textAlign: TextAlign? = null,
) {
    if (!text.contains(OMR_TEXT_MARKER)) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
            textAlign = textAlign,
        )
        return
    }

    val fontSize = if (style.fontSize.isSpecified) style.fontSize else 16.sp
    val placeholderHeight = fontSize * 0.72f
    val placeholderWidth = placeholderHeight * OMANI_RIAL_ASPECT
    val tint = if (color == Color.Unspecified) LocalContentColor.current else color

    val annotated = buildAnnotatedString {
        var start = 0
        while (true) {
            val idx = text.indexOf(OMR_TEXT_MARKER, start)
            if (idx < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, idx))
            appendInlineContent(OMR_INLINE_ID, OMR_TEXT_MARKER)
            start = idx + OMR_TEXT_MARKER.length
        }
    }

    val inline = InlineTextContent(
        Placeholder(
            width = placeholderWidth,
            height = placeholderHeight,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        )
    ) {
        OmaniRialSymbol(tint = tint, modifier = Modifier.fillMaxSize())
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        textAlign = textAlign,
        inlineContent = mapOf(OMR_INLINE_ID to inline),
    )
}
