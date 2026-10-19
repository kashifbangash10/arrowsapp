package com.amowpuzzle.aitricker.domain.generator

import com.amowpuzzle.aitricker.domain.model.Difficulty

/**
 * The recipe for one level. Everything that shapes how a board feels lives here,
 * so the difficulty curve is a table of numbers rather than logic spread through
 * the generator.
 */
data class LevelSpec(
    val id: Int,
    val cols: Int,
    val rows: Int,
    val difficulty: Difficulty,
    /** Wanted arrow count; generation stops growing the board past this. */
    val targetArrows: Int,
    val minArrows: Int,
    /** How many arrows may be tappable on the untouched board. */
    val targetInitiallyFree: Int,
    val maxInitiallyFree: Int,
    /**
     * Fewest rounds the board may take to clear, where one round is a whole wave of
     * simultaneously free arrows. This is the lever that separates a crowd of easy
     * arrows from a real chain of dependencies.
     */
    val minDependencyDepth: Int,
    val minDensity: Float,
    val minPathLength: Int,
    val maxPathLength: Int,
    /** 0f keeps arrows straight, 1f turns at every opportunity. */
    val turnChance: Float,
    val attempts: Int = 14
) {
    val seed: Long get() = id * 0x9E3779B1L + 0x5DEECE66DL

    fun satisfies(
        arrowCount: Int,
        initiallyFree: Int,
        dependencyDepth: Int,
        density: Float
    ): Boolean = arrowCount >= minArrows &&
        initiallyFree <= maxInitiallyFree &&
        dependencyDepth >= minDependencyDepth &&
        density >= minDensity

    /** Higher is a better match for this spec; used to keep the best of N attempts. */
    fun score(arrowCount: Int, initiallyFree: Int, dependencyDepth: Int, density: Float): Int {
        var value = 0
        value += if (arrowCount >= minArrows) 4000 else -(minArrows - arrowCount) * 120
        value -= (arrowCount - targetArrows).coerceAtLeast(0) * 30
        value += if (initiallyFree <= maxInitiallyFree) 3000
        else -(initiallyFree - maxInitiallyFree) * 250
        value += dependencyDepth.coerceAtMost(12) * 150
        value += (density * 2000f).toInt()
        return value
    }
}
