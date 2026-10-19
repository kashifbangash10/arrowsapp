package com.amowpuzzle.aitricker.data.level

import org.junit.Assert.*
import org.junit.Test

class CampaignChaptersTest {
    @Test
    fun `chapters cover the full catalog exactly once and retain the original ten worlds`() {
        assertEquals(145, CampaignChapters.all.size)
        assertEquals(LevelCatalog.levelIds.toList(), CampaignChapters.all.flatMap { it.levels.toList() })
        for (index in 0 until 10) {
            assertEquals((index * 10 + 1)..(index * 10 + 10), CampaignChapters.all[index].levels)
        }
        assertEquals(CampaignChapters.master, CampaignChapters.all.subList(10, 20))
        assertEquals(CampaignChapters.grandmaster, CampaignChapters.all.subList(20, 45))
        assertEquals(CampaignChapters.legend, CampaignChapters.all.subList(45, 70))
        assertEquals(CampaignChapters.extreme, CampaignChapters.all.subList(70, 95))
        assertEquals(CampaignChapters.apex, CampaignChapters.all.subList(95, 145))
    }

    @Test
    fun `resume finds correct worlds across every difficulty and category boundary`() {
        for (id in LevelCatalog.levelIds) {
            val index = CampaignChapters.indexOf(id)
            assertTrue("Level $id has no selectable chapter", index in CampaignChapters.all.indices)
            assertTrue("Level $id would resume in the wrong chapter", id in CampaignChapters.all[index].levels)
        }
        assertEquals(9, CampaignChapters.indexOf(100))
        assertEquals(10, CampaignChapters.indexOf(101))
        assertEquals(10, CampaignChapters.indexOf(115))
        assertEquals(11, CampaignChapters.indexOf(116))
        assertEquals(19, CampaignChapters.indexOf(250))
        assertEquals(20, CampaignChapters.indexOf(251))
        assertEquals(20, CampaignChapters.indexOf(260))
        assertEquals(21, CampaignChapters.indexOf(261))
        assertEquals(44, CampaignChapters.indexOf(500))
        assertEquals(45, CampaignChapters.indexOf(501))
        assertEquals(45, CampaignChapters.indexOf(510))
        assertEquals(46, CampaignChapters.indexOf(511))
        assertEquals(69, CampaignChapters.indexOf(750))
        assertEquals(70, CampaignChapters.indexOf(751))
        assertEquals(70, CampaignChapters.indexOf(760))
        assertEquals(71, CampaignChapters.indexOf(761))
        assertEquals(94, CampaignChapters.indexOf(1000))
        assertEquals(0, CampaignChapters.indexOf(0))
        assertEquals(95, CampaignChapters.indexOf(1001))
        assertEquals(95, CampaignChapters.indexOf(1010))
        assertEquals(96, CampaignChapters.indexOf(1011))
        assertEquals(119, CampaignChapters.indexOf(1250))
        assertEquals(120, CampaignChapters.indexOf(1251))
        assertEquals(144, CampaignChapters.indexOf(1500))
        assertEquals(144, CampaignChapters.indexOf(1501))
    }
}
