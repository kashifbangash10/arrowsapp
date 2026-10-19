package com.amowpuzzle.aitricker.ui.components

import com.amowpuzzle.aitricker.presentation.game.FeedbackTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowMotionTimingTest {
    @Test
    fun `long flights take longer but always stay within the feedback budget`() {
        val durations = listOf(0f, 8f, 20f, 40f, 60f, 120f, 10000f)
            .map(ArrowMotionTiming::travelDurationMs)
        assertTrue(durations.zipWithNext().all { (earlier, later) -> later >= earlier })
        assertTrue(durations.all { it in FeedbackTiming.MIN_ESCAPE_MS..FeedbackTiming.MAX_ESCAPE_MS })
        assertTrue(ArrowMotionTiming.travelDurationMs(60f) > ArrowMotionTiming.travelDurationMs(20f))
        assertEquals(FeedbackTiming.MAX_ESCAPE_MS, durations.last())
        assertTrue(FeedbackTiming.PRESS_MS in 70..90)
    }

    @Test
    fun `feedback remains defined for zero invalid or very large inputs`() {
        for (distance in listOf(-10f, 0f, Float.NaN, Float.POSITIVE_INFINITY, Float.MAX_VALUE)) {
            assertTrue(ArrowMotionTiming.travelDurationMs(distance) in
                FeedbackTiming.MIN_ESCAPE_MS..FeedbackTiming.MAX_ESCAPE_MS)
        }
        for (progress in listOf(-1f, 0f, 1f, 2f, Float.NaN)) {
            assertTrue(ArrowMotionTiming.strokeMultiplier(progress) in 1f..1.45f)
            assertTrue(ArrowMotionTiming.blockedTravelCells(1, progress).isFinite())
        }
    }

    @Test
    fun `flush blocked arrows never shift and any allowed nudge fits free clearance`() {
        for (frame in 0..300) {
            val progress = frame / 300f
            assertEquals(0f, ArrowMotionTiming.blockedTravelCells(0, progress), 0f)
            assertEquals(0f, ArrowMotionTiming.blockedTravelCells(-1, progress), 0f)
            for (clearance in 1..10) {
                val distance = ArrowMotionTiming.blockedTravelCells(clearance, progress)
                assertTrue(distance >= 0f && distance <= 0.40001f && distance < clearance)
            }
        }
    }

    @Test
    fun `blocked travel returns exactly to origin with gentle start and stop`() {
        assertEquals(0f, ArrowMotionTiming.blockedTravelCells(1, 0f), 0f)
        assertEquals(0f, ArrowMotionTiming.blockedTravelCells(1, 1f), 0f)
        assertTrue(ArrowMotionTiming.blockedTravelCells(1, 1f / 300f) < 0.001f)
        assertTrue(ArrowMotionTiming.blockedTravelCells(1, 299f / 300f) < 0.001f)
        assertEquals(
            ArrowMotionTiming.blockedTravelCells(1, 0.25f),
            ArrowMotionTiming.blockedTravelCells(1, 0.75f), 0.00001f
        )
    }

    @Test
    fun `white press acknowledgement is immediate finite and ends before travel feedback`() {
        assertEquals(1f, ArrowMotionTiming.blockedPulse(0f), 0f)
        assertEquals(0f, ArrowMotionTiming.blockedPulse(1f), 0f)
        val pressEnd = FeedbackTiming.PRESS_MS.toFloat() / FeedbackTiming.BLOCKED_MS
        assertEquals(0f, ArrowMotionTiming.blockedPulse(pressEnd), 0.00001f)
        // At maximum emphasis a .20-cell line remains far inside its own half-cell corridor.
        assertTrue(0.20f * ArrowMotionTiming.strokeMultiplier(1f) / 2f < 0.18f)
    }
}