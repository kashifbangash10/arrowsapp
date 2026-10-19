package com.amowpuzzle.aitricker.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.MovementRule
import com.amowpuzzle.aitricker.presentation.game.BlockedFeedback
import com.amowpuzzle.aitricker.presentation.game.DepartingArrow
import com.amowpuzzle.aitricker.presentation.game.FeedbackTiming
import com.amowpuzzle.aitricker.ui.components.ArrowSkin.drawClayArrow
import com.amowpuzzle.aitricker.ui.components.ArrowSkin.drawFlatArrow
import com.amowpuzzle.aitricker.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * The playfield: every arrow, the optional grid, and all of the board's feedback.
 *
 * The board draws what it is told and nothing more - it never decides whether a tap
 * was legal. Each animation is keyed on the token the view model issued with the
 * event, so a recomposition can never restart a flight that is already under way,
 * and two arrows leaving in quick succession animate independently.
 *
 * Every per-frame value is read inside the draw lambda rather than in composition,
 * so an arrow in flight invalidates drawing only. Nothing recomposes at 60 FPS.
 */
@Composable
fun ArrowBoard(
    level: Level,
    activeArrows: List<ArrowPath>,
    departing: List<DepartingArrow>,
    blocked: BlockedFeedback?,
    hintArrowId: Int?,
    showGrid: Boolean,
    enabled: Boolean,
    onArrowTap: (ArrowPath) -> Unit,
    onDepartureFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    classicArrows: Boolean = false,
    boardDark: Boolean = true
) {
    val label = contentDescription
    // "Big board" (zoom/pan, controls, dense-board hit-testing, geometry) is a property of
    // the level and must not change with the colour theme — tests assert every level is one.
    val bigBoard = level.presentation.usesDarkBoard
    val maxZoom = when (level.difficulty) {
        Difficulty.LEGEND, Difficulty.EXTREME, Difficulty.APEX -> 32f
        Difficulty.GRANDMASTER -> 24f
        Difficulty.MASTER -> 16f
        else -> 8f
    }
    val hintMaxZoom = when (level.difficulty) {
        Difficulty.LEGEND, Difficulty.EXTREME, Difficulty.APEX -> 20f
        Difficulty.GRANDMASTER -> 12f
        Difficulty.MASTER -> 8f
        else -> 4f
    }
    val isLevel1 = level.id == 1
    // White ink on dark is the default. Level 1 is always crisp white with navy arrows (Image 1 reference).
    val darkTheme = if (isLevel1) false else boardDark
    val boardBg = if (isLevel1) Color.White else if (darkTheme) Color(0xFF101318) else AppColors.Board
    val inkColor = if (darkTheme) Color.White else AppColors.Navy
    val isClassic = isLevel1 || classicArrows
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val controlsHeight = if (bigBoard) 72.dp else 0.dp
        val heightPx = with(density) { (maxHeight - controlsHeight).toPx() }
        if (widthPx < 1f || heightPx < 1f) return@BoxWithConstraints

        val geometry = remember(level, widthPx, heightPx) {
            BoardGeometry(level, widthPx, heightPx)
        }
        val readingScale = if (bigBoard) {
            readableBoardScale(geometry.cellSize, widthPx, heightPx, density.density, maxZoom)
        } else 1f
        var viewport by remember(level, widthPx, heightPx, readingScale) {
            mutableStateOf(BoardViewport(scale = readingScale))
        }

        fun adjustViewport(zoom: Float, pan: Offset, anchor: Offset) {
            viewport = viewport.transform(
                zoom, pan.x, pan.y, anchor.x, anchor.y,
                widthPx, heightPx, geometry.boardWidth, geometry.boardHeight, maxZoom
            )
        }

        // A hint centers the chosen piece at a usable scale without changing its white ink.
        LaunchedEffect(hintArrowId, geometry) {
            if (bigBoard && hintArrowId != null) {
                level.arrowById(hintArrowId)?.let { arrow ->
                    val head = geometry.center(arrow.head)
                    val comfortableScale = with(density) { 24.dp.toPx() } / geometry.cellSize
                    val scale = maxOf(viewport.scale, comfortableScale.coerceIn(1f, hintMaxZoom))
                    viewport = BoardViewport(scale = scale).transform(
                        1f,
                        (widthPx / 2f - head.x) * scale,
                        (heightPx / 2f - head.y) * scale,
                        widthPx / 2f, heightPx / 2f,
                        widthPx, heightPx, geometry.boardWidth, geometry.boardHeight, maxZoom
                    )
                }
            }
        }

        // Gesture handling is installed once; these keep it looking at fresh values
        // instead of the ones captured when the pointer modifier was created.
        val arrowsRef by rememberUpdatedState(activeArrows)
        val geometryRef by rememberUpdatedState(geometry)
        val viewportRef by rememberUpdatedState(viewport)
        val enabledRef by rememberUpdatedState(enabled)
        val tapRef by rememberUpdatedState(onArrowTap)
        val departureDone by rememberUpdatedState(onDepartureFinished)

        // Press and travel have independent values: acknowledgement is visible before any movement.
        val escapeProgress = remember { mutableStateMapOf<Long, Float>() }
        val pressRemaining = remember { mutableStateMapOf<Long, Float>() }
        // A tap ripple that blooms from the head of every arrow the instant it leaves.
        val ripple = remember { mutableStateMapOf<Long, Float>() }
        departing.forEach { leaving ->
            key(leaving.token) {
                LaunchedEffect(Unit) {
                    escapeProgress[leaving.token] = 0f
                    pressRemaining[leaving.token] = 1f
                    ripple[leaving.token] = 0f
                    // The ring runs alongside the press and flight so a successful tap always
                    // lands with a visible, satisfying pop rather than a silent disappearance.
                    val rippleJob = launch {
                        animate(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = tween(FeedbackTiming.RIPPLE_MS, easing = FastOutSlowInEasing)
                        ) { value, _ -> ripple[leaving.token] = value }
                    }
                    animate(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = tween(FeedbackTiming.PRESS_MS, easing = FastOutSlowInEasing)
                    ) { value, _ -> pressRemaining[leaving.token] = value }
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = ArrowMotionTiming.travelDurationMs(
                                geometry.escapeDistance(leaving.arrow) / geometry.cellSize.coerceAtLeast(0.0001f)
                            ),
                            easing = WithdrawalEasing
                        )
                    ) { value, _ -> escapeProgress[leaving.token] = value }
                    rippleJob.join()
                    // The model releases the reserved arrow and may finish the level only now.
                    departureDone(leaving.token)
                }
                DisposableEffect(Unit) {
                    onDispose {
                        escapeProgress.remove(leaving.token)
                        pressRemaining.remove(leaving.token)
                        ripple.remove(leaving.token)
                    }
                }
            }
        }

        // The refused tap: one out-and-back nudge, so the arrow visibly hits something.
        var bump by remember(blocked?.token) { mutableStateOf(0f) }
        if (blocked != null) {
            key(blocked.token) {
                LaunchedEffect(Unit) {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(FeedbackTiming.BLOCKED_MS, easing = LinearEasing)
                    ) { value, _ -> bump = value }
                    bump = 0f
                }
            }
        }

        val hintPulse = rememberInfiniteTransition(label = "hint")
        val pulse by hintPulse.animateFloat(
            initialValue = 0.32f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(760, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hintPulse"
        )

        // Built once per board size: a Stroke allocated inside the draw pass would be
        // one allocation per arrow per frame.
        val bodyStroke = remember(geometry) {
            Stroke(
                width = geometry.strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        }
        val haloStroke = remember(geometry) {
            Stroke(
                width = geometry.strokeWidth * 2.4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = controlsHeight)
                .then(if (bigBoard) Modifier.clipToBounds() else Modifier)
                .background(boardBg)
                .semantics { if (label != null) this.contentDescription = label }
                .pointerInput(level.id, bigBoard, widthPx, heightPx, maxZoom) {
                    if (bigBoard) {
                        detectBoardGestures(
                            onTransform = { centroid, pan, zoom -> adjustViewport(zoom, pan, centroid) },
                            onTap = { offset ->
                                if (enabledRef) {
                                    val point = Offset(
                                        viewportRef.boardX(offset.x, widthPx),
                                        viewportRef.boardY(offset.y, heightPx)
                                    )
                                    geometryRef.arrowAt(point, arrowsRef)?.let(tapRef)
                                }
                            }
                        )
                    } else {
                        detectTapGestures { offset ->
                            if (enabledRef) geometryRef.arrowAt(offset, arrowsRef)?.let(tapRef)
                        }
                    }
                }
        ) {
            withTransform({
                if (bigBoard) {
                    translate(viewport.panX, viewport.panY)
                    scale(viewport.scale, viewport.scale, Offset(widthPx / 2f, heightPx / 2f))
                }
            }) {
            if (showGrid) drawGridDots(level, geometry, darkTheme)

            for (arrow in activeArrows) {
                when {
                    bigBoard -> {
                        // Flush blocks pulse in place. A little free corridor permits a bounded
                        // out-and-back withdrawal along the same centerline, never a body shake.
                        if (blocked != null && blocked.arrowId == arrow.id) {
                            val wave = sin(bump * PI).toFloat()
                            drawInkAtTravel(
                                arrow, geometry,
                                pulsedStroke(bodyStroke, ArrowMotionTiming.blockedPulse(bump)),
                                ArrowMotionTiming.blockedTravelCells(blocked.cellsAhead, bump) * geometry.cellSize,
                                level.movementRule == MovementRule.SWEPT_TRANSLATION,
                                if (darkTheme) Color.White else lerp(inkColor, AppColors.Blocked, 0.7f * wave)
                            )
                        } else {
                            drawFlatArrow(arrow, geometry, bodyStroke, inkColor, 1f)
                        }
                        if (arrow.id == hintArrowId) {
                            drawCircle(
                                if (darkTheme) Color.White else AppColors.HintArrow,
                                radius = geometry.cellSize * (0.60f + 0.14f * pulse),
                                center = geometry.center(arrow.head),
                                alpha = 0.35f + 0.45f * pulse,
                                style = Stroke(geometry.strokeWidth * 0.7f)
                            )
                        }
                    }

                    arrow.id == hintArrowId ->
                        drawHinted(arrow, geometry, bodyStroke, haloStroke, pulse, isClassic)

                    blocked != null && arrow.id == blocked.arrowId ->
                        drawRefused(arrow, geometry, bodyStroke, blocked.cellsAhead, bump, isClassic)

                    isClassic ->
                        drawFlatArrow(arrow, geometry, bodyStroke, AppColors.Navy, 1f)

                    else ->
                        drawClayArrow(arrow, geometry, bodyStroke, ArrowSkin.hue(arrow), 1f)
                }
            }

            for (leaving in departing) {
                drawTapRipple(leaving, geometry, ripple[leaving.token] ?: 0f, if (darkTheme) Color.White else AppColors.Accent)
                drawEscaping(
                    leaving, geometry, bodyStroke,
                    escapeProgress[leaving.token] ?: 0f,
                    pressRemaining[leaving.token] ?: 1f, isClassic, bigBoard,
                    level.movementRule == MovementRule.SWEPT_TRANSLATION,
                    inkColor, darkTheme
                )
            }
            }
        }
        if (bigBoard) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val controlsInk = if (darkTheme) Color.White else AppColors.Navy
                val controlsFace = if (darkTheme) Color(0xFF242A33) else Color(0xFFE8EAF0)
                Text(
                    "Pinch or drag to explore \u00b7 Fit shows the full shape",
                    color = controlsInk.copy(alpha = 0.72f), fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ViewportButton("\u2212", "Zoom out", controlsInk, controlsFace) {
                        adjustViewport(1f / 1.5f, Offset.Zero, Offset(widthPx / 2f, heightPx / 2f))
                    }
                    ViewportButton("Fit", "Fit complete puzzle", controlsInk, controlsFace) { viewport = BoardViewport() }
                    ViewportButton("Read", "Readable arrow size", controlsInk, controlsFace) {
                        viewport = BoardViewport(scale = readingScale)
                    }
                    Text("${(viewport.scale * 10).toInt() / 10f}\u00d7", color = controlsInk, fontSize = 12.sp)
                    ViewportButton("+", "Zoom in", controlsInk, controlsFace) {
                        adjustViewport(1.5f, Offset.Zero, Offset(widthPx / 2f, heightPx / 2f))
                    }
                }
            }
        }

        // Animated tutorial overlay for Level 1 (Image 1 reference)
        if (isLevel1) {
            val showTutorial = activeArrows.size == 3 && departing.isEmpty()
            val tutorialTarget = remember(geometry) {
                geometry.center(com.amowpuzzle.aitricker.domain.model.Cell(3, 3))
            }
            TutorialOverlay(
                visible = showTutorial,
                targetOffset = tutorialTarget
            )
        }
    }
}

