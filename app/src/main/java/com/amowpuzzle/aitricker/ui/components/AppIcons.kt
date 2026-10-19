package com.amowpuzzle.aitricker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amowpuzzle.aitricker.R

/**
 * The app's icon set.
 *
 * Every control glyph is an authentic Material Symbol shipped as a vector drawable in
 * res/drawable and rendered here through [Icon] so it is tinted, centred and scaled the
 * same way everywhere. Wrapping each in a named composable keeps the call sites reading
 * as intent ("a lightbulb", "a gear") rather than resource ids, and means the whole app
 * can be re-skinned by swapping the drawable behind a name.
 *
 * Only the two brand marks - [BrandArrowIcon] and [PointerArrowIcon]'s companion on the
 * board - are drawn by hand, because they must share the exact geometry the board uses
 * for its arrows and no Material glyph matches that.
 */

/** Render a Material Symbol drawable tinted to [color] at [size]. */
@Composable
private fun MaterialIcon(
    resId: Int,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(resId),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(size)
    )
}

@Composable
fun ChevronLeftIcon(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_chevron_left, color, size, modifier)

@Composable
fun GearIcon(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_settings, color, size, modifier)

@Composable
fun HeartIcon(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_favorite, color, size, modifier)

@Composable
fun LightbulbIcon(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_lightbulb, color, size, modifier)

@Composable
fun GridHashIcon(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_grid, color, size, modifier)

@Composable
fun LockIcon(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_lock, color, size, modifier)

@Composable
fun CheckIcon(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_check, color, size, modifier)

@Composable
fun RestartIcon(color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_refresh, color, size, modifier)

@Composable
fun CloseIcon(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_close, color, size, modifier)

/**
 * The solid diagonal marker beside the remaining-arrow count. Material's arrow_outward,
 * so the count reads as "arrows still to send out".
 */
@Composable
fun PointerArrowIcon(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) =
    MaterialIcon(R.drawable.ic_arrow_outward, color, size, modifier)

/**
 * The app mark: a rounded shaft with a solid triangular head, the same geometry the
 * board draws its arrows with. Hand-drawn because it must match the board exactly and
 * doubles as the launcher foreground.
 */
@Composable
fun BrandArrowIcon(color: Color, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = 3.1f * u
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(3.4f * u, 12f * u),
            androidx.compose.ui.geometry.Offset(16.4f * u, 12f * u),
            stroke,
            StrokeCap.Round
        )
        val head = Path().apply {
            moveTo(21.2f * u, 12f * u)
            lineTo(14.4f * u, 7.4f * u)
            lineTo(14.4f * u, 16.6f * u)
            close()
        }
        drawPath(head, color)
    }
}
