package com.amowpuzzle.aitricker.domain.geometry

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * A rounded, tail-to-head centerline followed by an infinite ray in [direction].
 *
 * [window] advances a fixed-length section along that curve. Bends disappear only
 * when the tail passes them, while the head advances along the exit ray. This is
 * the production route-following geometry; collision remains in CollisionEngine.
 *
 * Coordinates and distances share the caller's units. Quadratic corners use 24
 * deterministic subdivisions; [length] measures the resulting polyline rather
 * than the unrounded input. Adjacent duplicate points are ignored. Collinear
 * vertices, including reversals, retain their original centerline.
 */
class WithdrawalPath(
    points: List<Point>,
    direction: Point,
    cornerRadius: Float
) {
    data class Point(val x: Float, val y: Float)

    val length: Float
    val head: Point

    private val centerline: List<Point>
    private val distances: DoubleArray
    private val exitX: Double
    private val exitY: Double

    init {
        require(points.isNotEmpty()) { "A withdrawal path needs at least one point" }
        require(points.all { it.x.isFinite() && it.y.isFinite() }) { "Points must be finite" }
        require(cornerRadius.isFinite() && cornerRadius >= 0f) { "Corner radius must be finite and nonnegative" }
        require(direction.x.isFinite() && direction.y.isFinite()) { "Exit direction must be finite" }
        val exitLength = hypot(direction.x.toDouble(), direction.y.toDouble())
        require(exitLength > 0.0) { "Exit direction must be nonzero" }
        exitX = direction.x / exitLength
        exitY = direction.y / exitLength

        val distinct = ArrayList<Point>(points.size)
        points.forEach { distinct.addDistinct(it) }
        head = distinct.last()
        centerline = roundCorners(distinct, cornerRadius.toDouble())
        distances = DoubleArray(centerline.size)
        for (index in 1 until centerline.size) {
            distances[index] = distances[index - 1] + distance(centerline[index - 1], centerline[index])
        }
        val measuredLength = distances.last()
        require(measuredLength <= Float.MAX_VALUE.toDouble()) { "Path length must fit in a finite Float" }
        length = measuredLength.toFloat()
        // Keep the public Float endpoint exactly at the head. This adjustment is
        // at most Float rounding error, with all interpolation still in Double.
        if (measuredLength > 0.0) {
            val scale = length.toDouble() / measuredLength
            for (index in distances.indices) distances[index] *= scale
            distances[distances.lastIndex] = length.toDouble()
        }
    }

    /** Point at a distance from the original tail; negative distances clamp to zero. */
    fun pointAt(distance: Float): Point {
        require(distance.isFinite()) { "Distance must be finite" }
        return pointAtDistance(distance.coerceAtLeast(0f).toDouble())
    }

    /**
     * Visible centerline C([travel, travel + length]), in tail-to-head order.
     * The last point is always head + travel * normalizedDirection. At travel >=
     * length, the complete window lies on the ray and contains just its endpoints.
     * A zero-length path returns one point. Negative travel clamps to zero.
     */
    fun window(travel: Float): List<Point> {
        require(travel.isFinite()) { "Travel must be finite" }
        val start = travel.coerceAtLeast(0f).toDouble()
        if (length == 0f) return listOf(rayPoint(start))
        val result = ArrayList<Point>(centerline.size + 1)
        result.addDistinct(pointAtDistance(start))
        for (index in centerline.indices) {
            if (distances[index] > start) result.addDistinct(centerline[index])
        }
        result.addDistinct(rayPoint(start))
        return result
    }

    private fun pointAtDistance(distance: Double): Point {
        if (distance >= length.toDouble()) return rayPoint(distance - length.toDouble())
        if (distance <= 0.0) return centerline.first()
        var low = 0
        var high = distances.lastIndex
        while (low + 1 < high) {
            val middle = (low + high) ushr 1
            if (distances[middle] <= distance) low = middle else high = middle
        }
        val fraction = (distance - distances[low]) / (distances[high] - distances[low])
        val from = centerline[low]
        val to = centerline[high]
        return finitePoint(
            from.x.toDouble() + (to.x.toDouble() - from.x) * fraction,
            from.y.toDouble() + (to.y.toDouble() - from.y) * fraction
        )
    }

    private fun rayPoint(distance: Double): Point = finitePoint(
        head.x.toDouble() + exitX * distance,
        head.y.toDouble() + exitY * distance
    )

    private companion object {
        const val CORNER_STEPS = 24

        fun roundCorners(points: List<Point>, radius: Double): List<Point> {
            if (points.size < 3 || radius == 0.0) return points.toList()
            val rounded = ArrayList<Point>()
            rounded.add(points.first())
            for (index in 1 until points.lastIndex) {
                val previous = points[index - 1]
                val corner = points[index]
                val next = points[index + 1]
                val incomingLength = distance(previous, corner)
                val outgoingLength = distance(corner, next)
                val incomingX = (corner.x.toDouble() - previous.x) / incomingLength
                val incomingY = (corner.y.toDouble() - previous.y) / incomingLength
                val outgoingX = (next.x.toDouble() - corner.x) / outgoingLength
                val outgoingY = (next.y.toDouble() - corner.y) / outgoingLength
                if (abs(incomingX * outgoingY - incomingY * outgoingX) <= 1e-12) {
                    rounded.addDistinct(corner)
                    continue
                }
                val trim = min(radius, min(incomingLength, outgoingLength) * 0.5)
                val entryX = corner.x.toDouble() - incomingX * trim
                val entryY = corner.y.toDouble() - incomingY * trim
                val exitX = corner.x.toDouble() + outgoingX * trim
                val exitY = corner.y.toDouble() + outgoingY * trim
                rounded.addDistinct(finitePoint(entryX, entryY))
                for (step in 1..CORNER_STEPS) {
                    val t = step.toDouble() / CORNER_STEPS
                    val remaining = 1.0 - t
                    rounded.addDistinct(finitePoint(
                        remaining * remaining * entryX + 2.0 * remaining * t * corner.x + t * t * exitX,
                        remaining * remaining * entryY + 2.0 * remaining * t * corner.y + t * t * exitY
                    ))
                }
            }
            rounded.addDistinct(points.last())
            return rounded
        }

        fun distance(first: Point, second: Point): Double = hypot(
            second.x.toDouble() - first.x,
            second.y.toDouble() - first.y
        )

        fun finitePoint(x: Double, y: Double): Point {
            val result = Point(x.toFloat(), y.toFloat())
            require(result.x.isFinite() && result.y.isFinite()) { "Requested point exceeds finite Float coordinates" }
            return result
        }

        fun MutableList<Point>.addDistinct(point: Point) {
            if (isEmpty() || last().x != point.x || last().y != point.y) add(point)
        }
    }
}