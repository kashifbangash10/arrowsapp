package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.*

/** Explicit tail-to-head paths, authored offline and independently replayed with HEAD_CORRIDOR.
 * The runtime expands only straight segments; no random generation or fallback. */
object DogLevel {
    const val COLS = 69
    const val ROWS = 69

    // 119 arrows; 6 openings; 35 dependency rounds.
    val level: Level by lazy {
        Level(
            id = 49, cols = 69, rows = 69, difficulty = Difficulty.EXPERT,
            presentation = LevelPresentation.DARK_WHITE,
            movementRule = MovementRule.HEAD_CORRIDOR,
            arrows = listOf(
                path(1, Direction.RIGHT, 28, 23, 28, 24, 25, 24, 25, 26, 27, 26),
                path(2, Direction.DOWN, 49, 35, 49, 34, 44, 34, 44, 38),
                path(3, Direction.DOWN, 36, 31, 36, 27, 38, 27, 38, 25, 39, 25, 39, 27),
                path(4, Direction.DOWN, 23, 22, 23, 25, 20, 25, 20, 23, 19, 23, 19, 30),
                path(5, Direction.LEFT, 51, 42, 51, 45, 50, 45, 50, 42, 49, 42),
                path(6, Direction.DOWN, 16, 44, 16, 43, 15, 43, 15, 45, 18, 45, 18, 42, 14, 42, 14, 45),
                path(7, Direction.DOWN, 14, 48, 14, 49, 15, 49, 15, 47, 14, 47, 14, 46, 16, 46, 16, 48, 17, 48, 17, 49),
                path(8, Direction.RIGHT, 53, 23, 55, 23, 55, 27, 56, 27),
                path(9, Direction.UP, 16, 22, 16, 23, 18, 23, 18, 22),
                path(10, Direction.RIGHT, 7, 25, 7, 23, 5, 23, 5, 25, 6, 25, 6, 26, 7, 26, 7, 28, 11, 28),
                path(11, Direction.DOWN, 52, 60, 56, 60, 56, 61),
                path(12, Direction.UP, 16, 9, 19, 9, 19, 8),
                path(13, Direction.LEFT, 22, 59, 22, 61, 24, 61, 24, 58, 18, 58),
                path(14, Direction.DOWN, 20, 12, 23, 12, 23, 13, 24, 13, 24, 12, 25, 12, 25, 13, 26, 13, 26, 11, 27, 11, 27, 13),
                path(15, Direction.UP, 52, 34, 52, 35, 50, 35, 50, 27),
                path(16, Direction.RIGHT, 34, 49, 30, 49, 30, 50, 28, 50, 28, 52, 31, 52, 31, 51, 32, 51, 32, 50, 33, 50),
                path(17, Direction.RIGHT, 17, 54, 19, 54, 19, 55, 18, 55, 18, 57, 19, 57),
                path(18, Direction.DOWN, 53, 20, 52, 20, 52, 18, 51, 18, 51, 20),
                path(19, Direction.RIGHT, 58, 24, 59, 24, 59, 21, 60, 21),
                path(20, Direction.UP, 37, 62, 37, 58, 36, 58, 36, 57, 37, 57, 37, 52),
                path(21, Direction.RIGHT, 25, 39, 26, 39, 26, 36, 28, 36),
                path(22, Direction.DOWN, 54, 32, 52, 32, 52, 28, 54, 28, 54, 29, 55, 29, 55, 28, 56, 28, 56, 29),
                path(23, Direction.UP, 60, 23, 60, 22, 61, 22, 61, 21, 62, 21, 62, 20, 58, 20, 58, 19, 57, 19, 57, 20, 56, 20, 56, 19, 55, 19, 55, 20, 54, 20, 54, 19),
                path(24, Direction.LEFT, 53, 46, 51, 46, 51, 48, 52, 48, 52, 49, 51, 49),
                path(25, Direction.UP, 60, 11, 58, 11, 58, 12, 57, 12, 57, 11),
                path(26, Direction.LEFT, 28, 20, 30, 20, 30, 18, 32, 18, 32, 17, 29, 17),
                path(27, Direction.RIGHT, 51, 64, 44, 64, 44, 63, 45, 63, 45, 62, 49, 62),
                path(28, Direction.UP, 59, 19, 62, 19, 62, 18, 61, 18, 61, 17, 63, 17, 63, 15, 62, 15, 62, 16, 61, 16, 61, 15),
                path(29, Direction.RIGHT, 7, 19, 6, 19, 6, 18, 4, 18, 4, 20, 7, 20),
                path(30, Direction.DOWN, 14, 62, 17, 62, 17, 61, 16, 61, 16, 60, 18, 60, 18, 62),
                path(31, Direction.DOWN, 11, 23, 11, 26, 8, 26, 8, 27),
                path(32, Direction.RIGHT, 43, 24, 43, 25, 42, 25, 42, 31, 47, 31),
                path(33, Direction.DOWN, 44, 39, 44, 43),
                path(34, Direction.LEFT, 52, 43, 57, 43, 57, 44, 58, 44, 58, 42, 56, 42),
                path(35, Direction.RIGHT, 27, 17, 28, 17, 28, 16, 32, 16, 32, 15, 31, 15, 31, 14, 32, 14),
                path(36, Direction.DOWN, 55, 54, 55, 55, 52, 55, 52, 56, 53, 56, 53, 59, 54, 59, 54, 56, 55, 56, 55, 58),
                path(37, Direction.UP, 49, 21, 46, 21, 46, 17, 49, 17, 49, 14),
                path(38, Direction.RIGHT, 45, 36, 52, 36),
                path(39, Direction.DOWN, 42, 42, 43, 42, 43, 38, 40, 38, 40, 39),
                path(40, Direction.DOWN, 35, 50, 34, 50, 34, 52, 32, 52, 32, 59),
                path(41, Direction.UP, 35, 32, 35, 27),
                path(42, Direction.RIGHT, 54, 33, 53, 33, 53, 36, 56, 36),
                path(43, Direction.UP, 55, 9, 55, 8, 54, 8, 54, 10, 55, 10, 55, 11, 53, 11, 53, 10, 52, 10, 52, 7),
                path(44, Direction.LEFT, 23, 4, 23, 6, 30, 6, 30, 7, 29, 7, 29, 8, 30, 8, 30, 9, 28, 9, 28, 7, 27, 7, 27, 10, 25, 10, 25, 8, 26, 8, 26, 7, 25, 7),
                path(45, Direction.UP, 33, 54, 33, 53, 35, 53, 35, 55, 34, 55, 34, 59, 35, 59, 35, 60, 36, 60, 36, 59),
                path(46, Direction.UP, 46, 10, 46, 9, 47, 9, 47, 10, 48, 10, 48, 8, 49, 8, 49, 9, 51, 9, 51, 8),
                path(47, Direction.LEFT, 22, 35, 24, 35, 24, 31, 19, 31),
                path(48, Direction.RIGHT, 6, 27, 4, 27, 4, 28, 6, 28),
                path(49, Direction.LEFT, 57, 41, 53, 41),
                path(50, Direction.RIGHT, 48, 37, 45, 37, 45, 38, 46, 38),
                path(51, Direction.LEFT, 20, 59, 14, 59, 14, 57, 17, 57, 17, 55, 15, 55),
                path(52, Direction.RIGHT, 14, 53, 19, 53, 19, 51, 20, 51, 20, 50, 24, 50),
                path(53, Direction.DOWN, 31, 56, 31, 55, 30, 55, 30, 53, 28, 53, 28, 58),
                path(54, Direction.DOWN, 32, 62, 31, 62, 31, 61, 30, 61, 30, 60, 31, 60, 31, 59, 29, 59, 29, 60),
                path(55, Direction.UP, 61, 8, 57, 8, 57, 9, 56, 9, 56, 8),
                path(56, Direction.RIGHT, 55, 39, 55, 40, 53, 40, 53, 39, 54, 39, 54, 37, 55, 37),
                path(57, Direction.RIGHT, 22, 14, 26, 14),
                path(58, Direction.LEFT, 17, 5, 16, 5, 16, 4, 21, 4, 21, 5, 20, 5, 20, 6, 18, 6, 18, 7, 17, 7, 17, 8, 16, 8),
                path(59, Direction.RIGHT, 12, 21, 12, 23, 14, 23),
                path(60, Direction.LEFT, 57, 7, 55, 7, 55, 6, 53, 6),
                path(61, Direction.LEFT, 56, 45, 54, 45, 54, 47, 52, 47),
                path(62, Direction.LEFT, 22, 33, 18, 33),
                path(63, Direction.UP, 63, 20, 63, 19),
                path(64, Direction.UP, 13, 21, 13, 22, 15, 22, 15, 20),
                path(65, Direction.UP, 15, 15, 17, 15, 17, 17, 18, 17, 18, 16),
                path(66, Direction.UP, 27, 41, 33, 41, 33, 40),
                path(67, Direction.DOWN, 41, 41, 39, 41, 39, 42, 40, 42, 40, 44, 42, 44, 42, 43, 43, 43, 43, 44),
                path(68, Direction.RIGHT, 6, 21, 10, 21),
                path(69, Direction.DOWN, 57, 34, 58, 34, 58, 40, 57, 40, 57, 39, 56, 39, 56, 40),
                path(70, Direction.LEFT, 22, 36, 22, 38, 17, 38),
                path(71, Direction.LEFT, 23, 64, 23, 63, 20, 63, 20, 64, 18, 64),
                path(72, Direction.UP, 24, 64, 24, 63),
                path(73, Direction.LEFT, 60, 7, 59, 7),
                path(74, Direction.DOWN, 16, 41, 20, 41, 20, 39, 22, 39, 22, 41),
                path(75, Direction.LEFT, 59, 6, 58, 6),
                path(76, Direction.LEFT, 52, 6, 49, 6, 49, 7, 48, 7),
                path(77, Direction.RIGHT, 46, 51, 44, 51, 44, 52, 48, 52),
                path(78, Direction.RIGHT, 17, 37, 20, 37, 20, 34, 22, 34),
                path(79, Direction.UP, 33, 26, 33, 29, 34, 29, 34, 27),
                path(80, Direction.LEFT, 28, 63, 30, 63, 30, 62, 28, 62),
                path(81, Direction.LEFT, 58, 25, 57, 25, 57, 29, 58, 29, 58, 30, 57, 30, 57, 31, 58, 31, 58, 32, 57, 32),
                path(82, Direction.UP, 14, 14, 17, 14, 17, 12),
                path(83, Direction.UP, 63, 11, 63, 14, 62, 14, 62, 12),
                path(84, Direction.UP, 36, 63, 36, 61, 33, 61, 33, 57),
                path(85, Direction.DOWN, 44, 61, 44, 62),
                path(86, Direction.DOWN, 55, 53, 54, 53, 54, 52, 53, 52, 53, 53),
                path(87, Direction.LEFT, 16, 29, 16, 30, 17, 30, 17, 35, 19, 35, 19, 36, 18, 36),
                path(88, Direction.DOWN, 16, 35, 16, 40, 17, 40, 17, 39, 18, 39, 18, 40),
                path(89, Direction.LEFT, 53, 51, 48, 51),
                path(90, Direction.DOWN, 15, 51, 15, 52, 17, 52, 17, 50, 14, 50, 14, 52),
                path(91, Direction.LEFT, 59, 9, 62, 9, 62, 10, 59, 10),
                path(92, Direction.LEFT, 55, 47, 55, 46, 56, 46, 56, 48, 54, 48),
                path(93, Direction.DOWN, 14, 15, 14, 17, 15, 17, 15, 16, 16, 16, 16, 17),
                path(94, Direction.UP, 16, 10, 17, 10, 17, 11, 18, 11, 18, 10),
                path(95, Direction.RIGHT, 12, 27, 12, 26, 13, 26, 13, 25, 12, 25, 12, 24, 13, 24),
                path(96, Direction.DOWN, 40, 29, 40, 36),
                path(97, Direction.UP, 38, 35, 38, 31),
                path(98, Direction.DOWN, 41, 26, 41, 27, 40, 27, 40, 28, 41, 28, 41, 29),
                path(99, Direction.UP, 44, 30, 45, 30, 45, 26),
                path(100, Direction.DOWN, 15, 61, 15, 60, 14, 60, 14, 61),
                path(101, Direction.DOWN, 16, 64, 17, 64, 17, 63, 14, 63, 14, 64),
                path(102, Direction.LEFT, 25, 41, 25, 40, 26, 40, 26, 42, 25, 42),
                path(103, Direction.UP, 37, 26, 34, 26, 34, 25),
                path(104, Direction.RIGHT, 4, 22, 8, 22),
                path(105, Direction.RIGHT, 4, 21, 5, 21),
                path(106, Direction.RIGHT, 45, 24, 44, 24, 44, 25, 45, 25),
                path(107, Direction.LEFT, 33, 63, 35, 63, 35, 62, 34, 62),
                path(108, Direction.UP, 38, 41, 38, 36),
                path(109, Direction.RIGHT, 42, 24, 40, 24, 40, 25, 41, 25),
                path(110, Direction.RIGHT, 14, 54, 16, 54),
                path(111, Direction.RIGHT, 14, 56, 16, 56),
                path(112, Direction.DOWN, 54, 63, 53, 63, 53, 64, 56, 64, 56, 62, 52, 62, 52, 64),
                path(113, Direction.DOWN, 51, 50, 54, 50, 54, 51, 55, 51, 55, 49, 56, 49, 56, 51),
                path(114, Direction.RIGHT, 13, 20, 14, 20),
                path(115, Direction.DOWN, 9, 20, 8, 20, 8, 19, 11, 19, 11, 18, 14, 18, 14, 19),
                path(116, Direction.DOWN, 22, 4, 22, 6),
                path(117, Direction.UP, 21, 8, 21, 6),
                path(118, Direction.DOWN, 54, 30, 54, 31, 56, 31, 56, 32),
                path(119, Direction.LEFT, 58, 33, 56, 33),
            )
        )
    }


