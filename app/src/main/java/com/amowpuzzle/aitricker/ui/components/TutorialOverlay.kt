package com.amowpuzzle.aitricker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Interactive tutorial overlay for Level 1:
 * - Animated cartoon glove hand with tapping gesture and pulse ripple
 * - Speech bubble pill "Tap to remove" with pointer arrow pointing up to the arrow
 */
@Composable
fun TutorialOverlay(
    visible: Boolean,
    targetOffset: Offset,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(250)) + scaleOut(tween(250)),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "tutorialHand")

        // 1200ms looping tap gesture: hover -> tap down -> ripple -> lift back
        val tapProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "tapProgress"
        )

        // Float motion for tooltip bubble
        val floatPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "floatPhase"
        )

        val floatY = sin(floatPhase) * 3.5f

        Box(modifier = Modifier.wrapContentSize()) {
            // Hand positioned with fingertip touching the target arrow
            TutorialCartoonHand(
                tapProgress = tapProgress,
                modifier = Modifier
                    .offset {
                        // Position so the index fingertip aligns with targetOffset
                        IntOffset(
                            x = (targetOffset.x - 30.dp.toPx()).toInt(),
                            y = (targetOffset.y - 48.dp.toPx()).toInt()
                        )
                    }
                    .size(90.dp)
            )

            // Speech bubble tooltip placed below the target arrow
            TutorialTooltipBubble(
                text = "Tap to remove",
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (targetOffset.x - 85.dp.toPx()).toInt(),
                            y = (targetOffset.y + 42.dp.toPx() + floatY.dp.toPx()).toInt()
                        )
                    }
            )
        }
    }
}

/**
 * Cartoon white glove hand drawn with Compose Canvas and smooth tap animation.
 */
