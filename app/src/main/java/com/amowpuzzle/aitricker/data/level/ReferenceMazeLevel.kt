package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.*

/** Manual coordinate trace of the supplied 36-arrow rectangular board.
 * Screenshot lattice: origin (94,453), pitch 41 px; converted to grid + one-cell margin.
 * 3 openings, 20 dependency rounds, 221 occupied cells. No runtime generation.
 * Corners below are ordered tail to head; all IDs remain stable. */
internal object ReferenceMazeLevel {
    val solutionIds: List<Int> = listOf(1, 10, 23, 2, 5, 4, 17, 3, 26, 16, 25, 7, 11, 35, 12, 18, 19, 22, 15, 20, 28, 14, 13, 30, 31, 32, 36, 8, 27, 6, 29, 21, 34, 24, 33, 9)

    val level: Level = Level(
        id = 6, cols = 16, rows = 18, difficulty = Difficulty.HARD,
        presentation = LevelPresentation.DARK_WHITE,
        movementRule = MovementRule.HEAD_CORRIDOR,
        arrows = listOf(
            path(1, Direction.RIGHT, 2, 1, 1, 1, 1, 4, 2, 4, 2, 2, 5, 2, 5, 5, 10, 5, 10, 3, 14, 3),
            path(2, Direction.LEFT, 7, 1, 3, 1),
            path(3, Direction.RIGHT, 8, 1, 12, 1),
            path(4, Direction.LEFT, 13, 1, 14, 1, 14, 2, 13, 2),
            path(5, Direction.LEFT, 12, 2, 8, 2, 8, 4, 6, 4, 6, 3, 7, 3, 7, 2, 6, 2),
            path(6, Direction.RIGHT, 3, 3, 4, 3),
            path(7, Direction.LEFT, 4, 4, 3, 4, 3, 5, 2, 5),
            path(8, Direction.DOWN, 9, 3, 9, 4),
            path(9, Direction.DOWN, 11, 4, 11, 5),
            path(10, Direction.RIGHT, 12, 7, 13, 7, 13, 5, 12, 5, 12, 4, 14, 4),
            path(11, Direction.DOWN, 14, 5, 14, 7),
            path(12, Direction.RIGHT, 11, 6, 12, 6),
            path(13, Direction.LEFT, 8, 6, 6, 6),
            path(14, Direction.LEFT, 4, 7, 4, 6, 3, 6),
            path(15, Direction.DOWN, 2, 6, 2, 7),
            path(16, Direction.DOWN, 1, 5, 1, 10),
            path(17, Direction.UP, 5, 7, 5, 6),
            path(18, Direction.RIGHT, 9, 7, 9, 6, 10, 6, 10, 7, 11, 7),
            path(19, Direction.RIGHT, 3, 10, 2, 10, 2, 8, 5, 8, 5, 9, 6, 9, 6, 7, 8, 7),
            path(20, Direction.RIGHT, 8, 8, 9, 8),
            path(21, Direction.DOWN, 7, 8, 7, 10),
            path(22, Direction.UP, 9, 9, 10, 9, 10, 8),
            path(23, Direction.RIGHT, 11, 8, 14, 8),
            path(24, Direction.LEFT, 12, 9, 11, 9),
            path(25, Direction.LEFT, 14, 9, 14, 16, 4, 16),
            path(26, Direction.UP, 3, 16, 1, 16, 1, 11, 2, 11, 2, 15, 13, 15, 13, 9),
            path(27, Direction.DOWN, 3, 11, 3, 13),
            path(28, Direction.LEFT, 5, 10, 5, 11, 4, 11, 4, 9, 3, 9),
            path(29, Direction.UP, 7, 13, 4, 13, 4, 12),
            path(30, Direction.UP, 12, 14, 10, 14, 10, 12, 9, 12, 9, 13, 8, 13, 8, 11, 7, 11, 7, 12, 6, 12, 6, 10),
            path(31, Direction.UP, 9, 11, 9, 10, 8, 10, 8, 9),
            path(32, Direction.DOWN, 11, 10, 10, 10, 10, 11),
            path(33, Direction.UP, 11, 11, 12, 11, 12, 10),
            path(34, Direction.LEFT, 12, 12, 11, 12),
            path(35, Direction.RIGHT, 11, 13, 12, 13),
            path(36, Direction.RIGHT, 3, 14, 9, 14),
        )
    )

    private fun path(id: Int, exit: Direction, vararg xy: Int): ArrowPath {
        require(xy.size >= 4 && xy.size % 2 == 0)
        val corners = xy.toList().chunked(2).map { Cell(it[0], it[1]) }
        val cells = mutableListOf(corners.first())
        for ((from, to) in corners.zipWithNext()) {
            require((from.col == to.col) != (from.row == to.row))
            val dx = (to.col - from.col).coerceIn(-1, 1)
            val dy = (to.row - from.row).coerceIn(-1, 1)
            var cursor = from
            while (cursor != to) {
                cursor = Cell(cursor.col + dx, cursor.row + dy)
                cells.add(cursor)
            }
        }
        return ArrowPath(id, cells, exit)
    }
}
