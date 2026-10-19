package com.amowpuzzle.aitricker.redesign

import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.domain.model.*

/** Copies only movement policy; authored coordinates and catalog are immutable. */
internal fun previewLevel(fixture: PreviewFixture): Level = when (fixture) {
    PreviewFixture.GUITAR -> LevelCatalog.level(50).copy(movementRule = MovementRule.HEAD_CORRIDOR)
    PreviewFixture.DOG -> LevelCatalog.level(49).copy(movementRule = MovementRule.HEAD_CORRIDOR)
    PreviewFixture.LEVEL_50 -> LevelCatalog.level(50)
    else -> Level(
        id = -fixture.ordinal - 1, cols = 12, rows = 10, difficulty = Difficulty.EASY,
        arrows = when (fixture) {
            PreviewFixture.STRAIGHT -> listOf(
                previewArrow(1, Cell(2, 6), Cell(2, 2)),
                previewArrow(2, Cell(6, 6), Cell(9, 6)),
                previewArrow(3, Cell(4, 3), Cell(8, 3))
            )
            PreviewFixture.L_BEND -> listOf(
                previewArrow(1, Cell(2, 7), Cell(2, 2), Cell(5, 2)),
                previewArrow(2, Cell(8, 5), Cell(8, 1))
            )
            PreviewFixture.U_BEND -> listOf(
                previewArrow(1, Cell(2, 2), Cell(2, 6), Cell(6, 6), Cell(6, 2)),
                previewArrow(2, Cell(4, 0), Cell(8, 0))
            )
            PreviewFixture.ZIGZAG -> listOf(
                previewArrow(1, Cell(2, 7), Cell(2, 5), Cell(5, 5), Cell(5, 3), Cell(8, 3)),
                previewArrow(2, Cell(10, 5), Cell(10, 2))
            )
            else -> error("Catalog fixture handled above")
        }
    )
}

private fun previewArrow(id: Int, vararg corners: Cell): ArrowPath {
    val cells = mutableListOf(corners.first())
    corners.toList().zipWithNext().forEach { (from, to) ->
        require(from.col == to.col || from.row == to.row)
        val dx = (to.col - from.col).coerceIn(-1, 1)
        val dy = (to.row - from.row).coerceIn(-1, 1)
        var current = from
        while (current != to) {
            current = Cell(current.col + dx, current.row + dy)
            cells.add(current)
        }
    }
    return ArrowPath(id, cells, requireNotNull(Direction.between(cells[cells.lastIndex - 1], cells.last())))
}
