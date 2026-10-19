package com.amowpuzzle.aitricker.ui.components

import com.amowpuzzle.aitricker.presentation.game.FeedbackTiming
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Feedback in grid units, independent of display density, fit size and camera zoom. */
internal object ArrowMotionTiming {
    fun travelDurationMs(travelCells: Float): Int {
        val distance = if (travelCells.isFinite()) travelCells.coerceAtLeast(0f) else 0f
        return (280f + distance.coerceAtMost(1000f) * 7f).roundToInt()
            .coerceIn(FeedbackTiming.MIN_ESCAPE_MS, FeedbackTiming.MAX_ESCAPE_MS)
    }

    /** Zero clearance means a stationary pulse; positive clearance permits a small reversible advance. */
    fun blockedTravelCells(cellsAhead: Int, progress: Float): Float {
        if (cellsAhead <= 0) return 0f
        val p = finiteFraction(progress)
        if (p == 0f || p == 1f) return 0f
        val wave = sin(p * PI).toFloat()
        return min(cellsAhead.toFloat(), 0.40f) * wave * wave
    }

    fun blockedPulse(progress: Float): Float =
        (1f - finiteFraction(progress) * FeedbackTiming.BLOCKED_MS / FeedbackTiming.PRESS_MS).coerceIn(0f, 1f)

    /** Widens ink only, never scales or translates the complete shape into its neighbors. */
    fun strokeMultiplier(pressRemaining: Float): Float = 1f + 0.45f * finiteFraction(pressRemaining)

    private fun finiteFraction(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
}