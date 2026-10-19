package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.CollisionEngine
import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.MovementRule
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Certifies the current campaign; the previous redesign proposal report remains historical. */
class WithdrawalCatalogAuditTest {
    @Test
    fun `every catalog solution hint and blocked move agrees with independent withdrawal geometry`() {
        val rows = LevelCatalog.levelIds.map { id -> audit(LevelCatalog.level(id)) }
        val userDirectory = requireNotNull(System.getProperty("user.dir")) { "Working directory is unavailable" }
        val root = requireNotNull(generateSequence(File(userDirectory).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile && File(it, "app").isDirectory }) {
            "Could not locate the project root for the campaign audit"
        }
        val report = File(root, "artifacts/campaign/campaign-validation.json")
        val reportDirectory = requireNotNull(report.parentFile) { "Campaign report needs a parent directory" }
        assertTrue(reportDirectory.isDirectory || reportDirectory.mkdirs())
        report.writeText("""
            {
              "scope": "Actual shipped campaign, all legal and blocked choices checked at every certified removal",
              "oracle": "Independent segment/ray intersections over the complete swept support of a path-following withdrawal",
              "limitations": ["Logical grid geometry, not device touch or rendered pixel clearance", "Does not certify concurrent animations"],
              "levelCount": ${rows.size},
              "levels": [
            """.trimIndent() + "\n" + rows.joinToString(",\n") + "\n  ]\n}\n")
    }

    private fun audit(level: Level): String {
        val solution = PuzzleSolver.solve(level)
        assertTrue("Level ${level.id} is unsolvable", solution.solvable)
        assertTrue("Level ${level.id} solution fails production replay", PuzzleSolver.verify(level, solution.moveSequence))
        assertEquals("All campaign arrows must follow their path", MovementRule.HEAD_CORRIDOR, level.movementRule)
        val occupied = level.arrows.flatMap { it.cells }
        assertEquals("Static paths intersect before movement", occupied.size, occupied.toSet().size)
        // A withdrawing body follows a suffix of its original disjoint route and
        // then the forward exit ray. Its complete swept support is that route
        // plus that ray. Only the ray adds newly occupied points; intersect it
        // with every actual obstacle segment, including its own original body.
        // This does not call the solver, occupancy rays, or escapeCorridor.
        // Geometry is immutable: retain each segment once instead of allocating
        // the same pairs for every moving arrow in the 1,500-board audit.
        val segments = level.arrows.associate { it.id to it.cells.zipWithNext() }
        val contacts = level.arrows.associate { it.id to withdrawalContacts(it, segments) }
        val nearestFirst = contacts.mapValues { (_, distances) ->
            distances.entries.sortedBy { it.value }.toTypedArray()
        }
        val blockers = contacts.mapValues { (_, distances) -> distances.keys }
        val alive = level.arrows.map { it.id }.toMutableSet()
        var rounds = 0
        var openings = 0
        while (alive.isNotEmpty()) {
            val free = alive.filter { id -> blockers.getValue(id).none { it in alive } }
            if (rounds == 0) openings = free.size
            if (free.isEmpty()) fail("Independent movement deadlock on ${level.id}: $alive")
            rounds++
            alive.removeAll(free.toSet())
        }
        assertEquals("Level ${level.id} opening count", openings, solution.initiallyFree)
        assertEquals("Level ${level.id} dependency rounds", rounds, solution.maxDependencyDepth)

        val active = level.arrows.toMutableList()
        var checkedChoices = 0
        for ((step, move) in solution.moveSequence.withIndex()) {
            val activeIds = active.map { it.id }.toSet()
            val grid = Occupancy.of(level.cols, level.rows, active)
            for (candidate in active) {
                // Keep every choice and clearance check. Sorted independent
                // contacts avoid a temporary map for each blocked candidate.
                var firstContact = Double.POSITIVE_INFINITY
                val distances = nearestFirst.getValue(candidate.id)
                for (index in distances.indices) {
                    val contact = distances[index]
                    if (contact.key in activeIds) {
                        firstContact = contact.value
                        break
                    }
                }
                val independentlyFree = !firstContact.isFinite()
                val actualFree = CollisionEngine.isFree(candidate, grid, level.movementRule)
                if (independentlyFree != actualFree) {
                    fail("Level ${level.id} step $step candidate ${candidate.id}: expected free=$independentlyFree, got $actualFree")
                }
                if (!independentlyFree) {
                    val expectedClearance = firstContact.toInt() - 1
                    val actualClearance = CollisionEngine.freeCellsAhead(candidate, grid, level.movementRule)
                    if (expectedClearance != actualClearance) {
                        fail("Level ${level.id} step $step nudge for ${candidate.id}: expected $expectedClearance, got $actualClearance")
                    }
                }
                checkedChoices++
            }
            assertTrue("Level ${level.id} illegal solution move ${move.id}",
                move.id in activeIds && blockers.getValue(move.id).none { it in activeIds })
            val hint = PuzzleSolver.findHint(level, active)
            assertNotNull("Level ${level.id} lacks a hint at step $step", hint)
            assertTrue("Level ${level.id} gives a blocked hint",
                blockers.getValue(requireNotNull(hint).id).none { it in activeIds })
            assertTrue(active.removeAll { it.id == move.id })
        }
        assertTrue(active.isEmpty())
        return """    {"levelId":${level.id},"difficulty":"${level.difficulty}","movementRule":"${level.movementRule}","arrowCount":${level.arrowCount},"openings":$openings,"dependencyRounds":$rounds,"verifiedChoices":$checkedChoices,"solutionIds":${solution.moveSequence.map { it.id }}}"""
    }

    private fun withdrawalContacts(
        moving: ArrowPath,
        segments: Map<Int, List<Pair<Cell, Cell>>>
    ): Map<Int, Double> {
        val contacts = mutableMapOf<Int, Double>()
        for ((obstacleId, pairs) in segments) {
            for ((from, to) in pairs) {
                val distance = raySegmentContact(moving, from, to) ?: continue
                contacts[obstacleId] = minOf(contacts[obstacleId] ?: Double.POSITIVE_INFINITY, distance)
            }
        }
        return contacts
    }

    /** Segment/ray intersection in a local frame, independent of grid traversal. */
    private fun raySegmentContact(moving: ArrowPath, first: Cell, last: Cell): Double? {
        val ex = moving.exit.dCol
        val ey = moving.exit.dRow
        fun along(point: Cell): Int = (point.col - moving.head.col) * ex + (point.row - moving.head.row) * ey
        fun across(point: Cell): Int = -(point.col - moving.head.col) * ey + (point.row - moving.head.row) * ex
        val u0 = along(first)
        val u1 = along(last)
        val v0 = across(first)
        val v1 = across(last)
        if (v0 == 0 && v1 == 0) {
            if (maxOf(u0, u1) <= 0) return null
            return maxOf(minOf(u0, u1).toDouble(), Double.MIN_VALUE)
        }
        if ((v0 > 0 && v1 > 0) || (v0 < 0 && v1 < 0) || v0 == v1) return null
        val fraction = -v0.toDouble() / (v1 - v0)
        val distance = u0 + fraction * (u1 - u0)
        return distance.takeIf { it > 0.0 }
    }
}
