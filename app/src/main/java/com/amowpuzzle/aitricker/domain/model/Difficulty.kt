package com.amowpuzzle.aitricker.domain.model

/** Difficulty label shown in the game header. */
enum class Difficulty(val label: String) {
    EASY("Easy"),
    NORMAL("Normal"),
    HARD("Hard"),
    EXPERT("Expert"),
    MASTER("Master"),
    GRANDMASTER("Grandmaster"),
    LEGEND("Legend"),
    EXTREME("Extreme"),
    APEX("Apex")
}
