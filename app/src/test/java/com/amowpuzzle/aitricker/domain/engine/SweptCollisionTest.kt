package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SweptCollisionTest {
    private val swept = MovementRule.SWEPT_TRANSLATION

    private fun arrow(id: Int, vararg cells: Pair<Int, Int>): ArrowPath {
        val points = cells.map { Cell(it.first, it.second) }
        return ArrowPath(id, points, requireNotNull(Direction.between(points[points.lastIndex - 1], points.last())))
    }

    private fun level(vararg arrows: ArrowPath): Level = Level(
        id = 1,
        cols = 7,
        rows = 7,
        difficulty = Difficulty.EXPERT,
        arrows = arrows.toList(),
        presentation = LevelPresentation.GUITAR_WHITE,
        movementRule = swept
    )

    // Its head has a clear upward corridor at column 4, but its left shoulder
    // would sweep column 1. Rotate this hand-checkable counterexample four ways.
    private val bent = arrow(1, 1 to 4, 1 to 3, 1 to 2, 2 to 2, 3 to 2, 3 to 3, 3 to 4, 4 to 4, 4 to 3)
    private val shoulderBlocker = arrow(2, 1 to 1, 1 to 0)

    private fun rotated(path: ArrowPath, turns: Int): ArrowPath {
        fun rotate(original: Cell): Cell {
            var result = original
            repeat(turns) { result = Cell(6 - result.row, result.col) }
            return result
        }
        val cells = path.cells.map(::rotate)
        return path.copy(cells = cells, exit = requireNotNull(Direction.between(cells[cells.lastIndex - 1], cells.last())))
    }

    @Test
    fun `all four exits test the body sweep even when the head corridor is clear`() {
        for (turns in 0..3) {
            val moving = rotated(bent, turns)
            val blocker = rotated(shoulderBlocker, turns)
            val grid = Occupancy.of(7, 7, listOf(moving, blocker))
            assertTrue(moving.isWellFormed(7, 7))
            assertTrue(blocker.isWellFormed(7, 7))
            assertTrue("legacy rule stays unchanged for ${moving.exit}", CollisionEngine.isFree(moving, grid))
            assertFalse("body is blocked for ${moving.exit}", CollisionEngine.isFree(moving, grid, swept))
            assertEquals(listOf(2), CollisionEngine.blockersOf(moving, grid, swept))
            assertEquals(0, CollisionEngine.freeCellsAhead(moving, grid, swept))
            grid.remove(blocker)
            assertTrue(CollisionEngine.isFree(moving, grid, swept))
        }
    }

    @Test
    fun `an empty concavity inside a bounding rectangle does not block translation`() {
        val moving = arrow(1, 1 to 4, 1 to 3, 1 to 2, 2 to 2, 3 to 2, 3 to 1)
        val inConcavity = arrow(2, 2 to 4, 3 to 4)
        val grid = Occupancy.of(7, 7, listOf(moving, inConcavity))
        assertTrue(CollisionEngine.isFree(moving, grid, swept))
        assertTrue(CollisionEngine.blockersOf(moving, grid, swept).isEmpty())
    }

    @Test
    fun `own body translates with the arrow instead of blocking it`() {
        val hook = arrow(1, 1 to 2, 1 to 1, 2 to 1, 3 to 1, 3 to 2, 2 to 2)
        val grid = Occupancy.of(7, 7, listOf(hook))
        assertFalse(CollisionEngine.isFree(hook, grid))
        assertTrue(CollisionEngine.isFree(hook, grid, swept))
        assertTrue(CollisionEngine.blockersOf(hook, grid, swept).isEmpty())
    }

    @Test
    fun `blocked distance uses first contact over the whole path and blockers are unique`() {
        val moving = arrow(1, 1 to 5, 1 to 4, 1 to 3, 2 to 3, 3 to 3, 3 to 4, 4 to 4, 4 to 3)
        val nearerBody = arrow(2, 1 to 1, 2 to 1)
        val fartherHead = arrow(3, 4 to 0, 5 to 0)
        val grid = Occupancy.of(7, 7, listOf(moving, nearerBody, fartherHead))
        assertEquals(1, CollisionEngine.freeCellsAhead(moving, grid, swept))
        assertEquals(listOf(2, 3), CollisionEngine.blockersOf(moving, grid, swept))
        grid.remove(nearerBody)
        assertEquals(2, CollisionEngine.freeCellsAhead(moving, grid, swept))
    }

    @Test
    fun `solver hints and verification all obey the selected movement rule`() {
        val puzzle = level(bent, shoulderBlocker)
        val result = PuzzleSolver.solve(puzzle)
        assertTrue(result.solvable)
        assertEquals(listOf(2, 1), result.moveSequence.map { it.id })
        assertEquals(1, result.initiallyFree)
        assertEquals(2, result.maxDependencyDepth)
        assertEquals(2, PuzzleSolver.findHint(puzzle, puzzle.arrows)?.id)
        assertTrue(PuzzleSolver.verify(puzzle, result.moveSequence))
        assertFalse(PuzzleSolver.verify(puzzle, puzzle.arrows))
        assertFalse(CollisionEngine.isFree(bent, puzzle, puzzle.arrows))
        assertEquals(listOf(2), CollisionEngine.freeArrows(puzzle.arrows, Occupancy.of(7, 7, puzzle.arrows), swept).map { it.id })
        val legacy = puzzle.copy(movementRule = MovementRule.HEAD_CORRIDOR)
        assertEquals(2, PuzzleSolver.solve(legacy).initiallyFree)
        assertEquals(1, PuzzleSolver.findHint(legacy, legacy.arrows)?.id)
    }

    @Test
    fun `swept body cycle is detected even when both head corridors are clear`() {
        val first = arrow(1, 1 to 3, 1 to 2, 2 to 2, 3 to 2, 3 to 3, 4 to 3, 4 to 2)
        val second = arrow(2, 2 to 1, 2 to 0, 1 to 0, 0 to 0, 0 to 1)
        val puzzle = level(first, second)
        // The second arrow descends at column 0 but its body also sweeps column 2.
        assertTrue(CollisionEngine.isFree(first, Occupancy.of(7, 7, puzzle.arrows)))
        assertTrue(CollisionEngine.isFree(second, Occupancy.of(7, 7, puzzle.arrows)))
        assertFalse(PuzzleSolver.solve(puzzle).solvable)
        assertNull(PuzzleSolver.findHint(puzzle, puzzle.arrows))
    }

    @Test
    fun `analytic sweep agrees with explicit translated geometry for compact paths`() {
        // Independent oracle: translate the complete polyline at successive cell
        // offsets, instead of tracing rays or inspecting its enclosing rectangle.
        // Orthogonal integer-grid segments first intersect at an integer offset.
        val side = 3
        val paths = mutableListOf<ArrowPath>()
        fun extend(cells: List<Cell>) {
            if (cells.size == 4) {
                paths += ArrowPath(1, cells, requireNotNull(Direction.between(cells[2], cells[3])))
                return
            }
            for (direction in Direction.ALL) {
                val next = cells.last().step(direction)
                if (next.isInside(side, side) && next !in cells) extend(cells + next)
            }
        }
        for (row in 0 until side) for (col in 0 until side) extend(listOf(Cell(col, row)))
        var comparisons = 0
        for (moving in paths) {
            for (row in 0 until side) for (col in 0 until side) {
                val start = Cell(col, row)
                for (direction in listOf(Direction.RIGHT, Direction.DOWN)) {
                    val end = start.step(direction)
                    if (!end.isInside(side, side) || start in moving.cells || end in moving.cells) continue
                    val blocker = ArrowPath(2, listOf(start, end), direction)
                    val blocked = (1..side).any { distance ->
                        moving.cells.any { cell -> cell.step(moving.exit, distance) in blocker.cells }
                    }
                    val grid = Occupancy.of(side, side, listOf(moving, blocker))
                    assertEquals(
                        "${moving.cells} exits ${moving.exit}, obstacle ${blocker.cells}",
                        !blocked,
                        CollisionEngine.isFree(moving, grid, swept)
                    )
                    comparisons++
                }
            }
        }
        assertTrue("All rotations and compact bends are exercised", comparisons > 300)
    }
}