@Composable
fun TutorialCartoonHand(
    tapProgress: Float,
    modifier: Modifier = Modifier
) {
    // Tap physics: 0..0.4 hover, 0.4..0.6 tap down, 0.6..0.7 bottom, 0.7..1.0 return
    val tapScale: Float
    val tapNudgeX: Float
    val tapNudgeY: Float
    val rippleProgress: Float

    when {
        tapProgress < 0.40f -> {
            tapScale = 1.0f
            tapNudgeX = 0f
            tapNudgeY = 0f
            rippleProgress = 0f
        }
        tapProgress < 0.60f -> {
            val t = (tapProgress - 0.40f) / 0.20f
            val ease = FastOutSlowInEasing.transform(t)
            tapScale = 1.0f - 0.12f * ease
            tapNudgeX = -6f * ease
            tapNudgeY = 6f * ease
            rippleProgress = 0f
        }
        tapProgress < 0.72f -> {
            val t = (tapProgress - 0.60f) / 0.12f
            tapScale = 0.88f + 0.04f * t
            tapNudgeX = -6f + 2f * t
            tapNudgeY = 6f - 2f * t
            rippleProgress = t
        }
        else -> {
            val t = (tapProgress - 0.72f) / 0.28f
            val ease = FastOutSlowInEasing.transform(t)
            tapScale = 0.92f + 0.08f * ease
            tapNudgeX = -4f * (1f - ease)
            tapNudgeY = 4f * (1f - ease)
            rippleProgress = 1f
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Fingertip contact point in local canvas coordinates: (w * 0.36f, h * 0.58f)
        val contactX = w * 0.36f
        val contactY = h * 0.58f

        // Draw tap contact ripple if active
        if (rippleProgress in 0.01f..0.99f) {
            val maxR = w * 0.28f
            val curR = maxR * rippleProgress
            val alpha = (1f - rippleProgress).coerceIn(0f, 1f) * 0.75f
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = alpha),
                radius = curR,
                center = Offset(contactX + tapNudgeX, contactY + tapNudgeY),
                style = Stroke(width = 3.dp.toPx() * (1f - rippleProgress * 0.5f))
            )
            drawCircle(
                color = Color(0xFF64B5F6).copy(alpha = alpha * 0.4f),
                radius = curR * 0.6f,
                center = Offset(contactX + tapNudgeX, contactY + tapNudgeY)
            )
        }

        // Apply tap transformation pivot around contact point
        with(drawContext.canvas) {
            save()
            translate(tapNudgeX, tapNudgeY)
            translate(contactX, contactY)
            scale(tapScale, tapScale)
            translate(-contactX, -contactY)
        }

        // 1. Soft drop shadow underneath the glove
        val shadowPath = createCartoonGlovePath(w, h)
        translate(left = 5.dp.toPx(), top = 7.dp.toPx()) {
            drawPath(
                path = shadowPath,
                color = Color(0x2E0F172A)
            )
        }

        // 2. White glove body fill
        val glovePath = createCartoonGlovePath(w, h)
        drawPath(
            path = glovePath,
            color = Color.White
        )

        // 3. Subtle inner shading on bottom-right curves of fingers for 3D cartoon volume
        val shadePath = Path().apply {
            moveTo(w * 0.42f, h * 0.62f)
            cubicTo(w * 0.48f, h * 0.72f, w * 0.64f, h * 0.76f, w * 0.72f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.74f)
            cubicTo(w * 0.60f, h * 0.82f, w * 0.44f, h * 0.78f, w * 0.38f, h * 0.66f)
            close()
        }
        drawPath(
            path = shadePath,
            color = Color(0xFFE2E8F0).copy(alpha = 0.85f)
        )

        // 4. Dark navy / black crisp cartoon contour outline
        val outlineColor = Color(0xFF132238)
        val outlineStroke = Stroke(
            width = 2.6.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawPath(
            path = glovePath,
            color = outlineColor,
            style = outlineStroke
        )

        // 5. Finger folds / knuckle separator lines
        val linesStroke = Stroke(
            width = 2.0.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        // Crease between index and middle finger
        val crease1 = Path().apply {
            moveTo(w * 0.44f, h * 0.50f)
            cubicTo(w * 0.48f, h * 0.54f, w * 0.52f, h * 0.55f, w * 0.56f, h * 0.52f)
        }
        drawPath(crease1, color = outlineColor, style = linesStroke)

        // Crease between middle and ring finger
        val crease2 = Path().apply {
            moveTo(w * 0.56f, h * 0.64f)
            cubicTo(w * 0.60f, h * 0.62f, w * 0.64f, h * 0.58f, w * 0.68f, h * 0.56f)
        }
        drawPath(crease2, color = outlineColor, style = linesStroke)

        // 6. Three signature cartoon glove darts on back of hand
        val dartStroke = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Left dart
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.60f, h * 0.36f),
            end = Offset(w * 0.68f, h * 0.42f),
            strokeWidth = dartStroke.width,
            cap = StrokeCap.Round
        )
        // Center dart
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.66f, h * 0.32f),
            end = Offset(w * 0.73f, h * 0.39f),
            strokeWidth = dartStroke.width,
            cap = StrokeCap.Round
        )
        // Right dart
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.72f, h * 0.28f),
            end = Offset(w * 0.78f, h * 0.36f),
            strokeWidth = dartStroke.width,
            cap = StrokeCap.Round
        )

        // 7. Glove cuff rim at wrist (top right)
        val cuffPath = Path().apply {
            moveTo(w * 0.78f, h * 0.20f)
            cubicTo(w * 0.84f, h * 0.24f, w * 0.88f, h * 0.30f, w * 0.84f, h * 0.36f)
        }
        drawPath(cuffPath, color = outlineColor, style = linesStroke)

        drawContext.canvas.restore()
    }
}

/**
 * Creates the organic bezier outline of a classic cartoon glove with index finger pointing down-left.
 */
