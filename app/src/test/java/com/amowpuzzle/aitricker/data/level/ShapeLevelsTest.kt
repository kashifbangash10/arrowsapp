package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test

class ShapeLevelsTest {
    @Test
    fun `six different authored shapes occupy their intended progression milestones`() {
        val expected = setOf(15, 20, 25, 30, 40, 45)
        assertEquals(expected, ShapeLevels.ids)
        for (id in expected) {
            val level = requireNotNull(ShapeLevels.level(id))
            assertEquals("Shape $id missing from campaign", level, LevelCatalog.level(id))
            assertEquals(id, level.id)
            assertTrue(level.presentation.usesDarkBoard)
            assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
        }
        assertNull(ShapeLevels.level(1))
        assertNull(ShapeLevels.level(50))
    }

    @Test
    fun `every authored shape removal certificate replays completely`() {
        for (id in ShapeLevels.ids) {
            val level = requireNotNull(ShapeLevels.level(id))
            val ids = ShapeLevels.solutionIds(id)
            assertEquals("Shape $id certificate length", level.arrowCount, ids.size)
            assertEquals("Shape $id certificate IDs", level.arrows.map { it.id }.toSet(), ids.toSet())
            val sequence = ids.map { arrowId -> requireNotNull(level.arrowById(arrowId)) }
            assertTrue("Shape $id certificate collides or leaves arrows", PuzzleSolver.verify(level, sequence))
        }
    }
}
