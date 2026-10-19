package com.amowpuzzle.aitricker.presentation.game

import androidx.lifecycle.ViewModelStore
import com.amowpuzzle.aitricker.data.prefs.GameProgressWriter
import com.amowpuzzle.aitricker.data.sound.SoundEngine
import com.amowpuzzle.aitricker.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Exercises the real ViewModel with an in-memory progress writer and deterministic clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val sound = SoundEngine().apply { enabled = false }
    private val writer = FakeWriter()
    private lateinit var model: GameViewModel

    // Arrow 1 must leave before arrow 2 can use the same upward lane.
    private val front = ArrowPath(1, listOf(Cell(1, 1), Cell(1, 0)), Direction.UP)
    private val rear = ArrowPath(2, listOf(Cell(1, 4), Cell(1, 3)), Direction.UP)
    private val fixture = Level(1, 4, 6, Difficulty.EASY, listOf(front, rear))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        model = GameViewModel(writer, sound, levelLoader = { fixture.copy(id = it) }, levelDispatcher = dispatcher)
        store.put("session", model)
    }

    @After
    fun tearDown() {
        store.clear()
        sound.release()
        Dispatchers.resetMain()
    }

    @Test
    fun `a stale touch with a reused ID cannot select different geometry`() = runTest {
        model.load(1); runCurrent()
        val stale = front.copy(cells = listOf(Cell(3, 2), Cell(3, 1)))
        model.tap(stale)
        assertEquals(0, model.state.value.movesMade)
        assertTrue(model.state.value.departing.isEmpty())
        assertEquals(3, model.state.value.lives)
        model.tap(front)
        assertEquals(1, model.state.value.movesMade)
    }

    @Test
    fun `accepted tap keeps the arrow visible and reserved until the matching callback`() = runTest {
        model.load(1); runCurrent()
        model.tap(front)
        val token = model.state.value.departing.single().token
        assertEquals(listOf(rear), model.state.value.activeArrows)
        assertFalse(model.state.value.acceptsInput)
        model.tap(rear)
        model.useHint()
        assertEquals(1, model.state.value.movesMade)
        assertEquals(3, model.state.value.lives)
        assertEquals(2, model.state.value.hintsRemaining)
        model.onDepartureFinished(token + 50)
        assertEquals(token, model.state.value.departing.single().token)
        model.onDepartureFinished(token)
        assertTrue(model.state.value.acceptsInput)
        model.tap(rear)
        assertEquals(2, model.state.value.movesMade)
        assertEquals(rear, model.state.value.departing.single().arrow)
    }

    @Test
    fun `time alone cannot win and final callback saves exactly one completion`() = runTest {
        model.load(1); runCurrent()
        model.tap(front); model.onDepartureFinished(model.state.value.departing.single().token)
        model.tap(rear)
        val finalToken = model.state.value.departing.single().token
        advanceTimeBy(5_000); runCurrent()
        assertEquals(GameStatus.PLAYING, model.state.value.status)
        assertTrue(writer.completed.isEmpty())
        model.onDepartureFinished(finalToken); runCurrent()
        assertEquals(GameStatus.VICTORY, model.state.value.status)
        assertEquals(listOf(1), writer.completed)
        model.onDepartureFinished(finalToken); runCurrent()
        assertEquals(listOf(1), writer.completed)
    }

    @Test
    fun `retry ignores the old callback even while the same arrow departs again`() = runTest {
        model.load(1); runCurrent()
        model.tap(front)
        val stale = model.state.value.departing.single().token
        model.retry(); runCurrent()
        model.tap(front)
        val fresh = model.state.value.departing.single().token
        assertNotEquals(stale, fresh)
        model.onDepartureFinished(stale)
        assertEquals(fresh, model.state.value.departing.single().token)
        assertEquals(1, model.state.value.movesMade)
        model.onDepartureFinished(fresh)
        assertTrue(model.state.value.acceptsInput)
    }

    @Test
    fun `blocked taps show the full nudge and repeated taps do not drain lives`() = runTest {
        model.load(1); runCurrent()
        model.tap(rear)
        assertEquals(2, model.state.value.lives)
        assertNotNull(model.state.value.blocked)
        model.tap(rear)
        assertEquals(2, model.state.value.lives)
        advanceTimeBy(FeedbackTiming.BLOCKED_MS.toLong()); runCurrent()
        assertNull(model.state.value.blocked)
        assertTrue(model.state.value.acceptsInput)
        model.tap(rear)
        advanceTimeBy(FeedbackTiming.BLOCKED_MS.toLong()); runCurrent()
        model.tap(rear)
        assertEquals(0, model.state.value.lives)
        assertEquals(GameStatus.PLAYING, model.state.value.status)
        advanceTimeBy(FeedbackTiming.BLOCKED_MS - 1L); runCurrent()
        assertNotNull(model.state.value.blocked)
        advanceTimeBy(1L); runCurrent()
        assertEquals(GameStatus.GAME_OVER, model.state.value.status)
    }

    @Test
    fun `latest load wins and old blocked work cannot mutate the next board`() = runTest {
        model.load(1)
        model.load(7)
        runCurrent()
        assertEquals(7, model.state.value.levelId)
        model.tap(rear)
        model.load(8); runCurrent()
        advanceTimeBy(2_000); runCurrent()
        assertEquals(8, model.state.value.levelId)
        assertEquals(3, model.state.value.lives)
        assertNull(model.state.value.blocked)
        assertEquals(0, model.state.value.movesMade)
    }

    @Test
    fun `completion pending in storage cannot switch a retried board to victory`() = runTest {
        writer.saveDelayMs = 1_000
        model.load(1); runCurrent()
        model.tap(front); model.onDepartureFinished(model.state.value.departing.single().token)
        model.tap(rear); model.onDepartureFinished(model.state.value.departing.single().token)
        runCurrent()
        model.retry(); runCurrent()
        advanceTimeBy(2_000); runCurrent()
        assertEquals(GameStatus.PLAYING, model.state.value.status)
        assertEquals(0, model.state.value.movesMade)
        assertEquals(2, model.state.value.activeArrows.size)
    }

    private class FakeWriter : GameProgressWriter {
        val completed = mutableListOf<Int>()
        var saveDelayMs = 0L
        override suspend fun onLevelCompleted(levelId: Int) {
            delay(saveDelayMs)
            completed.add(levelId)
        }
        override suspend fun setShowGrid(enabled: Boolean) = Unit
    }
}