private fun createCartoonGlovePath(w: Float, h: Float): Path {
    return Path().apply {
        // Start at wrist (top-right)
        moveTo(w * 0.82f, h * 0.24f)

        // Top back of hand curving down toward thumb
        cubicTo(w * 0.70f, h * 0.22f, w * 0.52f, h * 0.26f, w * 0.42f, h * 0.32f)

        // Thumb curving on the upper-left
        cubicTo(w * 0.36f, h * 0.34f, w * 0.28f, h * 0.38f, w * 0.30f, h * 0.44f)
        cubicTo(w * 0.32f, h * 0.48f, w * 0.38f, h * 0.46f, w * 0.42f, h * 0.44f)

        // Webbing before index finger
        cubicTo(w * 0.40f, h * 0.47f, w * 0.36f, h * 0.50f, w * 0.32f, h * 0.53f)

        // Index finger extending downward-left
        cubicTo(w * 0.28f, h * 0.56f, w * 0.24f, h * 0.58f, w * 0.26f, h * 0.63f)
        // Rounded index fingertip
        cubicTo(w * 0.28f, h * 0.67f, w * 0.35f, h * 0.67f, w * 0.38f, h * 0.62f)

        // Inner side of index finger
        cubicTo(w * 0.42f, h * 0.57f, w * 0.44f, h * 0.55f, w * 0.47f, h * 0.58f)

        // Curled middle finger knuckle
        cubicTo(w * 0.48f, h * 0.66f, w * 0.56f, h * 0.72f, w * 0.62f, h * 0.68f)

        // Curled ring finger knuckle
        cubicTo(w * 0.66f, h * 0.72f, w * 0.74f, h * 0.70f, w * 0.76f, h * 0.62f)

        // Little finger / base of hand
        cubicTo(w * 0.80f, h * 0.56f, w * 0.82f, h * 0.46f, w * 0.86f, h * 0.36f)

        // Back to wrist cuff
        cubicTo(w * 0.87f, h * 0.30f, w * 0.86f, h * 0.26f, w * 0.82f, h * 0.24f)
        close()
    }
}

/**
 * Custom speech bubble pill with a pointer triangle at the top.
 */
@Composable
fun TutorialTooltipBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    val bubbleShape = SpeechBubbleShape(
        cornerRadius = 18.dp,
        pointerWidth = 20.dp,
        pointerHeight = 11.dp,
        pointerOffsetFraction = 0.52f
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = bubbleShape,
                ambientColor = Color(0x401E88E5),
                spotColor = Color(0x601E88E5)
            )
            .background(Color(0xFF2196F3), shape = bubbleShape)
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Custom Speech Bubble Shape: a rounded rectangle with a triangular pointer arrow on top.
 */
class SpeechBubbleShape(
    private val cornerRadius: Dp = 18.dp,
    private val pointerWidth: Dp = 20.dp,
    private val pointerHeight: Dp = 11.dp,
    private val pointerOffsetFraction: Float = 0.5f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cr = with(density) { cornerRadius.toPx() }
        val pw = with(density) { pointerWidth.toPx() }
        val ph = with(density) { pointerHeight.toPx() }

        val pointerCenterX = size.width * pointerOffsetFraction
        val pointerLeft = pointerCenterX - pw / 2f
        val pointerRight = pointerCenterX + pw / 2f

        val path = Path().apply {
            // Start after top-left corner
            moveTo(cr, ph)

            // Top edge to pointer
            lineTo(pointerLeft, ph)
            // Pointer peak pointing UP
            lineTo(pointerCenterX, 0f)
            lineTo(pointerRight, ph)

            // Top edge to top-right corner
            lineTo(size.width - cr, ph)

            // Top-right rounded corner
            arcTo(
                rect = Rect(size.width - 2 * cr, ph, size.width, ph + 2 * cr),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Right edge to bottom-right corner
            lineTo(size.width, size.height - cr)

            // Bottom-right rounded corner
            arcTo(
                rect = Rect(size.width - 2 * cr, size.height - 2 * cr, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Bottom edge to bottom-left corner
            lineTo(cr, size.height)

            // Bottom-left rounded corner
            arcTo(
                rect = Rect(0f, size.height - 2 * cr, 2 * cr, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Left edge to top-left corner
            lineTo(0f, ph + cr)

            // Top-left rounded corner
            arcTo(
                rect = Rect(0f, ph, 2 * cr, ph + 2 * cr),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }

        return Outline.Generic(path)
    }
}
