package com.amowpuzzle.aitricker.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class CampaignProgressTest {
    @Test fun `old fifty-level completion unlocks the expansion without resetting saves`() {
        assertEquals(51, unlockedThrough(50, (1..50).toSet(), 100))
    }
    @Test fun `a partial campaign keeps its unlocked level`() {
        assertEquals(28, unlockedThrough(28, (1..24).toSet(), 100))
        assertEquals(26, unlockedThrough(25, (1..25).toSet(), 100))
    }
    @Test fun `new and finished campaigns remain in bounds`() {
        assertEquals(1, unlockedThrough(1, emptySet(), 100))
        assertEquals(100, unlockedThrough(100, (1..100).toSet(), 100))
    }
    @Test fun `invalid stored completion IDs cannot unlock the campaign`() {
        assertEquals(1, unlockedThrough(-8, setOf(-1, 0, 101, Int.MAX_VALUE), 100))
    }
    @Test fun `completed one hundred level save unlocks master without losing progress`() {
        val completed = (1..100).toSet()
        assertEquals(101, unlockedThrough(100, completed, 250))
    }
    @Test fun `master progress and final level remain in bounds`() {
        assertEquals(176, unlockedThrough(175, (1..175).toSet(), 250))
        assertEquals(250, unlockedThrough(249, (1..249).toSet(), 250))
        assertEquals(250, unlockedThrough(250, (1..250).toSet(), 250))
        assertEquals(250, unlockedThrough(Int.MAX_VALUE, emptySet(), 250))
        assertEquals(1, unlockedThrough(-1, setOf(-1, 0, 251, Int.MAX_VALUE), 250))
    }
    @Test fun `completed master save unlocks grandmaster without resetting completed levels`() {
        assertEquals(251, unlockedThrough(250, (1..250).toSet(), 500))
        assertEquals(101, unlockedThrough(100, (1..100).toSet(), 500))
    }
    @Test fun `grandmaster progress and five hundred completion stay bounded`() {
        assertEquals(376, unlockedThrough(375, (1..375).toSet(), 500))
        assertEquals(500, unlockedThrough(499, (1..499).toSet(), 500))
        assertEquals(500, unlockedThrough(500, (1..500).toSet(), 500))
        assertEquals(500, unlockedThrough(Int.MAX_VALUE, emptySet(), 500))
        assertEquals(1, unlockedThrough(-1, setOf(-1, 0, 501, Int.MAX_VALUE), 500))
    }
}
