package com.amowpuzzle.aitricker.redesign

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.amowpuzzle.aitricker.domain.engine.CollisionEngine
import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.geometry.WithdrawalPath
import com.amowpuzzle.aitricker.domain.geometry.WithdrawalPath.Point
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.ui.components.BoardViewport
import com.amowpuzzle.aitricker.ui.components.detectBoardGestures
import com.amowpuzzle.aitricker.ui.components.unambiguousTargetIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

/** Playable study; state stays in memory and never reaches campaign preferences. */
@Composable
fun WithdrawalPreviewBoard(
    fixture: PreviewFixture,
    showGrid: Boolean,
    hintRequest: Int,
    fitRequest: Int,
    resetRequest: Int,
    modifier: Modifier = Modifier,
    slowMotion: Boolean = false,
    onStats: (PreviewStats) -> Unit
) {
    key(fixture, resetRequest) {
        val level by produceState<Level?>(null) {
            value = withContext(Dispatchers.Default) { previewLevel(fixture) }
        }
        val ready = level
        if (ready == null) {
            SideEffect { onStats(PreviewStats(busy = true, message = "Preparing the study?")) }
            Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            PreviewSession(ready, showGrid, hintRequest, fitRequest, slowMotion, modifier, onStats)
        }
    }
}

private data class Flight(val arrow: ArrowPath, val distance: Float, val blocked: Boolean)