/**
 * The grid overlay: a faint dot in the middle of every cell. Dots rather than lines,
 * because a full lattice competes with the arrows for attention - the point of the
 * overlay is to help count squares, not to draw a table.
 */
private fun DrawScope.drawGridDots(level: Level, geometry: BoardGeometry, darkBoard: Boolean) {
    val radius = if (darkBoard) geometry.cellSize * 0.045f else (geometry.cellSize * 0.055f).coerceAtLeast(1.1f)
    for (row in 0 until level.rows) {
        val y = geometry.originY + (row + 0.5f) * geometry.cellSize
        for (col in 0 until level.cols) {
            val x = geometry.originX + (col + 0.5f) * geometry.cellSize
            drawCircle(color = if (darkBoard) Color(0xFF343A43) else AppColors.GridDot, radius = radius, center = Offset(x, y))
        }
    }
}

/**
 * The hinted arrow: a soft blue halo that breathes underneath, with the arrow drawn as
 * its usual clay tube on top so the hint reads as "look here" rather than recolouring
 * the piece. On the classic fallback the arrow itself takes the hint blue.
 */
private fun DrawScope.drawHinted(
    arrow: ArrowPath,
    geometry: BoardGeometry,
    stroke: Stroke,
    halo: Stroke,
    pulse: Float,
    classic: Boolean
) {
    drawPath(
        path = geometry.bodyOf(arrow),
        color = AppColors.HintGlow,
        alpha = 0.22f + 0.5f * pulse,
        style = halo
    )
    drawPath(
        path = geometry.headOf(arrow),
        color = AppColors.HintGlow,
        alpha = 0.18f + 0.4f * pulse
    )
    if (classic) {
        drawFlatArrow(arrow, geometry, stroke, AppColors.HintArrow, 1f)
    } else {
        drawClayArrow(arrow, geometry, stroke, ArrowSkin.hue(arrow), 1f)
    }
}

