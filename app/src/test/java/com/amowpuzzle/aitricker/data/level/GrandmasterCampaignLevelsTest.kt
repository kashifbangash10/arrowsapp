package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import org.junit.Assert.*
import org.junit.Test

class GrandmasterCampaignLevelsTest {
    @Test
    fun `two hundred fifty fixed grandmaster boards load only in the new campaign slots`() {
        assertEquals((251..500).toSet(), GrandmasterCampaignLevels.ids)
        assertNull(GrandmasterCampaignLevels.level(250))
        assertNull(GrandmasterCampaignLevels.level(501))
        val names = mutableSetOf<String>()
        for (id in GrandmasterCampaignLevels.ids) {
            val level = requireNotNull(GrandmasterCampaignLevels.level(id))
            assertEquals("Grandmaster $id catalog routing", level, LevelCatalog.level(id))
            assertEquals(id, level.id)
            assertEquals(Difficulty.GRANDMASTER, level.difficulty)
            assertEquals(LevelPresentation.DARK_WHITE, level.presentation)
            assertEquals(MovementRule.HEAD_CORRIDOR, level.movementRule)
            val name = requireNotNull(GrandmasterCampaignLevels.name(id))
            assertTrue("Grandmaster $id needs a descriptive shape name", name.isNotBlank())
            assertTrue("Grandmaster repeats shape name $name", names.add(name))
        }
    }

    @Test
    fun `each grandmaster exceeds the strongest master chain and bounds available openings`() {
        val deepestMaster = MasterCampaignLevels.ids.maxOf { PuzzleSolver.solve(LevelCatalog.level(it)).maxDependencyDepth }
        for (id in GrandmasterCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val solved = PuzzleSolver.solve(level)
            assertTrue("Grandmaster $id is unsolvable", solved.solvable)
            assertTrue("Grandmaster $id needs at least 300 arrows", level.arrowCount >= 300)
            assertTrue("Grandmaster $id needs at least 115 dependency rounds", solved.maxDependencyDepth >= 115)
            assertTrue("Grandmaster $id is shallower than an existing Master", solved.maxDependencyDepth > deepestMaster)
            assertTrue("Grandmaster $id offers too many openings: ${solved.initiallyFree}/${level.arrowCount}",
                solved.initiallyFree in 1..maxOf(3, level.arrowCount / 25))
            if (solved.initiallyFree > 5) {
                assertTrue("Extra openings on $id require at least 125 rounds", solved.maxDependencyDepth >= 125)
            }
            assertTrue("Grandmaster $id needs winding paths", level.arrows.count { it.turnCount > 0 } >= level.arrowCount / 2)
            assertTrue("Grandmaster $id lacks a long winding path", level.arrows.any { it.length >= 10 && it.turnCount >= 3 })
        }
    }

    @Test
    fun `every grandmaster is a single connected silhouette with a distinct normalized mask`() {
        val masks = mutableSetOf<String>()
        val existingMasks = (1..250).map { normalizedMask(LevelCatalog.level(it).arrows.flatMap { arrow -> arrow.cells }.toSet()) }.toSet()
        for (id in GrandmasterCampaignLevels.ids) {
            val cells = LevelCatalog.level(id).arrows.flatMap { it.cells }.toSet()
            val normalized = normalizedMask(cells)
            assertTrue("Grandmaster $id repeats another new silhouette", masks.add(normalized))
            assertFalse("Grandmaster $id repeats an existing silhouette", normalized in existingMasks)
            val remaining = cells.toMutableSet()
            val pending = ArrayDeque<Cell>()
            val first = remaining.first()
            pending.addLast(first)
            remaining.remove(first)
            while (pending.isNotEmpty()) {
                val cell = pending.removeFirst()
                for (neighbor in listOf(Cell(cell.col - 1, cell.row), Cell(cell.col + 1, cell.row),
                    Cell(cell.col, cell.row - 1), Cell(cell.col, cell.row + 1))) {
                    if (remaining.remove(neighbor)) pending.addLast(neighbor)
                }
            }
            assertTrue("Grandmaster $id contains detached shapes or stranded contour cells: ${remaining.size}", remaining.isEmpty())
        }
        assertEquals(250, masks.size)
    }

    @Test
    fun `every stored grandmaster removal certificate clears the actual loaded board`() {
        for (id in GrandmasterCampaignLevels.ids) {
            val level = LevelCatalog.level(id)
            val ids = GrandmasterCampaignLevels.solutionIds(id)
            assertEquals("Grandmaster $id certificate length", level.arrowCount, ids.size)
            assertEquals("Grandmaster $id certificate identities", level.arrows.map { it.id }.toSet(), ids.toSet())
            val moves = ids.map { arrowId -> requireNotNull(level.arrowById(arrowId)) }
            assertTrue("Grandmaster $id stored certificate fails actual movement", PuzzleSolver.verify(level, moves))
        }
    }

    @Test
    fun `twenty five new categories contain ten matching grandmaster boards each`() {
        val categories = CampaignChapters.grandmaster
        assertEquals(25, categories.size)
        assertEquals(25, categories.map { it.name }.toSet().size)
        assertEquals(categories, CampaignChapters.all.subList(20, 45))
        assertEquals((251..500).toList(), categories.flatMap { it.levels.toList() })
        for ((index, chapter) in categories.withIndex()) {
            assertEquals(10, chapter.count)
            assertEquals((251 + index * 10)..(260 + index * 10), chapter.levels)
            for (id in chapter.levels) {
                assertEquals("Grandmaster $id runtime/selector category differs", chapter.name, GrandmasterCampaignLevels.category(id))
            }
        }
    }

    private fun normalizedMask(cells: Set<Cell>): String {
        val left = cells.minOf { it.col }
        val top = cells.minOf { it.row }
        return cells.map { Cell(it.col - left, it.row - top) }
            .sortedWith(compareBy<Cell> { it.row }.thenBy { it.col })
            .joinToString(";") { "${it.col},${it.row}" }
    }
}
