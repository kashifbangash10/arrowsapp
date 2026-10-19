package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Level

data class SolutionResult(
    val solvable: Boolean,
    val moveSequence: List<ArrowPath>,
    /** Number of rounds the board takes to clear - the longest blocking chain. */
    val maxDependencyDepth: Int,
    val initiallyFree: Int,
    /** Average number of arrows tappable per round; low means a tight board. */
    val averageBranching: Float
)

object PuzzleSolver {

    /** Finds an arrow currently free to escape, suitable for a hint. */
    fun findHint(level: Level, activeArrows: List<ArrowPath>): ArrowPath? {
        val grid = Occupancy.of(level.cols, level.rows, activeArrows)
        return activeArrows.firstOrNull { arrow ->
            CollisionEngine.isFree(arrow, grid, level.movementRule)
        }
    }

    /**
     * Solves the board from [activeArrows], returning full metrics and move sequence.
     *
     * The board is peeled a round at a time: every arrow that is free right now is
     * taken off, then the next round looks at what that opened up. Taking an arrow
     * off the board can never block another one, so every arrow that was free at the
     * start of a round is still free at the end of it - which makes this both a valid
     * play order and a correct measure of how deep the level's dependencies run. A
     * board of forty arrows that are all free at once clears in one round; twelve
     * arrows in a chain take twelve.
     */
    fun solve(level: Level, activeArrows: List<ArrowPath> = level.arrows): SolutionResult {
        val grid = Occupancy.of(level.cols, level.rows, activeArrows)
        val active = activeArrows.toMutableList()
        val sequence = ArrayList<ArrowPath>(active.size)

        var initiallyFree = 0
        var rounds = 0
        var totalBranching = 0

        while (active.isNotEmpty()) {
            val free = active.filter { CollisionEngine.isFree(it, grid, level.movementRule) }
            if (free.isEmpty()) {
                return SolutionResult(
                    solvable = false,
                    moveSequence = emptyList(),
                    maxDependencyDepth = rounds,
                    initiallyFree = initiallyFree,
                    averageBranching = 0f
                )
            }

            if (rounds == 0) initiallyFree = free.size
            rounds++
            totalBranching += free.size

            for (arrow in free) {
                grid.remove(arrow)
                sequence.add(arrow)
            }
            active.removeAll(free.toHashSet())
        }

        val avgBranching = if (rounds > 0) totalBranching.toFloat() / rounds else 0f
        return SolutionResult(
            solvable = true,
            moveSequence = sequence,
            maxDependencyDepth = rounds,
            initiallyFree = initiallyFree,
            averageBranching = avgBranching
        )
    }

    fun isSolvable(level: Level): Boolean = solve(level).solvable

    /**
     * Replays [sequence] against a fresh board and reports whether it really clears
     * the level. The generator is solvable by construction and the solver agrees with
     * itself by definition, so this exists to catch the case where the two disagree -
     * it is what the level tests assert against.
     */
    fun verify(level: Level, sequence: List<ArrowPath>): Boolean {
        if (sequence.size != level.arrowCount) return false
        if (sequence.map { it.id }.toHashSet().size != sequence.size) return false
        val grid = Occupancy.of(level.cols, level.rows, level.arrows)
        for (arrow in sequence) {
            val onBoard = level.arrowById(arrow.id) ?: return false
            if (!CollisionEngine.isFree(onBoard, grid, level.movementRule)) return false
            grid.remove(onBoard)
        }
        return level.arrows.all { grid.isEmpty(it.head) }
    }

    /** Longest chain of "clear that one first" dependencies, in rounds. */
    fun maxDependencyDepth(level: Level): Int = solve(level).maxDependencyDepth
}
