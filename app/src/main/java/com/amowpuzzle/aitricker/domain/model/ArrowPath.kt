package com.amowpuzzle.aitricker.domain.model

/**
 * One arrow on the board: an orthogonal polyline of grid cells with a single
 * arrowhead at the [head] end.
 *
 * [cells] runs from tail to head and every consecutive pair is orthogonally
 * adjacent. [exit] is always the direction of the final segment, so the drawn
 * arrowhead and the direction the arrow escapes in are the same thing - which is
 * what makes the puzzle readable.
 */
data class ArrowPath(
    val id: Int,
    val cells: List<Cell>,
    val exit: Direction
) {
    val head: Cell get() = cells[cells.size - 1]
    val tail: Cell get() = cells[0]
    val length: Int get() = cells.size

    /** Cells where the path changes direction. */
    val corners: List<Cell>
        get() {
            if (cells.size < 3) return emptyList()
            val result = ArrayList<Cell>(cells.size - 2)
            for (i in 1 until cells.size - 1) {
                val a = Direction.between(cells[i - 1], cells[i])
                val b = Direction.between(cells[i], cells[i + 1])
                if (a != b) result.add(cells[i])
            }
            return result
        }

    val turnCount: Int get() = corners.size

    /**
     * True when the geometry is usable: at least two cells, all inside the board,
     * no repeated cell, every step orthogonal, and [exit] matching the last segment.
     */
    fun isWellFormed(cols: Int, rows: Int): Boolean {
        if (cells.size < 2) return false
        if (cells.any { !it.isInside(cols, rows) }) return false
        if (cells.toHashSet().size != cells.size) return false
        for (i in 1 until cells.size) {
            if (Direction.between(cells[i - 1], cells[i]) == null) return false
        }
        return Direction.between(cells[cells.size - 2], head) == exit
    }

    /**
     * Cells the arrow travels through on its way off the board, head exclusive.
     * These are the cells that decide whether the arrow is free or blocked.
     */
    fun escapeCorridor(cols: Int, rows: Int): List<Cell> {
        val result = ArrayList<Cell>()
        var cell = head.step(exit)
        while (cell.isInside(cols, rows)) {
            result.add(cell)
            cell = cell.step(exit)
        }
        return result
    }

    /** True when the arrow's own body sits in its escape corridor - it could never leave. */
    fun isSelfBlocking(cols: Int, rows: Int): Boolean {
        val own = cells.toHashSet()
        return escapeCorridor(cols, rows).any { own.contains(it) }
    }
}
