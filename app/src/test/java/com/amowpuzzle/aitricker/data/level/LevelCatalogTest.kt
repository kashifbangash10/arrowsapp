package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validation of the whole shipped curve.
 *
 * Every explicit campaign level is solved and its complete removal sequence replayed.
 * Difficulty is checked against actual opening choices and dependency rounds, not
 * a requested generation target or an assigned label.
 *
 * Levels are cached inside [LevelCatalog], so the data expansion cost is paid once for
 * the whole class no matter how many test methods walk the list.
 */
class LevelCatalogTest {

    private val levels: List<Level> = LevelCatalog.levelIds.map { LevelCatalog.level(it) }

    @Test
    fun `the catalog ships exactly fifteen hundred levels`() {
        assertEquals(1500, LevelCatalog.TOTAL_LEVELS)
        assertEquals(1..1500, LevelCatalog.levelIds)
        assertEquals(1500, levels.size)
        assertEquals(LevelCatalog.levelIds.toList(), levels.map { it.id })
    }

    /** Out-of-range ids are clamped rather than thrown, so deep links cannot crash. */
    @Test
    fun `level ids outside the range are clamped`() {
        assertEquals(1, LevelCatalog.level(0).id)
        assertEquals(1, LevelCatalog.level(-7).id)
        assertEquals(1500, LevelCatalog.level(1501).id)
        assertEquals(1500, LevelCatalog.level(9999).id)
        assertTrue(LevelCatalog.isCached(1))
    }

    @Test
    fun `every arrow is well formed and could in principle escape`() {
        for (level in levels) {
            val where = "level ${level.id}"
            assertTrue("$where has no arrows", level.arrowCount > 0)
            assertEquals(
                "$where reuses an arrow id",
                level.arrowCount,
                level.arrows.map { it.id }.toHashSet().size
            )
            for (arrow in level.arrows) {
                assertTrue(
                    "$where arrow ${arrow.id} is malformed",
                    arrow.isWellFormed(level.cols, level.rows)
                )
                assertTrue(
                    "$where arrow ${arrow.id} blocks itself",
                    !arrow.isSelfBlocking(level.cols, level.rows)
                )
                assertTrue(
                    "$where arrow ${arrow.id} is too short",
                    arrow.length >= 2
                )
            }
        }
    }

    /** Overlapping arrows would make the board unreadable and the occupancy map a lie. */
    @Test
    fun `no two arrows share a cell`() {
        for (level in levels) {
            val cells = level.arrows.flatMap { it.cells }
            assertEquals(
                "level ${level.id} has overlapping arrows",
                cells.size,
                cells.toHashSet().size
            )
            val grid = Occupancy.of(level.cols, level.rows, level.arrows)
            for (arrow in level.arrows) {
                for (cell in arrow.cells) {
                    assertEquals(
                        "level ${level.id} cell $cell has the wrong owner",
                        arrow.id,
                        grid.ownerAt(cell)
                    )
                }
            }
        }
    }

    /** The assertion the whole game rests on: every shipped level can be finished. */
    @Test
    fun `every level is solvable and its solution replays cleanly`() {
        for (level in levels) {
            val where = "level ${level.id}"
            val solution = PuzzleSolver.solve(level)

            assertTrue("$where is unsolvable", solution.solvable)
            assertEquals(
                "$where solution does not clear the board",
                level.arrowCount,
                solution.moveSequence.size
            )
            assertEquals(
                "$where solution repeats an arrow",
                level.arrowCount,
                solution.moveSequence.map { it.id }.toHashSet().size
            )
            assertTrue(
                "$where solution fails to replay",
                PuzzleSolver.verify(level, solution.moveSequence)
            )
        }
    }

