package com.amowpuzzle.aitricker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.ui.theme.AppColors

/**
 * The arrow's material.
 *
 * The reference game draws flat black arrows; ours are glossy, extruded, multi-colour
 * tubes. All of that lives here so the live board, the completion preview and the
 * tutorial illustrations read as the exact same object.
 *
 * A clay arrow is the one body-plus-head shape drawn several times, stacked in depth:
 *
 *   1. two soft contact shadows, offset down and fading, that seat the tube on the board
 *   2. a riser in the shade colour, offset down, that reads as the tube's thickness
 *   3. the top face in the arrow's own hue, lifted to a subtle vertical gloss
 *   4. a thin specular highlight along the top, offset up toward white
 *
 * Colour is decoration and identity only. It never encodes whether an arrow can move -
 * that is the head direction's job alone - so the classic flat [AppColors.Navy] skin is
 * kept as an accessibility fallback and produces the same silhouette.
 */
object ArrowSkin {

    /** Depth offsets as a fraction of stroke width, so the look scales with the board. */
    private const val RISER_DROP = 0.34f
    private const val SHADOW_DROP_1 = 0.55f
    private const val SHADOW_DROP_2 = 1.05f
    private const val SPECULAR_LIFT = 0.24f

    private const val SHADE = 0.66f      // riser darkness under the top face
    private const val TOP_LIFT = 0.10f   // gloss on the upper half of the face
    private const val SPECULAR_ALPHA = 0.55f

    /** The base hue for an arrow, spread across the eight bases by id. */
    fun hue(arrow: ArrowPath): Color =
        AppColors.ArrowBases[(arrow.id.mod(AppColors.ArrowBases.size))]

    private fun Color.shade(f: Float): Color = Color(red * f, green * f, blue * f, alpha)
    private fun Color.lift(a: Float): Color = lerp(this, Color.White, a)

    /**
     * Draw [arrow] as a 3D clay tube in [hue] at [alpha]. [extraOffsetX]/[extraOffsetY]
     * translate the whole stack together - used to fly an escaping arrow off the board
     * without disturbing its internal depth.
     */
    fun DrawScope.drawClayArrow(
        arrow: ArrowPath,
        geometry: BoardGeometry,
        stroke: Stroke,
        hue: Color,
        alpha: Float = 1f,
        extraOffsetX: Float = 0f,
        extraOffsetY: Float = 0f,
        bodyOverride: Path? = null
    ) {
        val body = bodyOverride ?: geometry.bodyOf(arrow)
        val head = geometry.headOf(arrow)
        val sw = geometry.strokeWidth
        val riser = hue.shade(SHADE)

        withTransform({ translate(extraOffsetX, extraOffsetY) }) {
            // 1. Contact shadows: warm, soft, offset down. Two passes fake a blur.
            drawStack(body, head, stroke, AppColors.ClayShadow, 0.10f * alpha, 0f, sw * SHADOW_DROP_2)
            drawStack(body, head, stroke, AppColors.ClayShadow, 0.14f * alpha, 0f, sw * SHADOW_DROP_1)
            // 2. Riser: the coloured side wall, just below the face.
            drawStack(body, head, stroke, riser, alpha, 0f, sw * RISER_DROP)
            // 3. Top face: the hue, with a light upper gloss (approximated flat here;
            //    the specular pass on top is what actually sells the roundness).
            drawStack(body, head, stroke, hue.lift(TOP_LIFT), alpha, 0f, 0f)
            // 4. Specular: a thin white-lifted highlight riding just above the face.
            drawPath(body, color = Color.White, alpha = SPECULAR_ALPHA * alpha, style = geometry.specularStroke)
            drawPath(head, color = Color.White.copy(alpha = SPECULAR_ALPHA * 0.5f * alpha))
        }
    }

    /** The flat classic skin: one colour, no depth. The accessibility fallback. */
    fun DrawScope.drawFlatArrow(
        arrow: ArrowPath,
        geometry: BoardGeometry,
        stroke: Stroke,
        color: Color,
        alpha: Float = 1f,
        extraOffsetX: Float = 0f,
        extraOffsetY: Float = 0f,
        bodyOverride: Path? = null
    ) {
        withTransform({ translate(extraOffsetX, extraOffsetY) }) {
            drawPath(bodyOverride ?: geometry.bodyOf(arrow), color = color, alpha = alpha, style = stroke)
            drawPath(geometry.headOf(arrow), color = color, alpha = alpha)
        }
    }

    private fun DrawScope.drawStack(
        body: Path,
        head: Path,
        stroke: Stroke,
        color: Color,
        alpha: Float,
        dx: Float,
        dy: Float
    ) {
        withTransform({ translate(dx, dy) }) {
            drawPath(body, color = color, alpha = alpha, style = stroke)
            drawPath(head, color = color, alpha = alpha)
        }
    }
}
