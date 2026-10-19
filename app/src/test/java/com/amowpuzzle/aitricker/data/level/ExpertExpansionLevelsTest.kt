package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test

class ExpertExpansionLevelsTest {
    @Test
    fun `fifty new expert shapes occupy levels fifty one through one hundred`() {
        assertEquals((51..100).toSet(), ExpertExpansionLevels.ids)
        assertNull(ExpertExpansionLevels.level(50))
        assertNull(ExpertExpansionLevels.level(101))
        for (id in ExpertExpansionLevels.ids) {
            val level = requireNotNull(ExpertExpansionLevels.level(id))
            assertEquals("Expansion $id is not routed to the catalog", level, LevelCatalog.level(id))
            assertEquals(id, level.id)
            assertEquals(Difficulty.EXPERT, level.difficulty)
            assertEquals(LevelPresentation.DARK_WHITE, level.presentation)
            assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
        }
    }

    @Test
    fun `new shapes have at least one hundred arrows and deep measured dependencies`() {
        for (id in ExpertExpansionLevels.ids) {
            val level = LevelCatalog.level(id)
            val solved = PuzzleSolver.solve(level)
            assertTrue("Expert expansion $id is unsolvable", solved.solvable)
            assertTrue("Expert expansion $id needs at least 100 arrows", level.arrowCount >= 100)
            assertTrue("Expert expansion $id needs at least 30 dependency rounds", solved.maxDependencyDepth >= 30)
            assertTrue("Expert expansion $id offers too many obvious openings",
                solved.initiallyFree in 1..maxOf(3, level.arrowCount / 16))
            if (solved.initiallyFree > 3) {
                assertTrue("Extra openings on $id require at least 35 dependency rounds", solved.maxDependencyDepth >= 35)
            }
        }
    }

    @Test
    fun `each new expert silhouette has a different occupied shape`() {
        val masks = ExpertExpansionLevels.ids.map { id ->
            val cells = LevelCatalog.level(id).arrows.flatMap { it.cells }.toSet()
            val left = cells.minOf { it.col }
            val top = cells.minOf { it.row }
            // Translation and changed path directions cannot disguise the same shape.
            cells.map { Cell(it.col - left, it.row - top) }
                .sortedWith(compareBy<Cell> { it.row }.thenBy { it.col })
                .joinToString(";") { "${it.col},${it.row}" }
        }
        assertEquals("Expansion repeats an occupied silhouette", 50, masks.toSet().size)
    }

    @Test
    fun `every new expert authored certificate removes its complete board`() {
        for (id in ExpertExpansionLevels.ids) {
            val level = LevelCatalog.level(id)
            val ids = ExpertExpansionLevels.solutionIds(id)
            assertEquals("Expansion $id certificate length", level.arrowCount, ids.size)
            assertEquals("Expansion $id certificate identities", level.arrows.map { it.id }.toSet(), ids.toSet())
            val moves = ids.map { arrowId -> requireNotNull(level.arrowById(arrowId)) }
            assertTrue("Expansion $id authored certificate is not legal", PuzzleSolver.verify(level, moves))
        }
    }
}
