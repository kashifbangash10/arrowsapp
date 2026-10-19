package com.amowpuzzle.aitricker.presentation.game

import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure half of the presentation layer: the derived fields the screen reads and the
 * progress rules the menu reads. Both are plain data with no Android in them, so they
 * can be pinned down on the JVM - unlike [GameViewModel], which needs a Context to
 * reach DataStore and belongs in an instrumentation test.
 */
class GameStateTest {

    private fun straight(id: Int, from: Cell, to: Cell): ArrowPath =
        ArrowPath(id, listOf(from, to), requireNotNull(Direction.between(from, to)))

    private val arrows = listOf(
        straight(1, Cell(0, 1), Cell(0, 0)),
        straight(2, Cell(2, 1), Cell(2, 0)),
        straight(3, Cell(4, 1), Cell(4, 0))
    )

    private val level = Level(
        id = 12,
        cols = 5,
        rows = 5,
        difficulty = Difficulty.EASY,
        arrows = arrows
    )

    private fun playing(
        activeArrows: List<ArrowPath> = arrows,
        lives: Int = 3,
        hintsRemaining: Int = 2,
        status: GameStatus = GameStatus.PLAYING
    ) = GameUiState(
        level = level,
        isLoading = false,
        activeArrows = activeArrows,
        lives = lives,
        hintsRemaining = hintsRemaining,
        status = status
    )

    /** Nothing is drawn before a level arrives, so every derived field has to be safe. */
    @Test
    fun `the empty state is loading and inert`() {
        val state = GameUiState()

        assertTrue(state.isLoading)
        assertNull(state.level)
        assertNull(state.difficulty)
        assertEquals(0, state.levelId)
        assertEquals(0, state.totalArrows)
        assertEquals(0, state.remainingArrows)
        assertFalse(state.acceptsInput)
        assertFalse(state.canUseHint)
    }

    @Test
    fun `header fields come from the level and the live board`() {
        val state = playing(activeArrows = arrows.drop(1))

        assertEquals(12, state.levelId)
        assertEquals(3, state.totalArrows)
        assertEquals(2, state.remainingArrows)
        assertEquals(Difficulty.EASY, state.difficulty)
        assertEquals(3, state.lives)
    }

    @Test
    fun `taps are accepted only while a loaded board is still in play`() {
        assertTrue(playing().acceptsInput)
        assertFalse(playing().copy(isLoading = true).acceptsInput)
        assertFalse(playing(status = GameStatus.VICTORY).acceptsInput)
        assertFalse(playing(status = GameStatus.GAME_OVER).acceptsInput)
    }

    /**
     * The hint button has to switch itself off in three separate situations, and each
     * one of them is a state the player can reach: spent hints, a lost board, and the
     * frame between the last arrow leaving and the victory screen appearing.
     */
    @Test
    fun `the hint button is offered only when a hint could do something`() {
        assertTrue(playing().canUseHint)
        assertFalse(playing(hintsRemaining = 0).canUseHint)
        assertFalse(playing(activeArrows = emptyList()).canUseHint)
        assertFalse(playing(status = GameStatus.GAME_OVER).canUseHint)
        assertFalse(playing(status = GameStatus.VICTORY).canUseHint)
    }

    @Test
    fun `feedback carries a token so repeat animations restart cleanly`() {
        val first = DepartingArrow(arrows[0], token = 1L)
        val second = DepartingArrow(arrows[0], token = 2L)
        val blockedOnce = BlockedFeedback(arrowId = 1, cellsAhead = 2, token = 1L)
        val blockedTwice = BlockedFeedback(arrowId = 1, cellsAhead = 2, token = 2L)

        assertEquals(first.arrow, second.arrow)
        assertTrue(first != second)
        assertTrue(blockedOnce != blockedTwice)
    }