/**
 * A refused tap. The arrow lunges at whatever is in its way and settles back, taking
 * on a little of the warning red at the top of the arc. [cellsAhead] is capped at one
 * cell deliberately: the arrow should look like it tried, not like it moved.
 */
private fun DrawScope.drawRefused(
    arrow: ArrowPath,
    geometry: BoardGeometry,
    stroke: Stroke,
    cellsAhead: Int,
    progress: Float,
    classic: Boolean
) {
    val wave = sin(progress * PI).toFloat()
    val reach = (cellsAhead.coerceAtMost(1) * 0.40f + 0.12f) * geometry.cellSize
    val shift = wave * reach
    val dx = arrow.exit.dCol * shift
    val dy = arrow.exit.dRow * shift
    if (classic) {
        drawFlatArrow(
            arrow, geometry, stroke,
            lerp(AppColors.Navy, AppColors.Blocked, wave), 1f, dx, dy
        )
    } else {
        // The tube itself flushes toward the warning red at the top of the lunge.
        drawClayArrow(
            arrow, geometry, stroke,
            lerp(ArrowSkin.hue(arrow), AppColors.Blocked, wave * 0.8f), 1f, dx, dy
        )
    }
}

/**
 * Smooth head-led withdrawal; the body stays in original canvas coordinates.
 *
 * The press widens the stroke briefly, then the tail follows its original route.
 * Campaign ink stays opaque until the complete tail has crossed the viewport edge.
 */
