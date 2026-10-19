package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.CollisionEngine
import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class DogLevelTest {
    private val level = DogLevel.level

    @Test
    fun `level forty nine is the expert white path-following dog silhouette`() {
        assertSame(level, LevelCatalog.level(49))
        assertEquals(49, level.id)
        assertEquals(Difficulty.EXPERT, level.difficulty)
        assertEquals(LevelPresentation.DARK_WHITE, level.presentation)
        assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
    }

    @Test
    fun `the dog paths have stable unique identities and disjoint valid geometry`() {
        assertTrue(level.arrowCount >= 50)
        val occupied = level.arrows.flatMap { it.cells }
        assertEquals("Paths overlap", occupied.size, occupied.toSet().size)
        for (arrow in level.arrows) {
            assertTrue("Malformed arrow ${arrow.id}", arrow.isWellFormed(level.cols, level.rows))
        }
    }

    @Test
    fun `difficulty retuning preserves the exact dog silhouette mask`() {
        val occupied = level.arrows.flatMap { it.cells }.toSet()
        assertEquals(1046, occupied.size)
        val canonical = occupied.sortedWith(compareBy<Cell> { it.row }.thenBy { it.col })
            .joinToString("") { "${it.col},${it.row};" }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("360acd821e65c952cfc5bed30df4d048c57ace8d3f65cf37e9d1f97039e9977a", digest)
    }

    @Test
    fun `the expert dog has a complete valid solution and substantial dependencies`() {
        val solution = PuzzleSolver.solve(level)
        assertTrue(solution.solvable)
        assertEquals(level.arrowCount, solution.moveSequence.size)
        assertTrue(PuzzleSolver.verify(level, solution.moveSequence))
        // Difficulty is recertified under path-following movement, rather than
        // retaining the obsolete rigid-body collision measurements.
        assertTrue("Expert dog must retain a substantial path count", level.arrowCount >= 75)
        assertTrue("Expert dog has too many opening moves",
            solution.initiallyFree in 1..maxOf(3, level.arrowCount / 16))
        assertTrue("Expert dog needs a substantial blocking chain", solution.maxDependencyDepth >= 20)
    }
    @Test
    fun `the dog authored certificate clears all paths under withdrawal rules`() {
        val ids = DogLevel.solutionIds
        assertEquals(level.arrowCount, ids.size)
        assertEquals(level.arrows.map { it.id }.toSet(), ids.toSet())
        assertTrue(PuzzleSolver.verify(level, ids.map { requireNotNull(level.arrowById(it)) }))
    }

}
