package com.amowpuzzle.aitricker.domain.model

/** A single grid coordinate on the puzzle board. */
data class Cell(val col: Int, val row: Int) {

    fun step(direction: Direction, times: Int = 1): Cell =
        Cell(col + direction.dCol * times, row + direction.dRow * times)

    fun isInside(cols: Int, rows: Int): Boolean = col in 0 until cols && row in 0 until rows

    override fun toString(): String = "($col,$row)"
}
