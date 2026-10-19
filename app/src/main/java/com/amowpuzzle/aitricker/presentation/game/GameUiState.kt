package com.amowpuzzle.aitricker.presentation.game

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Level

/** Interaction timings. Victory is driven by the renderer's completion callback. */
object FeedbackTiming {
    /** Brief stationary feedback before an accepted arrow begins moving. */
    const val PRESS_MS = 80
    const val MIN_ESCAPE_MS = 360
    const val MAX_ESCAPE_MS = 760

    /** How long the "that one is blocked" bump lasts. */
    const val BLOCKED_MS = 300

    /** How long the tap ripple ring blooms and fades after a successful tap. */
    const val RIPPLE_MS = 380

    /** How long a hint keeps glowing before it fades on its own. */
    const val HINT_MS = 2600

    /** Ignores a second tap that lands within this window of the previous one. */
    const val TAP_DEBOUNCE_MS = 110L
}

/** Everything the game screen draws, in one immutable snapshot. */
data class GameUiState(
    val level: Level? = null,
    val isLoading: Boolean = true,
    /** Arrows still on the board. Tapped arrows leave this list immediately. */
    val activeArrows: List<ArrowPath> = emptyList(),
    /** Arrows already removed from play that are still flying off screen. */
    val departing: List<DepartingArrow> = emptyList(),
    val blocked: BlockedFeedback? = null,
    val hintArrowId: Int? = null,
    val lives: Int = 3,
    val hintsRemaining: Int = 2,
    val movesMade: Int = 0,
    val status: GameStatus = GameStatus.PLAYING
) {
    val levelId: Int get() = level?.id ?: 0
    val totalArrows: Int get() = level?.arrowCount ?: 0
    val remainingArrows: Int get() = activeArrows.size
    val difficulty: Difficulty? get() = level?.difficulty

    val canUseHint: Boolean
        get() = acceptsInput && hintsRemaining > 0 && activeArrows.isNotEmpty()

    val acceptsInput: Boolean
        get() = !isLoading && status == GameStatus.PLAYING &&
            departing.isEmpty() && blocked == null && lives > 0 && !hasFinishedBoard

    /** No fixed delay can safely replace waiting for every visible tail to leave. */
    val hasFinishedBoard: Boolean
        get() = level != null && !isLoading && totalArrows > 0 &&
            movesMade == totalArrows && activeArrows.isEmpty() && departing.isEmpty()
}

/**
 * An arrow mid-escape. [token] is unique per departure so the board can animate two
 * arrows that happen to have the same id in quick succession, and so a recomposition
 * never restarts a flight that is already under way.
 */
data class DepartingArrow(val arrow: ArrowPath, val token: Long)

/**
 * A rejected tap. [cellsAhead] is how far the arrow could actually move, so the bump
 * animation nudges it right up against whatever is in its way.
 */
data class BlockedFeedback(val arrowId: Int, val cellsAhead: Int, val token: Long)
