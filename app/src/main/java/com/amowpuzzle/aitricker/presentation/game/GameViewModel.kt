package com.amowpuzzle.aitricker.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amowpuzzle.aitricker.data.haptics.Haptics
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.data.prefs.GameProgressWriter
import com.amowpuzzle.aitricker.data.sound.SoundEngine
import com.amowpuzzle.aitricker.domain.engine.CollisionEngine
import com.amowpuzzle.aitricker.domain.engine.Occupancy
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Level
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the play session. An accepted arrow remains reserved in occupancy until its
 * visible tail has left. Flights are serialized, so a newly freed arrow cannot
 * pass through its departing predecessor. Renderer callbacks commit removals.
 */
class GameViewModel(
    private val preferences: GameProgressWriter,
    private val sound: SoundEngine,
    private val haptics: Haptics = Haptics.None,
    private val levelLoader: (Int) -> Level = { LevelCatalog.level(it) },
    private val levelDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()
    private var board: Occupancy? = null
    private var hintJob: Job? = null
    private var blockedJob: Job? = null
    private var autoPlayJob: Job? = null
    private var loadJob: Job? = null
    private var completionJob: Job? = null
    private var lastTapAt = 0L
    private var tokens = 0L
    private var session = 0L

    fun load(levelId: Int) {
        val generation = ++session
        loadJob?.cancel()
        hintJob?.cancel()
        blockedJob?.cancel()
        autoPlayJob?.cancel()
        completionJob?.cancel()
        completionJob = null
        board = null
        lastTapAt = 0L
        _state.value = GameUiState(isLoading = true)
        loadJob = viewModelScope.launch {
            val level = withContext(levelDispatcher) { levelLoader(levelId) }
            if (generation != session) return@launch
            board = Occupancy.of(level.cols, level.rows, level.arrows)
            _state.value = GameUiState(
                level = level, isLoading = false, activeArrows = level.arrows,
                lives = level.startingLives, hintsRemaining = level.startingHints,
                status = GameStatus.PLAYING
            )
            if (level.id < LevelCatalog.TOTAL_LEVELS) {
                withContext(levelDispatcher) { levelLoader(level.id + 1) }
            }
        }
    }

    fun retry() {
        val id = _state.value.levelId
        if (id > 0) load(id)
    }

    fun tap(arrow: ArrowPath) {
        val current = _state.value
        val level = current.level ?: return
        val grid = board ?: return
        if (!current.acceptsInput) return
        // Reject a stale touch from a previous board that reused this arrow ID.
        val selected = current.activeArrows.firstOrNull { it == arrow } ?: return
        val now = System.nanoTime() / 1_000_000L
        if (lastTapAt != 0L && now - lastTapAt < FeedbackTiming.TAP_DEBOUNCE_MS) return
        lastTapAt = now
        val token = ++tokens
        if (CollisionEngine.isFree(selected, grid, level.movementRule)) {
            _state.value = current.copy(
                activeArrows = current.activeArrows.filterNot { it.id == selected.id },
                departing = listOf(DepartingArrow(selected, token)),
                hintArrowId = null, blocked = null, movesMade = current.movesMade + 1
            )
            hintJob?.cancel()
            sound.playEscape()
            haptics.tap()
        } else {
            val ahead = CollisionEngine.freeCellsAhead(selected, grid, level.movementRule)
            val lives = (current.lives - 1).coerceAtLeast(0)
            val generation = session
            _state.value = current.copy(lives = lives, blocked = BlockedFeedback(selected.id, ahead, token))
            sound.playBlocked()
            haptics.reject()
            blockedJob?.cancel()
            blockedJob = viewModelScope.launch {
                delay(FeedbackTiming.BLOCKED_MS.toLong())
                if (generation != session) return@launch
                _state.update { state ->
                    if (state.blocked?.token != token) state else state.copy(
                        blocked = null,
                        status = if (state.lives == 0) GameStatus.GAME_OVER else state.status
                    )
                }
                lastTapAt = 0L
            }
        }
    }

    fun useHint() {
        val current = _state.value
        val level = current.level ?: return
        if (!current.canUseHint) return
        val suggestion = PuzzleSolver.findHint(level, current.activeArrows) ?: return
        val generation = session
        _state.value = current.copy(hintsRemaining = current.hintsRemaining - 1, hintArrowId = suggestion.id)
        sound.playHint()
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(FeedbackTiming.HINT_MS.toLong())
            if (generation == session) {
                _state.update { it.copy(hintArrowId = it.hintArrowId?.takeIf { id -> id != suggestion.id }) }
            }
        }
    }

    /** Duplicate callbacks and callbacks from previous loads have no effect. */
    fun onDepartureFinished(token: Long) {
        val current = _state.value
        val finished = current.departing.firstOrNull { it.token == token } ?: return
        board?.remove(finished.arrow)
        val next = current.copy(departing = current.departing.filterNot { it.token == token })
        _state.value = next
        lastTapAt = 0L
        if (next.hasFinishedBoard && next.status == GameStatus.PLAYING) {
            complete(requireNotNull(next.level), session)
        }
    }

    fun toggleGrid(currentlyShown: Boolean) {
        viewModelScope.launch { preferences.setShowGrid(!currentlyShown) }
    }

    /** Debug-only action; it uses the same input and animation lifecycle as a tap. */
    fun playSolution() {
        val current = _state.value
        val level = current.level ?: return
        if (autoPlayJob?.isActive == true) {
            autoPlayJob?.cancel()
            return
        }
        if (!current.acceptsInput) return
        val generation = session
        autoPlayJob = viewModelScope.launch {
            val sequence = PuzzleSolver.solve(level, current.activeArrows).moveSequence
            for (arrow in sequence) {
                while (generation == session && _state.value.status == GameStatus.PLAYING &&
                    !_state.value.acceptsInput) delay(16L)
                if (generation != session || _state.value.status != GameStatus.PLAYING) break
                delay(120L)
                if (generation == session) tap(arrow)
            }
        }
    }

    private fun complete(level: Level, generation: Long) {
        if (completionJob != null) return
        completionJob = viewModelScope.launch {
            preferences.onLevelCompleted(level.id)
            if (generation != session) return@launch
            if (_state.value.hasFinishedBoard && _state.value.status == GameStatus.PLAYING) {
                sound.playComplete()
                haptics.success()
                _state.update { it.copy(status = GameStatus.VICTORY) }
            }
        }
    }

    override fun onCleared() {
        ++session
        loadJob?.cancel()
        hintJob?.cancel()
        blockedJob?.cancel()
        autoPlayJob?.cancel()
        completionJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(
            preferences: GamePreferences,
            sound: SoundEngine,
            haptics: Haptics = Haptics.None
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(preferences, sound, haptics) }
        }
    }
}