private fun DrawScope.drawEscaping(
    leaving: DepartingArrow,
    geometry: BoardGeometry,
    stroke: Stroke,
    progress: Float,
    pressRemaining: Float,
    classic: Boolean,
    bigBoard: Boolean,
    rigid: Boolean,
    ink: Color,
    darkInk: Boolean
) {
    val arrow = leaving.arrow
    val p = progress.coerceIn(0f, 1f)
    val distance = geometry.escapeDistance(arrow) * p
    val stroked = pulsedStroke(stroke, pressRemaining)
    val flash = pressRemaining.coerceIn(0f, 1f)
    val fade = 1f - ((p - 0.66f) / 0.34f).coerceIn(0f, 1f)

    if (bigBoard) {
        val color = if (darkInk) Color.White else lerp(ink, AppColors.Accent, 0.55f * flash)
        drawInkAtTravel(arrow, geometry, stroked, distance, rigid, color)
        return
    }
    val dx = arrow.exit.dCol * distance
    val dy = arrow.exit.dRow * distance
    val base = if (classic) lerp(AppColors.Navy, AppColors.Accent, 0.55f * flash) else ArrowSkin.hue(arrow)
    if (rigid) {
        drawFlatArrow(arrow, geometry, stroked, base, fade, dx, dy)
    } else if (classic) {
        drawPath(geometry.withdrawnBody(arrow, distance), color = base, alpha = fade, style = stroked)
        withTransform({ translate(dx, dy) }) {
            drawPath(geometry.headOf(arrow), color = base, alpha = fade)
        }
    } else {
        // Preserve the optional old clay material. Its helper translates body and head
        // together, so convert only its body into head-local coordinates first.
        val body = Path().apply {
            addPath(geometry.withdrawnBody(arrow, distance))
            translate(Offset(-dx, -dy))
        }
        drawClayArrow(arrow, geometry, stroked, base, fade, dx, dy, body)
    }
}

