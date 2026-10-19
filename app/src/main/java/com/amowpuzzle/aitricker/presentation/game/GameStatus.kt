package com.amowpuzzle.aitricker.presentation.game

/** Which phase of a play session the screen is showing. */
enum class GameStatus {
    /** Normal play: the board accepts taps. */
    PLAYING,

    /** Every arrow is gone - the completion screen takes over. */
    VICTORY,

    /** All hearts are spent; the board is frozen behind the retry card. */
    GAME_OVER
}
