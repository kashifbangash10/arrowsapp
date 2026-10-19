package com.amowpuzzle.aitricker.domain.engine

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell

/**
 * Flat cell -> arrow-id map for the arrows currently on the board.
 *
 * Collision queries run on every tap and thousands of times per level during
 * generation and validation, so the board is kept as a single [IntArray] instead
 * of rebuilding sets of cells.
 */
class Occupancy(val cols: Int, val rows: Int) {

    companion object {
        const val EMPTY = -1

        fun of(cols: Int, rows: Int, arrows: Collection<ArrowPath>): Occupancy {
            val grid = Occupancy(cols, rows)
            arrows.forEach { grid.place(it) }
            return grid
        }
    }

    private val owner = IntArray(cols * rows) { EMPTY }

    private fun index(col: Int, row: Int) = row * cols + col

    fun ownerAt(col: Int, row: Int): Int =
        if (col in 0 until cols && row in 0 until rows) owner[index(col, row)] else EMPTY

    fun ownerAt(cell: Cell): Int = ownerAt(cell.col, cell.row)

    fun isEmpty(col: Int, row: Int): Boolean = ownerAt(col, row) == EMPTY

    fun isEmpty(cell: Cell): Boolean = ownerAt(cell) == EMPTY

    fun place(arrow: ArrowPath) {
        arrow.cells.forEach { owner[index(it.col, it.row)] = arrow.id }
    }

    fun remove(arrow: ArrowPath) {
        arrow.cells.forEach { owner[index(it.col, it.row)] = EMPTY }
    }

    /** Number of cells covered by arrows, as a fraction of the whole board. */
    fun density(): Float = owner.count { it != EMPTY }.toFloat() / owner.size
}