@Composable
private fun PreviewSession(
    level: Level, showGrid: Boolean, hintRequest: Int, fitRequest: Int,
    slowMotion: Boolean, modifier: Modifier, onStats: (PreviewStats) -> Unit
) {
    var active by remember { mutableStateOf(level.arrows) }
    var lives by remember { mutableStateOf(3) }
    var flight by remember { mutableStateOf<Flight?>(null) }
    var hintId by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("Find an open tip. Pinch to inspect the bends.") }
    var viewport by remember { mutableStateOf(BoardViewport()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val geometry = remember(level) { level.arrows.associate { it.id to previewPath(it) } }
    val outlines = remember(geometry) { geometry.mapValues { (_, path) -> path.window(0f) } }
    val staticPaths = remember(outlines) { outlines.mapValues { (_, points) -> points.toComposePath() } }
    val sizeRef by rememberUpdatedState(canvasSize)
    val slowRef by rememberUpdatedState(slowMotion)

    fun hint() {
        if (flight != null || lives == 0 || active.isEmpty()) return
        val arrow = PuzzleSolver.findHint(level, active)
        hintId = arrow?.id
        message = if (arrow == null) "No free path under this motion rule." else "Follow the highlighted tip."
        if (arrow != null && maxOf(level.cols, level.rows) > 30 && canvasSize.width > 0) {
            val layout = PreviewLayout(level, canvasSize)
            val headX = layout.left + (arrow.head.col + .5f) * layout.cell
            val headY = layout.top + (arrow.head.row + .5f) * layout.cell
            val scale = maxOf(viewport.scale, 3f)
            viewport = BoardViewport(scale).transform(
                1f, (canvasSize.width / 2f - headX) * scale,
                (canvasSize.height / 2f - headY) * scale,
                canvasSize.width / 2f, canvasSize.height / 2f,
                canvasSize.width.toFloat(), canvasSize.height.toFloat(),
                layout.cell * level.cols, layout.cell * level.rows
            )
        }
    }
    val initialHint = remember { hintRequest }
    val initialFit = remember { fitRequest }
    LaunchedEffect(hintRequest) { if (hintRequest != initialHint) hint() }
    LaunchedEffect(fitRequest) { if (fitRequest != initialFit) viewport = BoardViewport() }

    fun tap(point: Offset) {
        if (flight != null || lives == 0 || active.isEmpty() || canvasSize.width == 0) return
        val layout = PreviewLayout(level, canvasSize)
        val x = (viewport.boardX(point.x, canvasSize.width.toFloat()) - layout.left) / layout.cell
        val y = (viewport.boardY(point.y, canvasSize.height.toFloat()) - layout.top) / layout.cell
        val index = unambiguousTargetIndex(
            active.map { distanceToPath(x, y, outlines.getValue(it.id)) }, .43f, .10f
        ) ?: return
        val arrow = active[index]
        val grid = Occupancy.of(level.cols, level.rows, active)
        val free = CollisionEngine.isFree(arrow, grid, level.movementRule)
        val path = geometry.getValue(arrow.id)
        val travel = if (free) {
            // The tail leaves the visible viewport, even on a very tall/wide window.
            val inverseExtent = maxOf(canvasSize.width, canvasSize.height) / layout.cell
            path.length + inverseExtent + 4f
        } else {
            min(.35f, CollisionEngine.freeCellsAhead(arrow, grid, level.movementRule) + .12f)
        }
        // Serial departures reserve every occupied path until the visible motion ends.
        flight = Flight(arrow, travel, blocked = !free)
        hintId = null
        if (!free) {
            lives--
            message = if (lives == 0) "Out of hearts. Reset to try this study again." else "That path is blocked. Look along its tip."
        } else message = "Watch the tail follow each bend."
        scope.launch {
            progress.snapTo(0f)
            val duration = if (slowRef) 2400 else (360 + path.length * 5f).toInt().coerceIn(360, 700)
            progress.animateTo(1f, tween(if (free) duration else if (slowRef) 650 else 150, easing = LinearEasing))
            if (free) {
                active = active.filterNot { it.id == arrow.id }
                message = if (active.isEmpty()) "Study complete. Every path is clear." else "A clear path. Find the next one."
            } else progress.animateTo(0f, tween(if (slowRef) 650 else 150, easing = LinearEasing))
            flight = null
        }
    }
    val tapRef by rememberUpdatedState(::tap)
    val viewportRef by rememberUpdatedState(viewport)
    SideEffect {
        onStats(PreviewStats(active.size, level.arrowCount, lives, flight != null, active.isEmpty(), message))
    }

    Canvas(modifier.clipToBounds().onSizeChanged { canvasSize = it }
        .semantics { contentDescription = "Motion study board, " + active.size + " arrows remaining. Pinch to zoom; use Hint to highlight an open path." }
        .pointerInput(level) {
            detectBoardGestures(
                onTransform = { anchor, pan, zoom ->
                    val current = sizeRef
                    val layout = PreviewLayout(level, current)
                    viewport = viewportRef.transform(
                        zoom, pan.x, pan.y, anchor.x, anchor.y,
                        current.width.toFloat(), current.height.toFloat(),
                        layout.cell * level.cols, layout.cell * level.rows
                    )
                },
                onTap = { tapRef(it) }
            )
        }
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val layout = PreviewLayout(level, IntSize(size.width.toInt(), size.height.toInt()))
        withTransform({
            translate(viewport.panX, viewport.panY)
            scale(viewport.scale, viewport.scale, center)
            translate(layout.left, layout.top)
            scale(layout.cell, layout.cell, Offset.Zero)
        }) {
            if (showGrid) {
                for (row in 0 until level.rows) for (col in 0 until level.cols) {
                    drawCircle(Color(0xFFBBC7D5), .035f, Offset(col + .5f, row + .5f))
                }
            }
            val moving = flight
            val stroke = Stroke(.19f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            active.forEach { arrow ->
                val path = geometry.getValue(arrow.id)
                val travel = if (moving?.arrow?.id == arrow.id) moving.distance * progress.value else 0f
                val body = if (travel == 0f) staticPaths.getValue(arrow.id) else path.window(travel).toComposePath()
                val head = Point(path.head.x + arrow.exit.dCol * travel, path.head.y + arrow.exit.dRow * travel)
                val headPath = previewHead(head, arrow)
                val hue = if (moving?.arrow?.id == arrow.id && moving.blocked) Color(0xFFCA4C57) else PreviewHues[arrow.id.mod(PreviewHues.size)]
                if (hintId == arrow.id) {
                    drawPath(body, Color(0xFFFFC64D), style = Stroke(.48f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(Color(0xFFFFC64D).copy(alpha = .5f), .50f, Offset(head.x, head.y))
                }
                withTransform({ translate(0f, .06f) }) {
                    drawPath(body, Color(0xFF24344A).copy(alpha = .13f), style = Stroke(.23f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(headPath, Color(0xFF24344A).copy(alpha = .13f))
                }
                withTransform({ translate(0f, .035f) }) {
                    val shade = Color(hue.red * .70f, hue.green * .70f, hue.blue * .70f)
                    drawPath(body, shade, style = stroke)
                    drawPath(headPath, shade)
                }
                drawPath(body, hue, style = stroke)
                drawPath(headPath, hue)
                if (layout.cell * viewport.scale >= 10f) {
                    drawPath(body, Color.White.copy(alpha = .32f), style = Stroke(.035f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

private val PreviewHues = listOf(
    Color(0xFF4169CE), Color(0xFF258E88), Color(0xFFBE5B43), Color(0xFF9A6D19),
    Color(0xFF7652B0), Color(0xFF50824A), Color(0xFFB84979)
)

private class PreviewLayout(level: Level, size: IntSize) {
    val cell = min(size.width / (level.cols + 1.6f), size.height / (level.rows + 1.6f)).coerceAtLeast(.001f)
    val left = (size.width - level.cols * cell) / 2f
    val top = (size.height - level.rows * cell) / 2f
}

private fun previewPath(arrow: ArrowPath): WithdrawalPath {
    val points = arrow.cells.map { Point(it.col + .5f, it.row + .5f) }
    val first = points.first()
    val next = points.getOrElse(1) { Point(first.x + arrow.exit.dCol, first.y + arrow.exit.dRow) }
    val tail = Point(first.x - (next.x - first.x) * .12f, first.y - (next.y - first.y) * .12f)
    return WithdrawalPath(listOf(tail) + points, Point(arrow.exit.dCol.toFloat(), arrow.exit.dRow.toFloat()), .18f)
}

private fun List<Point>.toComposePath(): Path = Path().also { path ->
    firstOrNull()?.let { path.moveTo(it.x, it.y) }
    for (i in 1 until size) path.lineTo(this[i].x, this[i].y)
}

private fun previewHead(head: Point, arrow: ArrowPath): Path {
    val dx = arrow.exit.dCol.toFloat()
    val dy = arrow.exit.dRow.toFloat()
    return Path().apply {
        moveTo(head.x + dx * .30f, head.y + dy * .30f)
        lineTo(head.x - dx * .18f - dy * .24f, head.y - dy * .18f + dx * .24f)
        lineTo(head.x - dx * .18f + dy * .24f, head.y - dy * .18f - dx * .24f)
        close()
    }
}

private fun distanceToPath(x: Float, y: Float, points: List<Point>): Float =
    points.zipWithNext().minOfOrNull { (a, b) ->
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0f) 0f else (((x - a.x) * dx + (y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
        hypot(x - a.x - t * dx, y - a.y - t * dy)
    } ?: Float.POSITIVE_INFINITY
