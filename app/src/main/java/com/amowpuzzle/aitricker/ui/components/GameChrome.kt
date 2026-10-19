package com.amowpuzzle.aitricker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText

/**
 * The round control the game uses for hint and grid.
 *
 * Kept as a thin alias over [ClayCircleButton] so existing call sites compile unchanged
 * while picking up the new clay material. [active] keeps the button pressed-in and ringed
 * for as long as the thing it controls is on, so the grid button reads as a switch.
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    badge: Int? = null,
    icon: @Composable (Color) -> Unit
) {
    ClayCircleButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        active = active,
        enabled = enabled,
        badge = badge,
        icon = icon
    )
}

/** The little count that rides on the hint button. */
@Composable
fun BadgeBubble(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .shadow(3.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(if (count > 0) AppColors.Accent else AppColors.SlateMuted)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(text = count.toString(), style = AppText.Badge, color = AppColors.Background)
    }
}

/**
 * The rounded chip used for the arrow count and the difficulty label. A thin alias over
 * [ClayPill] so it carries the same clay material as every other surface.
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = AppColors.Background,
    contentColor: Color = AppColors.Navy,
    contentDescription: String? = null,
    leading: @Composable (() -> Unit)? = null
) {
    ClayPill(
        text = text,
        modifier = modifier,
        container = container,
        contentColor = contentColor,
        style = AppText.Pill,
        contentDescription = contentDescription,
        leading = leading
    )
}

/** Hearts, spent ones greyed rather than removed so the row never shifts. */
@Composable
fun HeartsRow(
    lives: Int,
    total: Int,
    modifier: Modifier = Modifier,
    heartSize: Dp = 27.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            HeartIcon(
                color = if (index < lives) AppColors.Heart else AppColors.HeartEmpty,
                size = heartSize
            )
        }
    }
}

/**
 * The full-width primary action on every screen. A thin alias over [ClayButton] with its
 * 3D riser and physical press. [elevation] is kept for source compatibility with existing
 * call sites but no longer used - the clay button owns its own depth.
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = AppColors.Accent,
    contentColor: Color = AppColors.Background,
    enabled: Boolean = true,
    height: Dp = 58.dp,
    @Suppress("UNUSED_PARAMETER") elevation: Dp = 4.dp,
    style: TextStyle = AppText.Button
) {
    ClayButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        container = container,
        contentColor = contentColor,
        enabled = enabled,
        height = height,
        style = style
    )
}

/** A label that behaves like a button and looks like text. */
@Composable
fun PlainTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Accent,
    style: TextStyle = AppText.Button
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClickLabel = text, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = style, color = color, textAlign = TextAlign.Center)
    }
}

/**
 * The bar at the top of every screen: an optional back chevron, a centred title, and
 * an optional gear. The two side slots keep their width even when empty so the title
 * stays optically centred whether or not a screen has both controls.
 */
@Composable
fun HeaderBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String? = null,
    onSettings: (() -> Unit)? = null,
    settingsDescription: String? = null,
    titleStyle: TextStyle = AppText.ScreenTitle,
    titleColor: Color = AppColors.Navy
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderAction(onClick = onBack, label = backDescription) { tint ->
            ChevronLeftIcon(color = tint, size = 27.dp)
        }
        Text(
            text = title,
            style = titleStyle,
            color = titleColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        HeaderAction(onClick = onSettings, label = settingsDescription) { tint ->
            GearIcon(color = tint, size = 25.dp)
        }
    }
}

/** One side slot of [HeaderBar]: a tappable icon, or empty space of the same width. */
@Composable
private fun HeaderAction(
    onClick: (() -> Unit)?,
    label: String?,
    icon: @Composable (Color) -> Unit
) {
    if (onClick == null) {
        Spacer(Modifier.size(46.dp))
        return
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon(AppColors.Accent)
    }
}
