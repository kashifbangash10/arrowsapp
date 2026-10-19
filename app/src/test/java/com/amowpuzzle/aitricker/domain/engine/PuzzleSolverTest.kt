package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The solver is what lets the game promise a level can be finished, so these boards
 * are small enough that the right answer can be read off the page.
 */
class PuzzleSolverTest {

    private fun straight(id: Int, from: Cell, to: Cell): ArrowPath =
        ArrowPath(id, listOf(from, to), requireNotNull(Direction.between(from, to)))

    private fun levelOf(vararg arrows: ArrowPath): Level = Level(
        id = 1,
        cols = 5,
        rows = 5,
        difficulty = Difficulty.EASY,
        arrows = arrows.toList()
    )

    /** B stands in A's corridor, so there is exactly one order that works. */
    private val blocked = straight(1, Cell(2, 3), Cell(2, 2))
    private val blocker = straight(2, Cell(1, 1), Cell(2, 1))

    @Test
    fun `a chained board is solved in dependency order`() {
        val level = levelOf(blocked, blocker)
        val result = PuzzleSolver.solve(level)

        assertTrue(result.solvable)
        assertEquals(listOf(2, 1), result.moveSequence.map { it.id })
        assertEquals(2, result.moveSequence.size)
    }

    @Test
    fun `verify accepts the solver's own answer and rejects the reverse`() {
        val level = levelOf(blocked, blocker)
        val result = PuzzleSolver.solve(level)

        assertTrue(PuzzleSolver.verify(level, result.moveSequence))
        assertFalse(PuzzleSolver.verify(level, listOf(blocked, blocker)))
    }

    @Test
    fun `verify rejects sequences that are short, padded, or repeated`() {
        val level = levelOf(blocked, blocker)

        assertFalse(PuzzleSolver.verify(level, listOf(blocker)))
        assertFalse(PuzzleSolver.verify(level, listOf(blocker, blocker)))
        assertFalse(PuzzleSolver.verify(level, emptyList()))
    }

    /**
     * Two arrows pointing into each other: neither can ever leave. The generator
     * cannot produce this, but the solver has to recognise it, because that is what
     * makes the "all 50 levels are solvable" test meaningful rather than vacuous.
     */
    @Test
    fun `a head-on pair is unsolvable and offers no hint`() {
        val left = straight(1, Cell(0, 2), Cell(1, 2))
        val right = straight(2, Cell(4, 2), Cell(3, 2))
        val level = levelOf(left, right)
        val result = PuzzleSolver.solve(level)

        assertEquals(Direction.RIGHT, left.exit)
        assertEquals(Direction.LEFT, right.exit)
        assertFalse(result.solvable)
        assertTrue(result.moveSequence.isEmpty())
        assertFalse(PuzzleSolver.isSolvable(level))
        assertNull(PuzzleSolver.findHint(level, level.arrows))
    }

    /** A hint has to name an arrow that really can leave, not just any arrow. */
    @Test
    fun `findHint returns a genuinely free arrow`() {
        val level = levelOf(blocked, blocker)
        val hint = PuzzleSolver.findHint(level, level.arrows)

        assertEquals(2, hint?.id)
        assertTrue(
            CollisionEngine.isFree(
                requireNotNull(hint),
                Occupancy.of(level.cols, level.rows, level.arrows)
            )
        )
    }

    @Test
    fun `findHint follows the board as arrows leave it`() {
        val level = levelOf(blocked, blocker)
        val remaining = listOf(blocked)

        assertEquals(1, PuzzleSolver.findHint(level, remaining)?.id)
    }

    /**
     * Depth counts rounds, not moves: a crowd of arrows that are all free at once is
     * one round however many there are, while a chain of two takes two.
     */
    @Test
    fun `dependency depth counts rounds rather than arrows`() {
        val chain = levelOf(blocked, blocker)
        assertEquals(2, PuzzleSolver.maxDependencyDepth(chain))

        val crowd = levelOf(
            straight(1, Cell(0, 1), Cell(0, 0)),
            straight(2, Cell(2, 1), Cell(2, 0)),
            straight(3, Cell(4, 1), Cell(4, 0))
        )
        val result = PuzzleSolver.solve(crowd)

        assertTrue(result.solvable)
        assertEquals(1, result.maxDependencyDepth)
        assertEquals(3, result.initiallyFree)
        assertEquals(3f, result.averageBranching, 0.0001f)
    }

    @Test
    fun `initially free and branching describe how tight a board is`() {
        val level = levelOf(blocked, blocker)
        val result = PuzzleSolver.solve(level)

        assertEquals(1, result.initiallyFree)
        assertEquals(1f, result.averageBranching, 0.0001f)
    }

    /**
     * A three-deep chain, each link in a different direction so the ordering cannot
     * fall out of the board's geometry by accident:
     *
     *   1 exits UP  along column 0 and is held by 2's body at (0,2)
     *   2 exits RIGHT along row 2 and is held by 3's body at (2,2)
     *   3 exits UP  along column 2 with nothing above it, so it goes first
     */
    @Test
    fun `a three step chain takes three rounds`() {
        val first = straight(1, Cell(0, 4), Cell(0, 3))
        val second = straight(2, Cell(0, 2), Cell(1, 2))
        val third = straight(3, Cell(2, 3), Cell(2, 2))
        val level = levelOf(first, second, third)
        val result = PuzzleSolver.solve(level)

        assertEquals(Direction.UP, first.exit)
        assertEquals(Direction.RIGHT, second.exit)
        assertEquals(Direction.UP, third.exit)

        assertTrue(result.solvable)
        assertEquals(3, result.maxDependencyDepth)
        assertEquals(1, result.initiallyFree)
        assertEquals(listOf(3, 2, 1), result.moveSequence.map { it.id })
        assertTrue(PuzzleSolver.verify(level, result.moveSequence))
    }
}
