package com.amowpuzzle.aitricker.domain.generator

import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import kotlin.math.abs
import kotlin.random.Random

/**
 * Builds boards that are solvable by construction, then keeps the attempt that best
 * matches its [LevelSpec].
 *
 * Arrows are laid down in the order they will later be removed. As soon as an arrow
 * is placed, every cell of its escape corridor is reserved, so nothing placed after
 * it can ever step into its way. Arrow k can therefore only be blocked by arrows
 * 0..k-1, and peeling them off in placement order always clears the board: no
 * search, and no risk of shipping an impossible puzzle.
 *
 * Cells are visited from the board edge inwards, so the arrows placed first sit near
 * the rim with corridors one or two cells long, and later arrows aim outwards across
 * ground that is already covered. That keeps boards dense while producing the long
 * "clear that one before this one" chains the puzzle lives on.
 */
object LevelGenerator {

    fun generate(spec: LevelSpec): Level {
        var best: Level? = null
        var bestScore = Int.MIN_VALUE

        for (attempt in 0 until spec.attempts) {
            val arrows = Builder(spec, Random(spec.seed + attempt * 7919L)).build()
            if (arrows.size < 2) continue

            val level = Level(spec.id, spec.cols, spec.rows, spec.difficulty, arrows)
            val solution = PuzzleSolver.solve(level)
            if (!solution.solvable) continue

            val density = Occupancy.of(spec.cols, spec.rows, arrows).density()
            val depth = solution.maxDependencyDepth
            if (spec.satisfies(arrows.size, solution.initiallyFree, depth, density)) return level

            val score = spec.score(arrows.size, solution.initiallyFree, depth, density)
            if (score > bestScore) {
                bestScore = score
                best = level
            }
        }
        return best ?: fallback(spec)
    }

    /** Straight arrows in the upper rows, all pointing off the top edge. Always solvable. */
    private fun fallback(spec: LevelSpec): Level {
        val arrows = ArrayList<ArrowPath>()
        var row = 0
        while (row + 1 < spec.rows && arrows.size < spec.minArrows) {
            for (col in 0 until spec.cols) {
                if (arrows.size >= spec.minArrows) break
                arrows.add(
                    ArrowPath(
                        id = arrows.size,
                        cells = listOf(Cell(col, row + 1), Cell(col, row)),
                        exit = Direction.UP
                    )
                )
            }
            row += 2
        }
        return Level(spec.id, spec.cols, spec.rows, spec.difficulty, arrows)
    }

    private class Candidate(
        val cells: List<Cell>,
        val exit: Direction,
        val blockedBy: Int,
        val newlyReserved: Int,
        val corridor: List<Cell>
    )

    private class Builder(val spec: LevelSpec, val rng: Random) {

        val cols = spec.cols
        val rows = spec.rows
        val owner = IntArray(cols * rows) { -1 }
        val reserved = BooleanArray(cols * rows)
        val arrows = ArrayList<ArrowPath>()
        var initiallyFree = 0

        fun index(cell: Cell) = cell.row * cols + cell.col

        fun inside(cell: Cell) = cell.col in 0 until cols && cell.row in 0 until rows

        fun isPlaceable(cell: Cell): Boolean {
            if (!inside(cell)) return false
            val i = index(cell)
            return owner[i] == -1 && !reserved[i]
        }

        fun build(): List<ArrowPath> {
            val starts = startOrder()
            // The first pass lays down full-length arrows; the second squeezes short
            // ones into the gaps that are left, so boards read as dense, not patchy.
            layDown(starts, spec.minPathLength)
            layDown(starts, 2)
            return arrows
        }

        private fun layDown(starts: List<Cell>, minLength: Int) {
            for (start in starts) {
                if (arrows.size >= spec.targetArrows) return
                if (!isPlaceable(start)) continue
                val path = growPath(start, minLength) ?: continue
                val candidates = exitCandidates(path)
                if (candidates.isEmpty()) continue
                commit(choose(candidates))
            }
        }