/** Draws the arrow at a given travel distance in one ink colour, using the same centerline as its collision rule. */
private fun DrawScope.drawInkAtTravel(
    arrow: ArrowPath,
    geometry: BoardGeometry,
    stroke: Stroke,
    travel: Float,
    rigid: Boolean,
    color: Color,
    alpha: Float = 1f
) {
    val dx = arrow.exit.dCol * travel
    val dy = arrow.exit.dRow * travel
    if (rigid) {
        drawFlatArrow(arrow, geometry, stroke, color, alpha, dx, dy)
    } else {
        // WithdrawalPath already places this body at C([travel, travel + L]).
        // Translating it with the head would be incorrect and would sweep over neighbors.
        drawPath(geometry.withdrawnBody(arrow, travel), color = color, alpha = alpha, style = stroke)
        withTransform({ translate(dx, dy) }) {
            drawPath(geometry.headOf(arrow), color = color, alpha = alpha)
        }
    }
}

/** A ring that blooms out from the tapped arrow's head and fades, so a tap always pops. */
private fun DrawScope.drawTapRipple(
    leaving: DepartingArrow,
    geometry: BoardGeometry,
    progress: Float,
    color: Color
) {
    if (progress <= 0f || progress >= 1f) return
    drawCircle(
        color = color,
        radius = geometry.cellSize * (0.35f + 0.85f * progress),
        center = geometry.center(leaving.arrow.head),
        alpha = (1f - progress) * 0.5f,
        style = Stroke(geometry.strokeWidth * (1f - progress) + 0.5f)
    )
}

private fun pulsedStroke(base: Stroke, remaining: Float): Stroke {
    if (remaining <= 0f) return base
    return Stroke(
        width = base.width * ArrowMotionTiming.strokeMultiplier(remaining),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
}

private val WithdrawalEasing = CubicBezierEasing(0.30f, 0.02f, 0.45f, 1f)

@Composable
private fun ViewportButton(
    text: String, label: String, ink: Color, face: Color, onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(face)
            .semantics { contentDescription = label }
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = ink, fontSize = if (text.length > 1) 12.sp else 22.sp)
    }
}
