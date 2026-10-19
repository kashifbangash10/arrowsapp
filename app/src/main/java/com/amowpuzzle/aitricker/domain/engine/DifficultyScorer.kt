package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.Level

/**
 * Structural measurements for one level. Arrow count alone is a poor difficulty
 * signal - a board of 30 arrows that are all immediately free is easier than 12
 * arrows in a five-deep chain - so the curve is driven by these numbers instead.
 */
data class LevelMetrics(
    val arrowCount: Int,
    val initiallyFree: Int,
    val maxDependencyDepth: Int,
    val averageBranching: Float,
    /** Blocked arrows sitting on the board edge: they look free, they are not. */
    val misleadingArrows: Int,
    val density: Float,
    val turnsPerArrow: Float,
    val solutionLength: Int
) {
    /** Rough 0..100 hardness estimate, used to sanity check the shipped curve. */
    val score: Int
        get() {
            val chain = (maxDependencyDepth * 6).coerceAtMost(30)
            val tightness = ((6f - initiallyFree.coerceAtMost(6)) * 5f).toInt()
            val size = (arrowCount * 0.45f).toInt().coerceAtMost(25)
            val crowding = (density * 20f).toInt()
            val traps = (misleadingArrows * 1.5f).toInt().coerceAtMost(12)
            return (chain + tightness + size + crowding + traps).coerceIn(0, 100)
        }
}

object DifficultyScorer {

    fun measure(level: Level, solution: SolutionResult): LevelMetrics {
        val grid = Occupancy.of(level.cols, level.rows, level.arrows)
        var misleading = 0
        for (arrow in level.arrows) {
            if (CollisionEngine.isFree(arrow, grid)) continue
            val head = arrow.head
            val onEdge = head.col == 0 || head.row == 0 ||
                head.col == level.cols - 1 || head.row == level.rows - 1
            if (onEdge) misleading++
        }
        return LevelMetrics(
            arrowCount = level.arrowCount,
            initiallyFree = solution.initiallyFree,
            maxDependencyDepth = solution.maxDependencyDepth,
            averageBranching = solution.averageBranching,
            misleadingArrows = misleading,
            density = grid.density(),
            turnsPerArrow = if (level.arrows.isEmpty()) 0f
            else level.arrows.sumOf { it.turnCount }.toFloat() / level.arrows.size,
            solutionLength = solution.moveSequence.size
        )
    }
}
