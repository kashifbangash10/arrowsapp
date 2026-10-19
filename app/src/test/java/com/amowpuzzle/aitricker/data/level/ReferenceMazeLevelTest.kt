package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Cell
import org.junit.Assert.*
import org.junit.Test

class ReferenceMazeLevelTest {
    @Test
    fun `reference trace keeps its silhouette and complete removal certificate`() {
        val level = ReferenceMazeLevel.level
        assertEquals(level, LevelCatalog.level(6))
        assertEquals(36, level.arrowCount)
        assertEquals(221, level.arrows.sumOf { it.length })
        val occupied = level.arrows.flatMap { it.cells }.toSet()
        assertEquals(1, occupied.minOf { it.col })
        assertEquals(14, occupied.maxOf { it.col })
        assertEquals(1, occupied.minOf { it.row })
        assertEquals(16, occupied.maxOf { it.row })
        // The lower-right outer path and long woven top path anchor the manual trace.
        assertEquals(listOf(Cell(14, 9), Cell(14, 16), Cell(4, 16)),
            listOf(level.arrowById(25)!!.tail, level.arrowById(25)!!.cells[7], level.arrowById(25)!!.head))
        val solved = PuzzleSolver.solve(level)
        assertTrue(solved.solvable)
        assertEquals(3, solved.initiallyFree)
        assertEquals(20, solved.maxDependencyDepth)
        assertEquals(36, ReferenceMazeLevel.solutionIds.toSet().size)
        assertTrue(PuzzleSolver.verify(level,
            ReferenceMazeLevel.solutionIds.map { requireNotNull(level.arrowById(it)) }))
    }
}