    @Test
    fun `stored fixed board certificates clear every authored regular level`() {
        val dedicatedIds = ShapeLevels.ids + ExpertExpansionLevels.ids + MasterCampaignLevels.ids + GrandmasterCampaignLevels.ids + AdvancedCampaignLevels.ids + ApexCampaignLevels.ids + setOf(6, 49, 50)
        for (level in levels.filter { it.id !in dedicatedIds }) {
            val ids = FixedCampaignLevels.solutionIds(level.id)
            assertEquals("Level ${level.id} certificate length", level.arrowCount, ids.size)
            assertEquals("Level ${level.id} certificate identities", level.arrows.map { it.id }.toSet(), ids.toSet())
            val moves = ids.map { id -> requireNotNull(level.arrowById(id)) }
            assertTrue("Level ${level.id} stored certificate fails actual movement", PuzzleSolver.verify(level, moves))
        }
    }

    @Test
    fun `every level opens with a legal move and can offer a hint`() {
        for (level in levels) {
            val where = "level ${level.id}"
            val solution = PuzzleSolver.solve(level)

            assertTrue("$where has no opening move", solution.initiallyFree >= 1)
            assertNotNull("$where cannot produce a hint", PuzzleSolver.findHint(level, level.arrows))
            assertEquals("$where starts with the wrong lives", 3, level.startingLives)
            assertEquals("$where starts with the wrong hints", 2, level.startingHints)
        }
    }

    /**
     * Depth is measured in rounds, so it can never exceed the arrow count and must be
     * at least one on any board that has arrows at all. A level whose depth equals its
     * arrow count is a single strict chain; one with depth 1 is a free-for-all.
     */
    @Test
    fun `dependency depth stays within its bounds`() {
        for (level in levels) {
            val depth = PuzzleSolver.solve(level).maxDependencyDepth
            assertTrue(
                "level ${level.id} depth $depth is out of range for ${level.arrowCount} arrows",
                depth in 1..level.arrowCount
            )
        }
    }

    @Test
    fun `difficulty labels follow the published bands`() {
        for (level in levels) {
            val expected = when (level.id) {
                in 1..5 -> Difficulty.EASY
                in 6..25 -> Difficulty.HARD
                in 26..100 -> Difficulty.EXPERT
                in 101..250 -> Difficulty.MASTER
                in 251..500 -> Difficulty.GRANDMASTER
                in 501..750 -> Difficulty.LEGEND
                in 751..1000 -> Difficulty.EXTREME
                else -> Difficulty.APEX
            }
            assertEquals("level ${level.id} label", expected, level.difficulty)
            assertEquals(
                "level ${level.id} disagrees with difficultyOf",
                expected,
                LevelCatalog.difficultyOf(level.id)
            )
        }
    }

    @Test
    fun `every campaign level uses the shared dark white presentation`() {
        for (level in levels) {
            if (level.id == 1) {
                assertEquals(LevelPresentation.CLASSIC, level.presentation)
            } else {
                assertTrue("Level ${level.id} still uses the old colored presentation", level.presentation.usesDarkBoard)
            }
            assertEquals("Level ${level.id} must follow its original route when leaving",
                MovementRule.HEAD_CORRIDOR, level.movementRule)
        }
    }

    @Test
    fun `each campaign level has a different playable layout`() {
        // IDs and labels cannot hide a repeated puzzle.
        val signatures = levels.map { level ->
            listOf(level.cols, level.rows, level.movementRule).toString() +
                level.arrows.map { arrow -> "${arrow.exit}:${arrow.cells}" }.sorted().joinToString("|")
        }
        assertEquals("Campaign contains duplicate puzzles", levels.size, signatures.toSet().size)
    }

    @Test
    fun `the first five levels teach play with small approachable boards`() {
        for (level in levels.take(5)) {
            val solution = PuzzleSolver.solve(level)
            assertTrue("Level ${level.id} is too crowded for onboarding", level.arrowCount in 3..10)
            assertTrue("Level ${level.id} should offer multiple openings", solution.initiallyFree >= 2)
            assertTrue("Level ${level.id} has an expert length chain", solution.maxDependencyDepth in 1..5)
        }
    }

