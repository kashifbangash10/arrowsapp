package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.*
import java.util.concurrent.ConcurrentHashMap

/** Fixed, editable coordinate resources. Loading never authors or randomizes a puzzle. */
object AdvancedCampaignLevels {
    val ids: Set<Int> = (501..1000).toSet()
    private data class Metadata(val name: String, val category: String, val cols: Int, val rows: Int)
    private data class AuthoredBoard(val level: Level, val solution: List<Int>)
    private val boards = ConcurrentHashMap<Int, AuthoredBoard>()
    private val metadata: Map<Int, Metadata> by lazy {
        resource("index.tsv").lineSequence().filter { it.isNotBlank() }.associate { row ->
            val fields = row.split('\t')
            require(fields.size == 5) { "Invalid advanced index" }
            fields[0].toInt() to Metadata(fields[1], fields[2], fields[3].toInt(), fields[4].toInt())
        }.also { require(it.keys == ids) { "Incomplete advanced index" } }
    }

    fun level(id: Int): Level? = if (id in ids) board(id).level else null
    fun solutionIds(id: Int): List<Int> = if (id in ids) board(id).solution else emptyList()
    fun name(id: Int): String? = if (id in ids) metadata[id]?.name else null
    fun category(id: Int): String? = if (id in ids) metadata[id]?.category else null

    private fun resource(file: String): String = requireNotNull(
        AdvancedCampaignLevels::class.java.getResourceAsStream("/levels/advanced/$file")
    ) { "Missing authored advanced resource: $file" }.bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun board(id: Int): AuthoredBoard {
        boards[id]?.let { return it }
        val info = metadata.getValue(id)
        val lines = resource("level-$id.txt").lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size >= 4 && lines[0] == "ADVANCED_V1" && lines[1].toInt() == id)
        val solution = lines[2].split(',').map { it.toInt() }
        val arrows = lines.drop(3).map { row ->
            val fields = row.split('|')
            require(fields.size == 3)
            path(fields[0].toInt(), Direction.valueOf(fields[1]),
                *fields[2].split(',').map { it.toInt() }.toIntArray())
        }
        require(solution.size == arrows.size && solution.toSet() == arrows.map { it.id }.toSet())
        val built = AuthoredBoard(Level(
            id = id, cols = info.cols, rows = info.rows, difficulty = if (id <= 750) Difficulty.LEGEND else Difficulty.EXTREME,
            presentation = LevelPresentation.DARK_WHITE, movementRule = MovementRule.HEAD_CORRIDOR,
            arrows = arrows
        ), solution)
        return boards.putIfAbsent(id, built) ?: built
    }

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