    /** Complete valid removal certificate for path-following withdrawal. */
    val solutionIds: List<Int> = listOf(67, 112, 51, 58, 103, 101, 41, 71, 30, 100, 80, 53, 54, 107, 40, 79, 66, 117, 44, 76, 43, 46, 15, 37, 27, 85, 39, 96, 98, 3, 20, 45, 97, 108, 11, 36, 86, 77, 33, 2, 113, 16, 84, 17, 111, 110, 52, 90, 7, 6, 102, 5, 34, 24, 92, 61, 89, 13, 74, 49, 69, 42, 38, 18, 21, 56, 50, 88, 87, 62, 47, 119, 118, 22, 81, 32, 8, 106, 109, 99, 1, 78, 70, 4, 10, 48, 31, 12, 94, 91, 83, 35, 14, 57, 116, 60, 75, 73, 23, 19, 95, 55, 28, 63, 25, 72, 65, 9, 59, 93, 26, 82, 64, 114, 115, 29, 104, 68, 105)

    private fun path(id: Int, exit: Direction, vararg xy: Int): ArrowPath {
        require(xy.size >= 4 && xy.size % 2 == 0)
        val corners = xy.toList().chunked(2).map { Cell(it[0], it[1]) }
        val cells = mutableListOf(corners.first())
        for ((from, to) in corners.zipWithNext()) {
            require((from.col == to.col) xor (from.row == to.row))
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
