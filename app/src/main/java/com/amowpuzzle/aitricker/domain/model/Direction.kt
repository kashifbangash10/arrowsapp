package com.amowpuzzle.aitricker.domain.model

enum class Direction(val dx: Int, val dy: Int, val rotationDegrees: Float) {
    UP(dx = 0, dy = -1, rotationDegrees = 0f),
    RIGHT(dx = 1, dy = 0, rotationDegrees = 90f),
    DOWN(dx = 0, dy = 1, rotationDegrees = 180f),
    LEFT(dx = -1, dy = 0, rotationDegrees = 270f);

    val dCol: Int get() = dx
    val dRow: Int get() = dy
    val isHorizontal: Boolean get() = dy == 0
    val perpendicular: Pair<Direction, Direction>
        get() = if (isHorizontal) UP to DOWN else LEFT to RIGHT

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    companion object {
        val ALL: List<Direction> = listOf(UP, RIGHT, DOWN, LEFT)

        fun between(from: Cell, to: Cell): Direction? {
            val dc = to.col - from.col
            val dr = to.row - from.row
            return ALL.firstOrNull { it.dCol == dc && it.dRow == dr }
        }
    }
}
