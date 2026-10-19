package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The escape rule is the whole game, so it is pinned down here on boards small
 * enough to check by hand. Every case is a 5x5 grid with row 0 along the top, which
 * is how the board is drawn.
 */
class CollisionEngineTest {

    private val cols = 5
    private val rows = 5

    private fun straight(id: Int, from: Cell, to: Cell): ArrowPath =
        ArrowPath(id, listOf(from, to), requireNotNull(Direction.between(from, to)))

    private fun gridOf(vararg arrows: ArrowPath): Occupancy =
        Occupancy.of(cols, rows, arrows.toList())

    @Test
    fun `an arrow with a clear corridor is free`() {
        val arrow = straight(1, Cell(2, 3), Cell(2, 2))
        assertEquals(Direction.UP, arrow.exit)
        assertTrue(CollisionEngine.isFree(arrow, gridOf(arrow)))
    }

    @Test
    fun `anything standing in the corridor blocks the arrow`() {
        val arrow = straight(1, Cell(2, 3), Cell(2, 2))
        val blocker = straight(2, Cell(1, 1), Cell(2, 1))
        val grid = gridOf(arrow, blocker)

        assertFalse(CollisionEngine.isFree(arrow, grid))
        assertTrue(CollisionEngine.isFree(blocker, grid))
        assertEquals(listOf(2), CollisionEngine.blockersOf(arrow, grid))
    }

    /** Freeness is monotone: clearing the board can only ever open things up. */
    @Test
    fun `removing the blocker frees the arrow`() {
        val arrow = straight(1, Cell(2, 3), Cell(2, 2))
        val blocker = straight(2, Cell(1, 1), Cell(2, 1))
        val grid = gridOf(arrow, blocker)

        assertFalse(CollisionEngine.isFree(arrow, grid))
        grid.remove(blocker)
        assertTrue(CollisionEngine.isFree(arrow, grid))
    }

    /**
     * The case a naive "same row or same column" test gets wrong: this neighbour
     * shares the column but sits behind the tail, so it cannot possibly be in the way.
     */
    @Test
    fun `an arrow in the same column but behind the head does not block`() {
        val arrow = straight(1, Cell(2, 3), Cell(2, 2))
        val behind = straight(2, Cell(1, 4), Cell(2, 4))
        assertTrue(CollisionEngine.isFree(arrow, gridOf(arrow, behind)))
    }

    @Test
    fun `an arrow in the same row but behind the head does not block`() {
        val arrow = straight(1, Cell(2, 2), Cell(3, 2))
        val behind = straight(2, Cell(0, 2), Cell(0, 1))
        assertEquals(Direction.RIGHT, arrow.exit)
        assertTrue(CollisionEngine.isFree(arrow, gridOf(arrow, behind)))
    }

    @Test
    fun `blockers are reported nearest first and free distance is measured`() {
        val arrow = straight(1, Cell(0, 4), Cell(0, 3))
        val near = straight(2, Cell(0, 1), Cell(1, 1))
        val far = straight(3, Cell(0, 0), Cell(1, 0))
        val grid = gridOf(arrow, near, far)

        assertEquals(listOf(2, 3), CollisionEngine.blockersOf(arrow, grid))
        assertEquals(1, CollisionEngine.freeCellsAhead(arrow, grid))
    }

    /**
     * A path that curls in front of its own head could never leave, however empty the
     * rest of the board is. The generator refuses to build these; the rule is still
     * asserted here, because it is what makes that refusal necessary.
     */
    @Test
    fun `an arrow whose body crosses its own corridor is never free`() {
        val hook = ArrowPath(
            id = 1,
            cells = listOf(Cell(1, 2), Cell(1, 1), Cell(2, 1), Cell(3, 1), Cell(3, 2), Cell(2, 2)),
            exit = Direction.LEFT
        )
        val grid = gridOf(hook)

        assertTrue(hook.isWellFormed(cols, rows))
        assertTrue(hook.isSelfBlocking(cols, rows))
        assertFalse(CollisionEngine.isFree(hook, grid))
        // Its own body is not something the player can clear, so it is not a blocker.
        assertTrue(CollisionEngine.blockersOf(hook, grid).isEmpty())
    }

    @Test
    fun `freeArrows lists exactly the arrows that may be tapped`() {
        val blockedOne = straight(1, Cell(2, 3), Cell(2, 2))
        val freeOne = straight(2, Cell(1, 1), Cell(2, 1))
        val freeTwo = straight(3, Cell(4, 3), Cell(4, 2))
        val active = listOf(blockedOne, freeOne, freeTwo)

        val free = CollisionEngine.freeArrows(active, Occupancy.of(cols, rows, active))
        assertEquals(listOf(2, 3), free.map { it.id })
    }

    /** An arrowhead already on the rim has no corridor at all, so it is always free. */
    @Test
    fun `an arrow already at the edge is free with nothing ahead of it`() {
        val edge = straight(1, Cell(2, 1), Cell(2, 0))
        val grid = gridOf(edge)

        assertTrue(CollisionEngine.isFree(edge, grid))
        assertEquals(0, CollisionEngine.freeCellsAhead(edge, grid))
    }

    @Test
    fun `occupancy tracks owners and density`() {
        val arrow = straight(7, Cell(0, 0), Cell(1, 0))
        val grid = gridOf(arrow)

        assertEquals(7, grid.ownerAt(Cell(0, 0)))
        assertEquals(7, grid.ownerAt(Cell(1, 0)))
        assertTrue(grid.isEmpty(Cell(2, 0)))
        assertEquals(Occupancy.EMPTY, grid.ownerAt(-1, 0))
        assertEquals(2f / 25f, grid.density(), 0.0001f)
    }
}
