package com.amowpuzzle.aitricker.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.amowpuzzle.aitricker.domain.geometry.WithdrawalPath
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.MovementRule
import kotlin.math.min

/** Cached pixel geometry. Static and moving strokes use the exact same rounded centerline. */
class BoardGeometry(
    val level: Level,
    private val availableWidth: Float,
    private val availableHeight: Float
) {
    private val darkBoard = level.presentation.usesDarkBoard
    val cellSize: Float = min(availableWidth / level.cols, availableHeight / level.rows)
    val boardWidth: Float = cellSize * level.cols
    val boardHeight: Float = cellSize * level.rows
    val originX: Float = (availableWidth - boardWidth) / 2f
    val originY: Float = (availableHeight - boardHeight) / 2f

    val strokeWidth: Float = cellSize * if (darkBoard) 0.28f else 0.20f
    val cornerRadius: Float = cellSize * if (darkBoard) 0.18f else 0.30f
    val specularStroke: Stroke = Stroke(
        width = cellSize * 0.055f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    private val headLength: Float = cellSize * if (darkBoard) 0.62f else 0.52f
    private val headHalfWidth: Float = cellSize * if (darkBoard) 0.34f else 0.33f
    private val tipOvershoot: Float = cellSize * if (darkBoard) 0.32f else 0.42f
    private val tailOvershoot: Float = cellSize * if (darkBoard) 0.12f else 0.26f
    val tapTolerance: Float = cellSize * if (darkBoard) 0.42f else 0.62f

    private val bodies = HashMap<Int, Path>(level.arrowCount)
    private val heads = HashMap<Int, Path>(level.arrowCount)
    private val segments = HashMap<Int, List<Pair<Offset, Offset>>>(level.arrowCount)
    private val withdrawals = HashMap<Int, WithdrawalPath>(level.arrowCount)
    private val movingBodies = HashMap<Int, Path>()
    private val escapeDistances = HashMap<Int, Float>(level.arrowCount)

    init {
        level.arrows.forEach { arrow ->
            val points = outlinePoints(arrow)
            val curve = WithdrawalPath(
                points = points.map { WithdrawalPath.Point(it.x, it.y) },
                direction = WithdrawalPath.Point(arrow.exit.dCol.toFloat(), arrow.exit.dRow.toFloat()),
                cornerRadius = cornerRadius
            )
            withdrawals[arrow.id] = curve
            val frozen = curve.window(0f)
            bodies[arrow.id] = polyline(frozen)
            heads[arrow.id] = arrowHead(arrow)
            val head = center(arrow.head)
            val tip = head + Offset(arrow.exit.dCol * tipOvershoot, arrow.exit.dRow * tipOvershoot)
            val centerline = frozen.map { Offset(it.x, it.y) }
            segments[arrow.id] = centerline.zipWithNext() + if (darkBoard) listOf(head to tip) else emptyList()
            escapeDistances[arrow.id] = if (level.movementRule == MovementRule.SWEPT_TRANSLATION) {
                // Rigid legacy arrows clear the canvas with their trailing extremum.
                when {
                    arrow.exit.dCol > 0 -> availableWidth - points.minOf { it.x }
                    arrow.exit.dCol < 0 -> points.maxOf { it.x }
                    arrow.exit.dRow > 0 -> availableHeight - points.minOf { it.y }
                    else -> points.maxOf { it.y }
                } + cellSize
            } else {
                val headToEdge = when {
                    arrow.exit.dCol > 0 -> availableWidth - head.x
                    arrow.exit.dCol < 0 -> head.x
                    arrow.exit.dRow > 0 -> availableHeight - head.y
                    else -> head.y
                }
                // At this distance the entire fixed-length window, including its tail, is off-screen.
                headToEdge + curve.length + cellSize
            }
        }
    }

    fun center(cell: Cell): Offset = Offset(
        originX + (cell.col + 0.5f) * cellSize,
        originY + (cell.row + 0.5f) * cellSize
    )

    fun bodyOf(arrow: ArrowPath): Path = bodies.getValue(arrow.id)
    fun headOf(arrow: ArrowPath): Path = heads.getValue(arrow.id)
    fun escapeDistance(arrow: ArrowPath): Float = escapeDistances.getValue(arrow.id)

    /**
     * C([travel, travel + length]) in ORIGINAL canvas coordinates. Do not translate
     * this body again: only the separate head moves by exit * travel. A corner
     * remains frozen until the tail reaches it, so no body sweeps across its neighbors.
     */
    fun withdrawnBody(arrow: ArrowPath, travel: Float): Path {
        if (!travel.isFinite() || travel <= 0f) return bodyOf(arrow)
        val path = movingBodies.getOrPut(arrow.id) { Path() }
        return polyline(withdrawals.getValue(arrow.id).window(travel), path)
    }

    fun arrowAt(point: Offset, candidates: List<ArrowPath>): ArrowPath? {
        if (darkBoard) {
            val index = unambiguousTargetIndex(
                candidates.map { distanceTo(it, point) }, tapTolerance, cellSize * 0.10f
            ) ?: return null
            return candidates[index]
        }
        var best: ArrowPath? = null
        var bestDistance = Float.MAX_VALUE
        for (arrow in candidates) {
            val distance = distanceTo(arrow, point)
            if (distance < bestDistance) {
                bestDistance = distance
                best = arrow
            }
        }
        return if (bestDistance <= tapTolerance) best else null
    }

    private fun distanceTo(arrow: ArrowPath, point: Offset): Float {
        var best = Float.MAX_VALUE
        segments[arrow.id]?.forEach { (from, to) ->
            best = min(best, distanceToSegment(point, from, to))
        }
        return best
    }

    private fun outlinePoints(arrow: ArrowPath): List<Offset> {
        val centres = arrow.cells.map { center(it) }
        val first = centres.first()
        val inward = if (centres.size >= 2) centres[1] - first else
            Offset(arrow.exit.dCol * cellSize, arrow.exit.dRow * cellSize)
        val length = inward.getDistance()
        val tail = if (length == 0f) first else first - inward / length * tailOvershoot
        return listOf(tail) + centres
    }

    /** Already-rounded points; rounding again would change the curve during departure. */
    private fun polyline(points: List<WithdrawalPath.Point>, target: Path = Path()): Path {
        target.reset()
        if (points.isEmpty()) return target
        target.moveTo(points.first().x, points.first().y)
        for (index in 1 until points.size) target.lineTo(points[index].x, points[index].y)
        return target
    }

    private fun arrowHead(arrow: ArrowPath): Path {
        val head = center(arrow.head)
        val dir = arrow.exit
        val tipX = head.x + dir.dCol * tipOvershoot
        val tipY = head.y + dir.dRow * tipOvershoot
        val bx = tipX - dir.dCol * headLength
        val by = tipY - dir.dRow * headLength
        val px = -dir.dRow * headHalfWidth
        val py = dir.dCol * headHalfWidth
        return Path().apply {
            moveTo(tipX, tipY)
            lineTo(bx + px, by + py)
            lineTo(bx - px, by - py)
            close()
        }
    }

    private fun distanceToSegment(point: Offset, from: Offset, to: Offset): Float {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return (point - from).getDistance()
        val t = (((point.x - from.x) * dx + (point.y - from.y) * dy) / lenSq).coerceIn(0f, 1f)
        return (point - Offset(from.x + t * dx, from.y + t * dy)).getDistance()
    }
}