    @Test
    fun `hard levels already contain substantial blocking chains`() {
        for (level in levels.filter { it.difficulty == Difficulty.HARD }) {
            val solution = PuzzleSolver.solve(level)
            assertTrue("Hard ${level.id} needs at least 35 arrows", level.arrowCount >= 35)
            assertTrue("Hard ${level.id} has only ${solution.maxDependencyDepth} dependency rounds",
                solution.maxDependencyDepth >= 10)
            assertTrue("Hard ${level.id} has too many obvious openings",
                solution.initiallyFree in 1..maxOf(4, level.arrowCount / 10))
        }
    }

    @Test
    fun `later rectangular boards are dense winding mazes`() {
        val silhouetteIds = ShapeLevels.ids + ExpertExpansionLevels.ids + MasterCampaignLevels.ids + GrandmasterCampaignLevels.ids + AdvancedCampaignLevels.ids + ApexCampaignLevels.ids + setOf(49, 50)
        for (level in levels.filter { it.id > 5 && it.id !in silhouetteIds }) {
            val occupied = level.arrows.sumOf { it.length }
            val density = occupied.toDouble() / (level.cols * level.rows)
            assertTrue("Level ${level.id} leaves excessive empty grid: $density", density >= 0.65)
            assertTrue("Level ${level.id} needs winding paths, not only short straight arrows",
                level.arrows.count { it.turnCount >= 1 } >= if (level.id == 6) 17 else (level.arrowCount + 1) / 2)
            assertTrue("Level ${level.id} has no long winding path",
                level.arrows.any { it.length >= 10 && it.turnCount >= 3 })
        }
    }

    @Test
    fun `expert levels have substantial measured dependencies`() {
        for (level in levels.filter { it.difficulty == Difficulty.EXPERT }) {
            val solution = PuzzleSolver.solve(level)
            assertTrue("Expert ${level.id} needs a substantial board", level.arrowCount >= 75)
            // Opening count must be judged with board size and dependency depth.
            // Large silhouettes can offer several independent edge exits while
            // keeping at least 93.75% of pieces blocked and long chains inside.
            // Smaller expert boards retain the strict three-opening limit.
            val openingLimit = maxOf(3, level.arrowCount / 16)
            assertTrue("Expert ${level.id} has too many opening moves: ${solution.initiallyFree} of ${level.arrowCount}",
                solution.initiallyFree in 1..openingLimit)
            assertTrue("Expert ${level.id} has only ${solution.maxDependencyDepth} dependency rounds",
                solution.maxDependencyDepth >= 20)
            if (solution.initiallyFree > 3) {
                assertTrue("Additional openings require deep interior dependencies", solution.maxDependencyDepth >= 30)
            }
        }
    }

    @Test
    fun `board size grows through grandmaster and dependency depth increases across every tier`() {
        // Raw values avoid the old score saturating at five dependency rounds.
        val groups = listOf(Difficulty.EASY, Difficulty.HARD, Difficulty.EXPERT, Difficulty.MASTER,
            Difficulty.GRANDMASTER, Difficulty.LEGEND, Difficulty.EXTREME, Difficulty.APEX)
            .map { difficulty -> levels.filter { it.difficulty == difficulty } }
        val counts = groups.map { group -> group.map { it.arrowCount }.average() }
        val depths = groups.map { group -> group.map { PuzzleSolver.solve(it).maxDependencyDepth }.average() }
        // Legend and Extreme reuse the readable silhouette grids; their extra
        // difficulty comes from longer dependencies, not a smaller cell pitch.
        for (i in 1..4) {
            assertTrue("Average arrow count does not rise through Grandmaster: $counts", counts[i] > counts[i - 1])
        }
        for (i in 1 until groups.size) {
            assertTrue("Average dependency depth does not rise: $depths", depths[i] > depths[i - 1])
        }
        assertTrue("Expert depth is not meaningfully above Easy: $depths", depths[2] >= depths.first() * 2)
    }
}