    @Test
    fun `feedback timings are long enough to read and short enough to feel instant`() {
        assertTrue(FeedbackTiming.PRESS_MS in 60..100)
        assertTrue(FeedbackTiming.MIN_ESCAPE_MS >= 300)
        assertTrue(FeedbackTiming.MAX_ESCAPE_MS > FeedbackTiming.MIN_ESCAPE_MS)
        assertTrue(FeedbackTiming.PRESS_MS + FeedbackTiming.MAX_ESCAPE_MS <= 900)
        assertEquals(300, FeedbackTiming.BLOCKED_MS)
        assertTrue(FeedbackTiming.HINT_MS > FeedbackTiming.MAX_ESCAPE_MS)
        assertTrue(FeedbackTiming.TAP_DEBOUNCE_MS in 60L..200L)
    }

    @Test
    fun `a visible departing arrow reserves input and prevents early completion`() {
        val flying = playing(activeArrows = emptyList()).copy(
            movesMade = level.arrowCount,
            departing = listOf(DepartingArrow(arrows.last(), 91L))
        )
        assertFalse(flying.acceptsInput)
        assertFalse(flying.canUseHint)
        assertFalse(flying.hasFinishedBoard)
        val finished = flying.copy(departing = emptyList())
        assertTrue(finished.hasFinishedBoard)
        assertFalse(finished.acceptsInput)
        assertFalse(GameUiState().hasFinishedBoard)
    }

    @Test
    fun `blocked feedback prevents repeated life loss while the nudge is visible`() {
        val nudging = playing().copy(blocked = BlockedFeedback(arrows.first().id, 0, 17L))
        assertFalse(nudging.acceptsInput)
        assertFalse(nudging.canUseHint)
        assertFalse(playing(lives = 0).acceptsInput)
    }

    @Test
    fun `a fresh install opens the whole campaign without claiming any wins`() {
        val fresh = GamePreferences.Snapshot()

        assertEquals(LevelCatalog.TOTAL_LEVELS, fresh.highestUnlocked)
        LevelCatalog.levelIds.forEach { assertTrue(fresh.isUnlocked(it)) }
        assertFalse(fresh.isUnlocked(0))
        assertFalse(fresh.isUnlocked(-1))
        assertFalse(fresh.isUnlocked(LevelCatalog.TOTAL_LEVELS + 1))
        assertTrue(fresh.completed.isEmpty())
        assertFalse(fresh.isCompleted(1))
        assertEquals(1, fresh.resumeLevel)
        assertFalse(fresh.tutorialShown)
        assertFalse(fresh.showGrid)
        assertTrue(fresh.soundEnabled)
        assertTrue(fresh.musicEnabled)
        assertTrue(fresh.hapticsEnabled)
    }

    @Test
    fun `old progress opens all new tiers while retaining earned wins and resume position`() {
        val progressed = GamePreferences.Snapshot(highestUnlocked = 257, completed = (1..256).toSet())

        assertTrue(progressed.isUnlocked(250))
        assertTrue(progressed.isUnlocked(257))
        assertTrue(progressed.isUnlocked(258))
        assertTrue(progressed.isUnlocked(501))
        assertTrue(progressed.isUnlocked(751))
        assertTrue(progressed.isUnlocked(1000))
        assertTrue(progressed.isUnlocked(1001))
        assertTrue(progressed.isUnlocked(1500))
        assertTrue(progressed.isCompleted(256))
        assertFalse(progressed.isCompleted(257))
        assertEquals(257, progressed.resumeLevel)
    }

    /**
     * Play resumes at the first level the player has *not* finished, not the highest
     * one they have unlocked - so someone who skipped level 4 and cleared 5 is taken
     * back to 4 rather than pushed forward.
     */
    @Test
    fun `resume finds the first gap in the completed set`() {
        val skipped = GamePreferences.Snapshot(highestUnlocked = 6, completed = setOf(1, 2, 3, 5))

        assertEquals(4, skipped.resumeLevel)
    }

    @Test
    fun `resume stays on the last level once everything is finished`() {
        val finished = GamePreferences.Snapshot(
            highestUnlocked = LevelCatalog.TOTAL_LEVELS,
            completed = LevelCatalog.levelIds.toSet()
        )

        assertEquals(LevelCatalog.TOTAL_LEVELS, finished.resumeLevel)
        assertTrue(finished.isUnlocked(LevelCatalog.TOTAL_LEVELS))
        assertFalse(finished.isUnlocked(LevelCatalog.TOTAL_LEVELS + 1))
    }
}
