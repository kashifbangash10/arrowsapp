package com.amowpuzzle.aitricker.data.level

/** Explicit chapter ranges keep navigation and progress aligned across campaign tiers. */
data class CampaignChapter(val name: String, val levels: IntRange) {
    val count: Int get() = levels.last - levels.first + 1
}

object CampaignChapters {
    val master: List<CampaignChapter> = listOf(
        CampaignChapter("Nature & Garden", 101..115),
        CampaignChapter("Woodland Animals", 116..130),
        CampaignChapter("Sea Life", 131..145),
        CampaignChapter("Birds", 146..160),
        CampaignChapter("Transport", 161..175),
        CampaignChapter("Space", 176..190),
        CampaignChapter("Music", 191..205),
        CampaignChapter("Food & Treats", 206..220),
        CampaignChapter("Buildings", 221..235),
        CampaignChapter("Treasures & Play", 236..250),
    )
    val grandmaster: List<CampaignChapter> = listOf(
        CampaignChapter("Safari", 251..260),
        CampaignChapter("Forest Friends", 261..270),
        CampaignChapter("Farm & Outback", 271..280),
        CampaignChapter("Reptiles & Amphibians", 281..290),
        CampaignChapter("Bird Wonders", 291..300),
        CampaignChapter("Deep Ocean", 301..310),
        CampaignChapter("Tiny Creatures", 311..320),
        CampaignChapter("Flower Studio", 321..330),
        CampaignChapter("Botanical", 331..340),
        CampaignChapter("Orchard & Harvest", 341..350),
        CampaignChapter("Sweet Bakery", 351..360),
        CampaignChapter("Kitchen Craft", 361..370),
        CampaignChapter("Tableware", 371..380),
        CampaignChapter("Hand Tools", 381..390),
        CampaignChapter("Craft & Study", 391..400),
        CampaignChapter("Land Vehicles", 401..410),
        CampaignChapter("Aviation", 411..420),
        CampaignChapter("Watercraft", 421..430),
        CampaignChapter("Musical Craft", 431..440),
        CampaignChapter("Sport & Toys", 441..450),
        CampaignChapter("Wearables", 451..460),
        CampaignChapter("Architecture", 461..470),
        CampaignChapter("Fantasy", 471..480),
        CampaignChapter("Technology", 481..490),
        CampaignChapter("Adventure", 491..500),
    )
    val legend: List<CampaignChapter> = grandmaster.map {
        CampaignChapter("Legend ${it.name}", (it.levels.first + 250)..(it.levels.last + 250))
    }
    val extreme: List<CampaignChapter> = grandmaster.map {
        CampaignChapter("Extreme ${it.name}", (it.levels.first + 500)..(it.levels.last + 500))
    }
    val apex: List<CampaignChapter> = grandmaster.map {
        CampaignChapter("Apex ${it.name}", (it.levels.first + 750)..(it.levels.last + 750))
    } + grandmaster.map {
        CampaignChapter("Apex Elite ${it.name}", (it.levels.first + 1000)..(it.levels.last + 1000))
    }
    val all: List<CampaignChapter> = listOf(
        CampaignChapter("Intro", 1..10),
        CampaignChapter("Paths", 11..20),
        CampaignChapter("Shapes", 21..30),
        CampaignChapter("Mazes", 31..40),
        CampaignChapter("Silhouettes", 41..50),
        CampaignChapter("Nature", 51..60),
        CampaignChapter("Animals", 61..70),
        CampaignChapter("Objects", 71..80),
        CampaignChapter("Sky", 81..90),
        CampaignChapter("Music", 91..100),
    ) + master + grandmaster + legend + extreme + apex

    fun indexOf(levelId: Int): Int = all.indexOfFirst {
        levelId.coerceIn(1, LevelCatalog.TOTAL_LEVELS) in it.levels
    }
}