        /** Board cells ordered from the outer ring inwards, shuffled within each ring. */
        private fun startOrder(): List<Cell> {
            val cells = ArrayList<Triple<Cell, Int, Int>>(cols * rows)
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val ring = minOf(col, row, cols - 1 - col, rows - 1 - row)
                    cells.add(Triple(Cell(col, row), ring, rng.nextInt(100000)))
                }
            }
            cells.sortWith(compareBy({ it.second }, { it.third }))
            return cells.map { it.first }
        }

        /**
         * Walks a polyline out of [start] through unclaimed cells. The walk refuses
         * cells that touch what it has already drawn, so an arrow never doubles back
         * so tightly that its two legs read as one solid block.
         */
        private fun growPath(start: Cell, minLength: Int): List<Cell>? {
            val cells = ArrayList<Cell>(spec.maxPathLength + 1)
            cells.add(start)
            val target = rng.nextInt(minLength, spec.maxPathLength + 1)
            var direction = Direction.ALL.shuffled(rng).firstOrNull { isPlaceable(start.step(it)) }
                ?: return null

            while (cells.size < target) {
                val turns = direction.perpendicular.toList().shuffled(rng)
                val order = if (rng.nextFloat() < spec.turnChance) {
                    turns + direction
                } else {
                    listOf(direction) + turns
                }
                var advanced = false
                for (candidate in order) {
                    val next = cells[cells.size - 1].step(candidate)
                    if (!isPlaceable(next)) continue
                    if (touchesPath(cells, next)) continue
                    cells.add(next)
                    direction = candidate
                    advanced = true
                    break
                }
                if (!advanced) break
            }
            return if (cells.size >= minLength) cells else null
        }

        /** True when [next] lands on, or orthogonally beside, anything but the current tip. */
        private fun touchesPath(cells: List<Cell>, next: Cell): Boolean {
            val tip = cells[cells.size - 1]
            for (cell in cells) {
                if (cell == next) return true
                if (cell == tip) continue
                if (abs(cell.col - next.col) + abs(cell.row - next.row) <= 1) return true
            }
            return false
        }

        /**
         * Every legal way to put the arrowhead on this path: either end of the
         * polyline, plus a one-cell final turn at either end. The turns matter,
         * because they let the generator aim an arrow at a chosen neighbour instead
         * of only along whatever direction the walk happened to finish in.
         */
        private fun exitCandidates(path: List<Cell>): List<Candidate> {
            val result = ArrayList<Candidate>(6)
            for (oriented in listOf(path, path.reversed())) {
                val head = oriented[oriented.size - 1]
                val base = Direction.between(oriented[oriented.size - 2], head) ?: continue
                addCandidate(result, oriented, base)
                for (turn in base.perpendicular.toList()) {
                    val extra = head.step(turn)
                    if (!isPlaceable(extra)) continue
                    if (touchesPath(oriented, extra)) continue
                    addCandidate(result, oriented + extra, turn)
                }
            }
            return result
        }

        private fun addCandidate(out: MutableList<Candidate>, cells: List<Cell>, exit: Direction) {
            val own = cells.toHashSet()
            val corridor = ArrayList<Cell>()
            var blockedBy = 0
            var newlyReserved = 0
            var cell = cells[cells.size - 1].step(exit)
            while (inside(cell)) {
                // An arrow that would have to travel through its own body can never
                // leave the board, so that geometry is simply not offered.
                if (own.contains(cell)) return
                corridor.add(cell)
                val i = index(cell)
                if (owner[i] != -1) blockedBy++ else if (!reserved[i]) newlyReserved++
                cell = cell.step(exit)
            }
            out.add(Candidate(cells, exit, blockedBy, newlyReserved, corridor))
        }

        /**
         * Early arrows are aimed at open board so the player has somewhere to start.
         * Once the quota of opening moves is filled, arrows are aimed at something
         * that blocks them instead. Reserving as few empty cells as possible leaves
         * room for more arrows, which is what makes the later boards look crowded.
         */
        private fun choose(candidates: List<Candidate>): Candidate {
            val wantFree = initiallyFree < spec.targetInitiallyFree
            val free = candidates.filter { it.blockedBy == 0 }
            val blocked = candidates.filter { it.blockedBy > 0 }
            val pool = when {
                wantFree && free.isNotEmpty() -> free
                !wantFree && blocked.isNotEmpty() -> blocked
                else -> candidates
            }
            return pool.minWithOrNull(
                compareBy({ it.newlyReserved }, { -it.cells.size }, { it.exit.ordinal })
            )!!
        }

        private fun commit(candidate: Candidate) {
            val arrow = ArrowPath(arrows.size, candidate.cells, candidate.exit)
            arrows.add(arrow)
            candidate.cells.forEach { owner[index(it)] = arrow.id }
            candidate.corridor.forEach { reserved[index(it)] = true }
            if (candidate.blockedBy == 0) initiallyFree++
        }
    }
}
