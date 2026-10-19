package com.amowpuzzle.aitricker.domain.generator

import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's two promises: the same spec always builds the same board, and every
 * board it builds can be solved.
 *
 * Determinism is what lets the app ship 50 levels as a table of numbers instead of a
 * data file - level 23 has to be the same puzzle on every device and every reinstall,
 * or a player comparing notes with a friend is playing a different game.
 */
class LevelGeneratorTest {

    private fun spec(
        id: Int,
        cols: Int = 8,
        rows: Int = 9,
        targetArrows: Int = 14,
        targetInitiallyFree: Int = 2,
        maxInitiallyFree: Int = 4,
        minDependencyDepth: Int = 3,
        minDensity: Float = 0.34f,
        turnChance: Float = 0.3f,
        attempts: Int = 12
    ) = LevelSpec(
        id = id,
        cols = cols,
        rows = rows,
        difficulty = Difficulty.NORMAL,
        targetArrows = targetArrows,
        minArrows = 8,
        targetInitiallyFree = targetInitiallyFree,
        maxInitiallyFree = maxInitiallyFree,
        minDependencyDepth = minDependencyDepth,
        minDensity = minDensity,
        minPathLength = 2,
        maxPathLength = 4,
        turnChance = turnChance,
        attempts = attempts
    )

    @Test
    fun `the same spec always builds the same board`() {
        val subject = spec(id = 23)
        val first = LevelGenerator.generate(subject)
        val second = LevelGenerator.generate(subject)

        assertEquals(first.arrowCount, second.arrowCount)
        assertEquals(first.arrows, second.arrows)
        assertEquals(first, second)
    }

    /** Two different levels must not quietly collapse onto the same seed. */
    @Test
    fun `different level ids build different boards`() {
        val one = LevelGenerator.generate(spec(id = 7))
        val two = LevelGenerator.generate(spec(id = 8))

        assertNotEquals(one.arrows, two.arrows)
        assertEquals(7, one.id)
        assertEquals(8, two.id)
    }

    @Test
    fun `the seed is derived from the level id alone`() {
        assertEquals(spec(id = 12).seed, spec(id = 12, cols = 11, rows = 13).seed)
        assertNotEquals(spec(id = 12).seed, spec(id = 13).seed)
    }

    /**
     * Arrows are placed in the order they will be removed and each one reserves its
     * corridor, so placement order is itself a solution. Anything the generator
     * returns must therefore be solvable - including the fallback board.
     */
    @Test
    fun `every generated board is solvable and well formed`() {
        for (id in 1..12) {
            val level = LevelGenerator.generate(spec(id = id))
            val where = "spec $id"
            val solution = PuzzleSolver.solve(level)

            assertTrue("$where is unsolvable", solution.solvable)
            assertTrue("$where fails to replay", PuzzleSolver.verify(level, solution.moveSequence))
            assertEquals(
                "$where reuses an arrow id",
                level.arrowCount,
                level.arrows.map { it.id }.toHashSet().size
            )
            for (arrow in level.arrows) {
                assertTrue(
                    "$where arrow ${arrow.id} is malformed",
                    arrow.isWellFormed(level.cols, level.rows)
                )
                assertTrue(
                    "$where arrow ${arrow.id} blocks itself",
                    !arrow.isSelfBlocking(level.cols, level.rows)
                )
            }
        }
    }

    /**
     * [LevelSpec.targetArrows] is a ceiling, not a hint: the board stops growing once
     * it is reached, which is what keeps a 13x15 expert board from filling up solid.
     */
    @Test
    fun `generation never exceeds the wanted arrow count`() {
        for (target in listOf(6, 14, 24)) {
            val level = LevelGenerator.generate(spec(id = target, targetArrows = target))
            assertTrue(
                "spec asked for at most $target arrows and got ${level.arrowCount}",
                level.arrowCount in 2..target
            )
        }
    }

    @Test
    fun `arrows never overlap and stay on the board`() {
        val level = LevelGenerator.generate(spec(id = 31, cols = 10, rows = 12, targetArrows = 30))
        val cells = level.arrows.flatMap { it.cells }

        assertEquals(cells.size, cells.toHashSet().size)
        assertTrue(cells.all { it.isInside(level.cols, level.rows) })

        val density = Occupancy.of(level.cols, level.rows, level.arrows).density()
        assertTrue("board is empty", density > 0f)
        assertTrue("board is impossibly full", density <= 1f)
    }

    /** An unreachable spec must still produce a playable level rather than throwing. */
    @Test
    fun `an impossible spec still yields a solvable board`() {
        val impossible = spec(
            id = 99,
            cols = 6,
            rows = 6,
            targetArrows = 40,
            minDependencyDepth = 40,
            minDensity = 0.99f,
            attempts = 4
        )
        val level = LevelGenerator.generate(impossible)

        assertTrue(level.arrowCount >= 2)
        assertTrue(PuzzleSolver.solve(level).solvable)
    }

    @Test
    fun `satisfies holds a level to every threshold at once`() {
        val subject = spec(id = 1, maxInitiallyFree = 4, minDependencyDepth = 3, minDensity = 0.34f)

        assertTrue(subject.satisfies(10, 2, 3, 0.40f))
        // One failure each: too few arrows, too many openings, too shallow, too sparse.
        assertTrue(!subject.satisfies(7, 2, 3, 0.40f))
        assertTrue(!subject.satisfies(10, 5, 3, 0.40f))
        assertTrue(!subject.satisfies(10, 2, 2, 0.40f))
        assertTrue(!subject.satisfies(10, 2, 3, 0.30f))
    }

    /** Scoring is what picks between attempts, so deeper and denser has to win. */
    @Test
    fun `score prefers deeper chains and tighter boards`() {
        val subject = spec(id = 1)
        val base = subject.score(12, 2, 4, 0.40f)

        assertTrue(subject.score(12, 2, 6, 0.40f) > base)
        assertTrue(subject.score(12, 2, 4, 0.50f) > base)
        assertTrue(subject.score(12, 9, 4, 0.40f) < base)
        assertTrue(subject.score(4, 2, 4, 0.40f) < base)
    }
}
