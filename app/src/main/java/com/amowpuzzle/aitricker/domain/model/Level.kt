package com.amowpuzzle.aitricker.domain.model

/** A complete, playable puzzle. */
data class Level(
    val id: Int,
    val cols: Int,
    val rows: Int,
    val difficulty: Difficulty,
    val arrows: List<ArrowPath>,
    val startingLives: Int = 3,
    val startingHints: Int = 2,
    val presentation: LevelPresentation = LevelPresentation.CLASSIC,
    val movementRule: MovementRule = MovementRule.HEAD_CORRIDOR
) {
    val arrowCount: Int get() = arrows.size

    fun arrowById(id: Int): ArrowPath? = arrows.firstOrNull { it.id == id }
}
