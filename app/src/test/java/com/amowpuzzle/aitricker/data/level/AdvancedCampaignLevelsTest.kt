package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test

class AdvancedCampaignLevelsTest {
    @Test
    fun `five hundred fixed advanced boards load in the correct campaign slots`() {
        assertEquals((501..1000).toSet(), AdvancedCampaignLevels.ids)
        for (outsideId in listOf(500, 1001)) {
            assertNull(AdvancedCampaignLevels.level(outsideId))
            assertNull(AdvancedCampaignLevels.name(outsideId))
            assertNull(AdvancedCampaignLevels.category(outsideId))
            assertTrue(AdvancedCampaignLevels.solutionIds(outsideId).isEmpty())
        }
        val names = mutableSetOf<String>()
        for (id in AdvancedCampaignLevels.ids) {
            val level = requireNotNull(AdvancedCampaignLevels.level(id))
            assertEquals("Advanced $id catalog routing", level, LevelCatalog.level(id))
            assertSame("Advanced $id changes on reload", level, AdvancedCampaignLevels.level(id))
            assertEquals(id, level.id)
            assertEquals(if (id <= 750) Difficulty.LEGEND else Difficulty.EXTREME, level.difficulty)
            assertEquals(LevelPresentation.DARK_WHITE, level.presentation)
            assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
            val name = requireNotNull(AdvancedCampaignLevels.name(id))
            assertTrue("Advanced $id needs a descriptive shape name", name.isNotBlank())
            assertTrue("Advanced $id repeats the name $name", names.add(name))
        }
    }

    @Test
    fun `every stored advanced removal certificate clears the actual loaded board`() {
        for (id in AdvancedCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val ids = AdvancedCampaignLevels.solutionIds(id)
            assertEquals("Advanced $id certificate length", level.arrowCount, ids.size)
            assertEquals("Advanced $id certificate identities", level.arrows.map { it.id }.toSet(), ids.toSet())
            val moves = ids.map { arrowId -> requireNotNull(level.arrowById(arrowId)) }
            assertTrue("Advanced $id stored certificate fails actual movement", PuzzleSolver.verify(level, moves))
        }
    }

    @Test
    fun `legend and extreme have strictly separated dependency bands with at most five openings`() {
        val deepestGrandmaster = GrandmasterCampaignLevels.ids.maxOf {
            PuzzleSolver.solve(LevelCatalog.level(it)).maxDependencyDepth
        }
        for (id in AdvancedCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val solved = PuzzleSolver.solve(level)
            val expectedDepth = if (id <= 750) 190..249 else 275..600
            assertTrue("Advanced $id is unsolvable", solved.solvable)
            assertTrue("Advanced $id depth ${solved.maxDependencyDepth} is outside $expectedDepth",
                solved.maxDependencyDepth in expectedDepth)
            assertTrue("Advanced $id must exceed every Grandmaster dependency chain",
                solved.maxDependencyDepth > deepestGrandmaster)
            assertTrue("Advanced $id offers too many openings: ${solved.initiallyFree}/${level.arrowCount}",
                solved.initiallyFree in 1..5)
        }
    }

    @Test
    fun `advanced difficulty preserves each source silhouette and its readable grid dimensions`() {
        for (id in AdvancedCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val source = LevelCatalog.level(251 + (id - 501) % 250)
            assertEquals("Advanced $id resized its source grid width", source.cols, level.cols)
            assertEquals("Advanced $id resized its source grid height", source.rows, level.rows)
            assertEquals("Advanced $id changed its source silhouette",
                source.arrows.flatMap { it.cells }.toSet(), level.arrows.flatMap { it.cells }.toSet())
        }
    }

    @Test
    fun `fifty advanced categories each contain ten boards with matching runtime metadata`() {
        assertEquals(25, CampaignChapters.legend.size)
        assertEquals(25, CampaignChapters.extreme.size)
        val categories = CampaignChapters.legend + CampaignChapters.extreme
        assertEquals(50, categories.map { it.name }.toSet().size)
        assertEquals(categories, CampaignChapters.all.subList(45, 95))
        assertEquals((501..1000).toList(), categories.flatMap { it.levels.toList() })
        for ((index, chapter) in categories.withIndex()) {
            assertEquals(10, chapter.count)
            assertEquals((501 + index * 10)..(510 + index * 10), chapter.levels)
            for (id in chapter.levels) {
                assertEquals("Advanced $id runtime/selector category differs",
                    chapter.name, AdvancedCampaignLevels.category(id))
                assertEquals("Advanced $id selector category position differs",
                    45 + index, CampaignChapters.indexOf(id))
            }
        }
    }
}
