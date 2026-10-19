package com.amowpuzzle.aitricker.domain.geometry

import com.amowpuzzle.aitricker.domain.geometry.WithdrawalPath.Point
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WithdrawalPathTest {
    private val right = Point(1f, 0f)
    private val elbow = listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f))

    @Test
    fun `straight path uses distance from tail and extends beyond its head`() {
        val path = WithdrawalPath(listOf(Point(2f, 3f), Point(12f, 3f)), right, 3f)
        assertEquals(10f, path.length, 0f)
        assertEquals(Point(12f, 3f), path.head)
        assertPoint(Point(2f, 3f), path.pointAt(0f))
        assertPoint(Point(6f, 3f), path.pointAt(4f))
        assertPoint(path.head, path.pointAt(path.length))
        assertPoint(Point(17f, 3f), path.pointAt(15f))
        assertEquals(listOf(Point(6f, 3f), Point(12f, 3f), Point(16f, 3f)), path.window(4f))
    }

    @Test
    fun `rounded elbow has correct samples before inside and after the bend`() {
        val path = WithdrawalPath(elbow, Point(0f, 1f), 2f)
        val initial = path.window(0f)
        assertEquals(elbow.first(), initial.first())
        assertEquals(elbow.last(), initial.last())
        assertTrue(Point(8f, 0f) in initial)
        assertTrue(Point(9.5f, 0.5f) in initial)
        assertTrue(Point(10f, 2f) in initial)
        assertFalse(elbow[1] in initial)
        // The two straight parts are 8 units each; symmetry splits the quadratic
        // bend's arc at its known t=1/2 point, without assuming its sampled length.
        val curveLength = path.length - 16f
        assertTrue(curveLength > 2.8f && curveLength < 4f)
        assertPoint(Point(7f, 0f), path.pointAt(7f))
        assertPoint(Point(9.5f, 0.5f), path.pointAt(8f + curveLength / 2f))
        assertPoint(Point(10f, 3f), path.pointAt(9f + curveLength))
        assertPoint(Point(10f, 12f), path.pointAt(path.length + 2f))
        assertTrue("The corner has enough samples for drawing", initial.size >= 24)
    }

    @Test
    fun `straight L U and zigzag windows retain length in every cardinal direction`() {
        val shapes = listOf(
            listOf(Point(0f, 0f), Point(8f, 0f)),
            elbow,
            listOf(Point(0f, 0f), Point(0f, 8f), Point(6f, 8f), Point(6f, 0f)),
            listOf(Point(0f, 0f), Point(4f, 0f), Point(4f, 5f), Point(8f, 5f), Point(8f, 2f), Point(12f, 2f))
        )
        for (shape in shapes) for (turns in 0..3) {
            val points = shape.map { rotate(it, turns) }
            val finalDelta = Point(points.last().x - points[points.lastIndex - 1].x, points.last().y - points[points.lastIndex - 1].y)
            val magnitude = hypot(finalDelta.x.toDouble(), finalDelta.y.toDouble())
            val direction = Point((finalDelta.x / magnitude).toFloat(), (finalDelta.y / magnitude).toFloat())
            val path = WithdrawalPath(points, direction, 0.9f)
            for (step in 0..160) {
                val travel = path.length * step / 80f
                val window = path.window(travel)
                assertTrue(window.all { it.x.isFinite() && it.y.isFinite() })
                assertEquals("Constant length at rotation $turns, step $step", path.length.toDouble(), measure(window), 0.00005)
                assertPoint(path.pointAt(travel), window.first())
                assertPoint(Point(path.head.x + direction.x * travel, path.head.y + direction.y * travel), window.last())
            }
        }
    }

    @Test
    fun `unconsumed bends stay in place as the tail withdraws`() {
        val path = WithdrawalPath(elbow, Point(0f, 1f), 2f)
        val initial = path.window(0f)
        val moving = path.window(1f)
        assertPoint(Point(1f, 0f), moving.first())
        assertTrue("Remaining centerline samples do not morph or translate", moving.containsAll(initial.drop(1)))
        assertPoint(Point(10f, 11f), moving.last())
        val afterBend = path.window(path.length - 4f)
        assertTrue(afterBend.all { it.x == 10f })
        assertEquals(path.length.toDouble(), measure(afterBend), 0.00001)
    }

    @Test
    fun `once the original length has passed the window is entirely on the exit ray`() {
        for (turns in 0..3) {
            val direction = rotate(Point(0f, 1f), turns)
            val path = WithdrawalPath(elbow.map { rotate(it, turns) }, direction, 2f)
            for (travel in listOf(path.length, path.length + 0.1f, path.length * 3f)) {
                val window = path.window(travel)
                assertEquals(2, window.size)
                assertPoint(Point(path.head.x + direction.x * (travel - path.length), path.head.y + direction.y * (travel - path.length)), window.first())
                assertPoint(Point(path.head.x + direction.x * travel, path.head.y + direction.y * travel), window.last())
                assertEquals(path.length.toDouble(), measure(window), 0.00001)
            }
        }
    }

    @Test
    fun `corner trim cannot consume more than half either adjoining segment`() {
        val path = WithdrawalPath(listOf(Point(0f, 0f), Point(2f, 0f), Point(2f, 2f), Point(4f, 2f)), right, 100f)
        val initial = path.window(0f)
        assertTrue(Point(1f, 0f) in initial)
        assertTrue(Point(2f, 1f) in initial)
        assertTrue(Point(3f, 2f) in initial)
        assertTrue(initial.all { it.x in 0f..4f && it.y in 0f..2f })
        assertTrue(initial.zipWithNext().all { (first, second) -> second.x >= first.x && second.y >= first.y })
        assertEquals(1, initial.count { it == Point(2f, 1f) })
    }

    @Test
    fun `zero radius and collinear points preserve their original centerline`() {
        assertEquals(elbow, WithdrawalPath(elbow, Point(0f, 1f), 0f).window(0f))
        val collinear = listOf(Point(0f, 0f), Point(2f, 0f), Point(5f, 0f))
        val straight = WithdrawalPath(collinear, right, 50f)
        assertEquals(collinear, straight.window(0f))
        assertEquals(5f, straight.length, 0f)
        val reversal = WithdrawalPath(listOf(Point(0f, 0f), Point(4f, 0f), Point(2f, 0f)), Point(-1f, 0f), 50f)
        assertEquals(6f, reversal.length, 0f)
        assertPoint(Point(3f, 0f), reversal.pointAt(5f))
    }

    @Test
    fun `repeated points including signed zero do not create invalid corners`() {
        val repeated = listOf(Point(-0f, 0f), Point(0f, -0f), Point(0f, 0f), Point(10f, 0f), Point(10f, 0f), Point(10f, 10f), Point(10f, 10f))
        val path = WithdrawalPath(repeated, Point(0f, 1f), 2f)
        val clean = WithdrawalPath(elbow, Point(0f, 1f), 2f)
        assertEquals(clean.length, path.length, 0f)
        for (step in 0..100) {
            val travel = step / 4f
            assertEquals(measure(clean.window(travel)), measure(path.window(travel)), 0.00001)
            assertPoint(clean.pointAt(travel), path.pointAt(travel))
        }
    }

    @Test
    fun `one point or all identical points produce a zero length moving point`() {
        for (points in listOf(listOf(Point(3f, 4f)), List(5) { Point(3f, 4f) })) {
            val path = WithdrawalPath(points, Point(0f, -2f), 8f)
            assertEquals(0f, path.length, 0f)
            assertEquals(listOf(Point(3f, 4f)), path.window(0f))
            assertEquals(listOf(Point(3f, 1f)), path.window(3f))
            assertEquals(Point(3f, 1f), path.pointAt(3f))
        }
    }

    @Test
    fun `direction is normalized and negative travel clamps to the original path`() {
        val path = WithdrawalPath(listOf(Point(0f, 0f), Point(3f, 4f)), Point(30f, 40f), 0f)
        assertEquals(5f, path.length, 0f)
        assertPoint(Point(6f, 8f), path.window(5f).last())
        assertPoint(Point(6f, 8f), path.pointAt(10f))
        assertEquals(path.window(0f), path.window(-10f))
        assertEquals(path.pointAt(0f), path.pointAt(-10f))
    }

    @Test
    fun `nonfinite inputs invalid radius and missing direction are rejected`() {
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertInvalid { WithdrawalPath(listOf(Point(invalid, 0f)), right, 0f) }
            assertInvalid { WithdrawalPath(listOf(Point(0f, invalid)), right, 0f) }
            assertInvalid { WithdrawalPath(elbow, Point(invalid, 0f), 0f) }
            assertInvalid { WithdrawalPath(elbow, Point(0f, invalid), 0f) }
            assertInvalid { WithdrawalPath(elbow, right, invalid) }
            val path = WithdrawalPath(elbow, right, 0f)
            assertInvalid { path.pointAt(invalid) }
            assertInvalid { path.window(invalid) }
        }
        assertInvalid { WithdrawalPath(emptyList(), right, 0f) }
        assertInvalid { WithdrawalPath(elbow, Point(0f, 0f), 0f) }
        assertInvalid { WithdrawalPath(elbow, right, -1f) }
    }

    @Test
    fun `finite extremes either produce finite geometry or reject overflow`() {
        val tiny = WithdrawalPath(listOf(Point(0f, 0f), Point(Float.MIN_VALUE, 0f)), Point(Float.MIN_VALUE, 0f), 0f)
        assertEquals(Float.MIN_VALUE, tiny.length, 0f)
        assertEquals(Point(Float.MIN_VALUE, 0f), tiny.pointAt(tiny.length))
        val wideDirection = WithdrawalPath(elbow, Point(Float.MAX_VALUE, Float.MAX_VALUE), 1f)
        assertTrue(wideDirection.window(10f).all { it.x.isFinite() && it.y.isFinite() })
        assertInvalid { WithdrawalPath(listOf(Point(-Float.MAX_VALUE, 0f), Point(Float.MAX_VALUE, 0f)), right, 0f) }
        val atLimit = WithdrawalPath(listOf(Point(Float.MAX_VALUE, 0f)), right, 0f)
        assertInvalid { atLimit.pointAt(Float.MAX_VALUE) }
        assertInvalid { atLimit.window(Float.MAX_VALUE) }
    }

    @Test
    fun `caller mutations cannot change a constructed path`() {
        val input = elbow.toMutableList()
        val path = WithdrawalPath(input, Point(0f, 1f), 2f)
        val original = path.window(0f)
        input.clear()
        assertEquals(original, path.window(0f))
        (original as MutableList<Point>).clear()
        assertEquals(elbow.first(), path.window(0f).first())
        assertEquals(elbow.last(), path.head)
    }

    private fun rotate(point: Point, turns: Int): Point {
        var result = point
        repeat(turns) { result = Point(-result.y, result.x) }
        return result
    }

    private fun measure(points: List<Point>): Double = points.zipWithNext().sumOf { (first, second) ->
        hypot(second.x.toDouble() - first.x, second.y.toDouble() - first.y)
    }

    private fun assertPoint(expected: Point, actual: Point) {
        assertEquals(expected.x, actual.x, 0.00001f)
        assertEquals(expected.y, actual.y, 0.00001f)
    }

    private fun assertInvalid(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }
}