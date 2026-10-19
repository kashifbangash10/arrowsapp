package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test

class GuitarLevelTest {
    private val level = GuitarLevel.level

    @Test
    fun `the expert guitar closes the campaign with path-following movement`() {
        assertSame(level, LevelCatalog.level(50))
        assertEquals(50, level.id)
        assertEquals(Difficulty.EXPERT, level.difficulty)
        assertEquals(LevelPresentation.DARK_WHITE, level.presentation)
        assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
    }

    @Test
    fun `the dense guitar has disjoint valid paths and preserves reference orientation`() {
        assertEquals(115, level.arrowCount)
        assertEquals((1..115).toSet(), level.arrows.map { it.id }.toSet())
        val occupied = level.arrows.flatMap { it.cells }
        assertEquals(1465, occupied.size)
        assertEquals("Paths overlap", occupied.size, occupied.toSet().size)
        for (arrow in level.arrows) {
            assertTrue("Malformed arrow ${arrow.id}", arrow.isWellFormed(level.cols, level.rows))
            assertFalse("Arrow ${arrow.id} exits through its own route", arrow.isSelfBlocking(level.cols, level.rows))
        }
        assertTrue("Short paths missing", level.arrows.any { it.length <= 5 })
        assertTrue("Long winding paths missing", level.arrows.any { it.length >= 25 && it.turnCount >= 10 })
        val headstock = occupied.filter { it.row <= 16 }
        val body = occupied.filter { it.row >= 38 }
        assertTrue(headstock.minOf { it.col } >= 50)
        assertTrue(headstock.maxOf { it.col } >= 65)
        assertTrue(body.minOf { it.col } <= 2)
        assertTrue(body.maxOf { it.col } >= 40)
        assertTrue(body.maxOf { it.row } >= 66)
    }

    @Test
    fun `the path-following guitar has a deep complete solution`() {
        val solution = PuzzleSolver.solve(level)
        assertTrue(solution.solvable)
        assertTrue("Too many obvious opening moves", solution.initiallyFree in 1..7)
        assertTrue("Dependencies are too shallow", solution.maxDependencyDepth >= 20)
        assertEquals(level.arrowCount, solution.moveSequence.size)
        assertTrue(PuzzleSolver.verify(level, solution.moveSequence))
    }

    @Test
    fun `the updated authored removal certificate clears the path-following guitar`() {
        val ids = GuitarLevel.solutionIds
        assertEquals(level.arrows.map { it.id }.toSet(), ids.toSet())
        assertEquals(level.arrowCount, ids.size)
        val sequence = ids.map { requireNotNull(level.arrowById(it)) }
        assertTrue("Stored guitar certificate fails the actual movement rule", PuzzleSolver.verify(level, sequence))
        // The catalog audit independently checks all legal/blocked choices and
        // hints at every solution state, including this silhouette.
    }
}
