package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test

class MasterCampaignLevelsTest {
    @Test
    fun `one hundred fifty fixed master boards occupy only the new campaign slots`() {
        assertEquals((101..250).toSet(), MasterCampaignLevels.ids)
        assertNull(MasterCampaignLevels.level(100))
        assertNull(MasterCampaignLevels.level(251))
        for (id in MasterCampaignLevels.ids) {
            val level = requireNotNull(MasterCampaignLevels.level(id))
            assertEquals("Master $id is not routed to the catalog", level, LevelCatalog.level(id))
            assertEquals(id, level.id)
            assertEquals(Difficulty.MASTER, level.difficulty)
            assertEquals(LevelPresentation.DARK_WHITE, level.presentation)
            assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
            assertFalse("Master $id is missing a shape name", MasterCampaignLevels.name(id).isNullOrBlank())
        }
    }

    @Test
    fun `each master exceeds expert dependency depth with tightly bounded opening moves`() {
        val deepestExpert = (26..100).maxOf { PuzzleSolver.solve(LevelCatalog.level(it)).maxDependencyDepth }
        for (id in MasterCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val solved = PuzzleSolver.solve(level)
            assertTrue("Master $id is unsolvable", solved.solvable)
            assertTrue("Master $id needs at least 210 arrows", level.arrowCount >= 210)
            assertTrue("Master $id needs at least 55 dependency rounds", solved.maxDependencyDepth >= 55)
            assertTrue("Master $id is shallower than the strongest existing Expert", solved.maxDependencyDepth > deepestExpert)
            assertTrue("Master $id offers too many obvious openings: ${solved.initiallyFree}/${level.arrowCount}",
                solved.initiallyFree in 1..maxOf(3, level.arrowCount / 20))
            if (solved.initiallyFree > 3) {
                assertTrue("Extra openings on $id require at least 60 rounds", solved.maxDependencyDepth >= 60)
            }
            assertTrue("Master $id needs winding paths", level.arrows.count { it.turnCount > 0 } >= level.arrowCount / 2)
            assertTrue("Master $id has no long multi-turn path", level.arrows.any { it.length >= 10 && it.turnCount >= 3 })
        }
    }

    @Test
    fun `all master silhouettes and playable layouts are distinct`() {
        val masks = MasterCampaignLevels.ids.map { id ->
            val cells = LevelCatalog.level(id).arrows.flatMap { it.cells }.toSet()
            val left = cells.minOf { it.col }
            val top = cells.minOf { it.row }
            cells.map { Cell(it.col - left, it.row - top) }
                .sortedWith(compareBy<Cell> { it.row }.thenBy { it.col })
                .joinToString(";") { "${it.col},${it.row}" }
        }
        assertEquals("Changing IDs, exits or translating a mask does not create a new silhouette", 150, masks.toSet().size)
        val layouts = MasterCampaignLevels.ids.map { id ->
            val level = LevelCatalog.level(id)
            "${level.cols}x${level.rows}:" + level.arrows.map { "${it.exit}:${it.cells}" }.sorted().joinToString("|")
        }
        assertEquals("Master repeats a playable layout", 150, layouts.toSet().size)
    }

    @Test
    fun `every stored master removal certificate clears its entire actual board`() {
        for (id in MasterCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val ids = MasterCampaignLevels.solutionIds(id)
            assertEquals("Master $id certificate length", level.arrowCount, ids.size)
            assertEquals("Master $id certificate identities", level.arrows.map { it.id }.toSet(), ids.toSet())
            val moves = ids.map { arrowId -> requireNotNull(level.arrowById(arrowId)) }
            assertTrue("Master $id authored certificate is not legal", PuzzleSolver.verify(level, moves))
        }
    }

    @Test
    fun `ten master categories contain fifteen matching boards each`() {
        val categories = CampaignChapters.master
        assertEquals(10, categories.size)
        assertEquals(10, categories.map { it.name }.toSet().size)
        assertEquals((101..250).toList(), categories.flatMap { it.levels.toList() })
        for ((index, chapter) in categories.withIndex()) {
            assertEquals(15, chapter.levels.count())
            assertEquals((101 + index * 15)..(115 + index * 15), chapter.levels)
            for (id in chapter.levels) {
                assertEquals("Master $id category differs between runtime and selector", chapter.name, MasterCampaignLevels.category(id))
            }
        }
    }
}
