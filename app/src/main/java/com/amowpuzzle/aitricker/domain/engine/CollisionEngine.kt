package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.MovementRule

/**
 * Decides which arrows may currently leave the board.
 *
 * Existing levels retain their original head-corridor rule. Authored rigid-body
 * levels check the swept geometry of every body cell instead. Paths are made of
 * adjacent orthogonal cells, so these rays cover every translated segment, not
 * just the arrowhead or a bounding rectangle. Rendering keeps strokes and heads
 * within half a cell of their centreline, leaving parallel lanes separated.
 */
object CollisionEngine {

    fun isFree(
        arrow: ArrowPath,
        grid: Occupancy,
        rule: MovementRule = MovementRule.HEAD_CORRIDOR
    ): Boolean {
        if (rule == MovementRule.SWEPT_TRANSLATION) {
            val dir = arrow.exit
            for (cell in arrow.cells) {
                var col = cell.col + dir.dCol
                var row = cell.row + dir.dRow
                while (col in 0 until grid.cols && row in 0 until grid.rows) {
                    val owner = grid.ownerAt(col, row)
                    // Its own body moves with it and cannot block a rigid translation.
                    if (owner != Occupancy.EMPTY && owner != arrow.id) return false
                    col += dir.dCol
                    row += dir.dRow
                }
            }
            return true
        }
        val dir = arrow.exit
        var col = arrow.head.col + dir.dCol
        var row = arrow.head.row + dir.dRow
        while (col in 0 until grid.cols && row in 0 until grid.rows) {
            if (grid.ownerAt(col, row) != Occupancy.EMPTY) return false
            col += dir.dCol
            row += dir.dRow
        }
        return true
    }

    fun isFree(arrow: ArrowPath, level: Level, active: Collection<ArrowPath>): Boolean =
        isFree(arrow, Occupancy.of(level.cols, level.rows, active), level.movementRule)

    fun freeArrows(
        active: Collection<ArrowPath>,
        grid: Occupancy,
        rule: MovementRule = MovementRule.HEAD_CORRIDOR
    ): List<ArrowPath> = active.filter { isFree(it, grid, rule) }

    /** Ids of arrows in the escape geometry, nearest contact first. */
    fun blockersOf(
        arrow: ArrowPath,
        grid: Occupancy,
        rule: MovementRule = MovementRule.HEAD_CORRIDOR
    ): List<Int> {
        if (rule == MovementRule.SWEPT_TRANSLATION) {
            return sweptContacts(arrow, grid).entries.sortedWith(
                compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key }
            ).map { it.key }
        }
        val dir = arrow.exit
        val blockers = LinkedHashSet<Int>()
        var col = arrow.head.col + dir.dCol
        var row = arrow.head.row + dir.dRow
        while (col in 0 until grid.cols && row in 0 until grid.rows) {
            val owner = grid.ownerAt(col, row)
            if (owner != Occupancy.EMPTY && owner != arrow.id) blockers.add(owner)
            col += dir.dCol
            row += dir.dRow
        }
        return blockers.toList()
    }

    /**
     * How far the arrow can slide before something stops it, in cells.
     * Used only for the "blocked" nudge animation, so the arrow visibly bumps into
     * whatever is in its way instead of shaking in place.
     */
    fun freeCellsAhead(
        arrow: ArrowPath,
        grid: Occupancy,
        rule: MovementRule = MovementRule.HEAD_CORRIDOR
    ): Int {
        if (rule == MovementRule.SWEPT_TRANSLATION) {
            val firstContact = sweptContacts(arrow, grid).values.minOrNull()
            if (firstContact != null) return firstContact - 1
            // No contact: distance until even the rearmost cell has left the board.
            return arrow.cells.maxOf { cell ->
                when (arrow.exit) {
                    Direction.UP -> cell.row + 1
                    Direction.RIGHT -> grid.cols - cell.col
                    Direction.DOWN -> grid.rows - cell.row
                    Direction.LEFT -> cell.col + 1
                }
            }
        }
        val dir = arrow.exit
        var steps = 0
        var col = arrow.head.col + dir.dCol
        var row = arrow.head.row + dir.dRow
        while (col in 0 until grid.cols && row in 0 until grid.rows) {
            if (grid.ownerAt(col, row) != Occupancy.EMPTY) break
            steps++
            col += dir.dCol
            row += dir.dRow
        }
        return steps
    }

    /** The earliest translated-cell intersection with each other arrow. */
    private fun sweptContacts(arrow: ArrowPath, grid: Occupancy): Map<Int, Int> {
        val contacts = mutableMapOf<Int, Int>()
        val dir = arrow.exit
        for (cell in arrow.cells) {
            var col = cell.col + dir.dCol
            var row = cell.row + dir.dRow
            var distance = 1
            while (col in 0 until grid.cols && row in 0 until grid.rows) {
                val owner = grid.ownerAt(col, row)
                if (owner != Occupancy.EMPTY && owner != arrow.id) {
                    contacts[owner] = minOf(contacts[owner] ?: Int.MAX_VALUE, distance)
                }
                col += dir.dCol
                row += dir.dRow
                distance++
            }
        }
        return contacts
    }
}
