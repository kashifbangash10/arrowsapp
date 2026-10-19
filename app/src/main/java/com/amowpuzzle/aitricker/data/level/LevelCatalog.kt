package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Level
import java.util.concurrent.ConcurrentHashMap

/** 1,500 fixed puzzles, from Easy through Grandmaster, Legend, Extreme and Apex. */
object LevelCatalog {
    const val TOTAL_LEVELS = 1500
    private val cache = ConcurrentHashMap<Int, Level>()
    val levelIds: IntRange get() = 1..TOTAL_LEVELS

    fun difficultyOf(id: Int): Difficulty = when (id.coerceIn(1, TOTAL_LEVELS)) {
        in 1..5 -> Difficulty.EASY
        in 6..25 -> Difficulty.HARD
        in 26..100 -> Difficulty.EXPERT
        in 101..250 -> Difficulty.MASTER
        in 251..500 -> Difficulty.GRANDMASTER
        in 501..750 -> Difficulty.LEGEND
        in 751..1000 -> Difficulty.EXTREME
        else -> Difficulty.APEX
    }

    fun level(id: Int): Level {
        val key = id.coerceIn(1, TOTAL_LEVELS)
        cache[key]?.let { return it }
        val built = when (key) {
            6 -> ReferenceMazeLevel.level
            49 -> DogLevel.level
            50 -> GuitarLevel.level
            in 51..100 -> requireNotNull(ExpertExpansionLevels.level(key))
            in 101..250 -> requireNotNull(MasterCampaignLevels.level(key))
            in 251..500 -> requireNotNull(GrandmasterCampaignLevels.level(key))
            in 501..1000 -> requireNotNull(AdvancedCampaignLevels.level(key))
            in 1001..TOTAL_LEVELS -> requireNotNull(ApexCampaignLevels.level(key))
            else -> ShapeLevels.level(key) ?: FixedCampaignLevels.level(key)
        }
        check(built.id == key && built.difficulty == difficultyOf(key))
        return cache.putIfAbsent(key, built) ?: built
    }

    fun preload(range: Iterable<Int> = levelIds) = range.forEach { level(it) }
    fun isCached(id: Int) = cache.containsKey(id)
}
