package com.amowpuzzle.aitricker.domain.model

/** Visual presentation is independent of a level's movement and difficulty. */
enum class LevelPresentation {
    CLASSIC,
    DARK_WHITE,
    GUITAR_WHITE,
    DOG_WHITE;

    /** All campaign boards share the expert board's white ink and dark surface. */
    val usesDarkBoard: Boolean get() = this != CLASSIC
}
