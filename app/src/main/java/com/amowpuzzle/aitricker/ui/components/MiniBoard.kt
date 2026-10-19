package com.amowpuzzle.aitricker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.ui.components.ArrowSkin.drawClayArrow
import com.amowpuzzle.aitricker.ui.components.ArrowSkin.drawFlatArrow
import com.amowpuzzle.aitricker.ui.theme.AppColors

/**
 * A board with no behaviour: the miniature on the completion card, and the
 * illustrations in the tutorial.
 *
 * It reuses [BoardGeometry], so a level shrunk to thumbnail size keeps the same
 * proportions as the one that was just played - the preview is recognisably the
 * puzzle rather than a decoration. Geometry is remembered, not rebuilt per frame,
 * because these previews sit inside screens that animate.
 */
@Composable
fun MiniBoard(
    level: Level,
    modifier: Modifier = Modifier,
    arrows: List<ArrowPath> = level.arrows,
    color: Color = AppColors.Navy,
    highlighted: Set<Int> = emptySet(),
    highlightColor: Color = AppColors.HintArrow,
    clay: Boolean = true
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        if (widthPx < 1f || heightPx < 1f) return@BoxWithConstraints

        val geometry = remember(level, widthPx, heightPx) {
            BoardGeometry(level, widthPx, heightPx)
        }
        val stroke = remember(geometry) {
            Stroke(
                width = geometry.strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (level.presentation.usesDarkBoard) drawRect(Color(0xFF101318))
            for (arrow in arrows) {
                when {
                    level.presentation.usesDarkBoard ->
                        drawFlatArrow(arrow, geometry, stroke, Color.White, 1f)

                    // A highlighted arrow is drawn flat in the highlight colour so it
                    // reads as an instruction, not just another coloured piece.
                    arrow.id in highlighted ->
                        drawFlatArrow(arrow, geometry, stroke, highlightColor, 1f)

                    clay ->
                        drawClayArrow(arrow, geometry, stroke, ArrowSkin.hue(arrow), 1f)

                    else ->
                        drawFlatArrow(arrow, geometry, stroke, color, 1f)
                }
            }
        }
    }
}
