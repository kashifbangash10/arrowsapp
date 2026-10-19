package com.amowpuzzle.aitricker.redesign

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.prefs.GameProgressWriter
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.data.sound.SoundEngine
import com.amowpuzzle.aitricker.presentation.complete.LevelCompleteScreen
import com.amowpuzzle.aitricker.presentation.game.GameScreen
import com.amowpuzzle.aitricker.presentation.game.GameStatus
import com.amowpuzzle.aitricker.presentation.game.GameViewModel
import com.amowpuzzle.aitricker.presentation.levels.LevelSelectScreen
import com.amowpuzzle.aitricker.ui.components.SystemBarAppearance
import com.amowpuzzle.aitricker.ui.theme.ArrowPuzzleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/**
 * Debug APK only. Runs production GameScreen, GameViewModel and LevelCatalog, but
 * never constructs GamePreferences or writes campaign progress to disk.
 *
 * adb shell am start -n com.amowpuzzle.aitricker/.redesign.CampaignPreviewActivity \
 *     --ei level 26 --ez dark true --ez grid false --ez autoplay false
 *
 * Memory-only category/progression preview:
 *     --es screen levels --ei completedThrough 249 --ei unlockedThrough 250
 */
class CampaignPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val levelId = intent.getIntExtra("level", 1).coerceIn(1, LevelCatalog.TOTAL_LEVELS)
        val darkInitially = intent.getBooleanExtra("dark", true)
        val autoPlay = intent.getBooleanExtra("autoplay", false)
        val levelsInitially = intent.getStringExtra("screen") == "levels"
        val completedThrough = intent.getIntExtra("completedThrough", 0).coerceIn(0, LevelCatalog.TOTAL_LEVELS)
        val unlockedThrough = intent.getIntExtra("unlockedThrough", completedThrough + 1)
            .coerceIn(1, LevelCatalog.TOTAL_LEVELS)
        val resources = ViewModelProvider(this, viewModelFactory {
            initializer {
                PreviewSessionResources(intent.getBooleanExtra("grid", false), completedThrough, unlockedThrough)
            }
        })[PreviewSessionResources::class.java]
        val game = ViewModelProvider(this, viewModelFactory {
            initializer { GameViewModel(resources.progress, resources.sound) }
        })[GameViewModel::class.java]
        if (game.state.value.level == null) game.load(levelId)

        setContent {
            ArrowPuzzleTheme {
                val state by game.state.collectAsStateWithLifecycle()
                val showGrid by resources.progress.showGrid.collectAsStateWithLifecycle()
                val previewSnapshot by resources.progress.snapshot.collectAsStateWithLifecycle()
                var boardDark by rememberSaveable { mutableStateOf(darkInitially) }
                var showingLevels by rememberSaveable { mutableStateOf(levelsInitially) }
                SystemBarAppearance(showingLevels || (boardDark && state.levelId != 1) || state.status == GameStatus.VICTORY)
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var autoPlayStarted by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(levelId, autoPlay) {
                    if (autoPlay && !autoPlayStarted) {
                        game.state.first { !it.isLoading && it.levelId == levelId && it.acceptsInput }
                        delay(600L)
                        autoPlayStarted = true
                        game.playSolution()
                    }
                }
                LaunchedEffect(state.levelId, state.status) {
                    if (state.status == GameStatus.VICTORY) {
                        Log.i(TAG, "VICTORY level=${state.levelId} moves=${state.movesMade} lives=${state.lives}")
                    }
                }

                if (showingLevels) {
                    LevelSelectScreen(
                        snapshot = previewSnapshot,
                        onLevel = { selected ->
                            Log.i(TAG, "SELECT level=$selected")
                            game.load(selected)
                            showingLevels = false
                        },
                        onBack = { finish() }
                    )
                } else if (state.status == GameStatus.VICTORY) {
                    LevelCompleteScreen(
                        levelId = state.levelId,
                        moves = state.movesMade,
                        livesLeft = state.lives,
                        onNext = {
                            if (state.levelId < LevelCatalog.TOTAL_LEVELS) game.load(state.levelId + 1)
                            else finish()
                        },
                        onReplay = game::retry,
                        onMenu = { showingLevels = true }
                    )
                } else {
                    GameScreen(
                        state = state,
                        showGrid = showGrid,
                        boardDark = boardDark,
                        onTap = game::tap,
                        onDepartureFinished = game::onDepartureFinished,
                        onHint = game::useHint,
                        onToggleGrid = { game.toggleGrid(showGrid) },
                        onRetry = game::retry,
                        onBack = { showingLevels = true },
                        onSettings = { showSettings = true },
                        onSolved = { _, _, _ -> },
                        onPlaySolution = game::playSolution
                    )
                }
                if (showSettings) {
                    AlertDialog(
                        onDismissRequest = { showSettings = false },
                        title = { Text("Preview settings") },
                        text = {
                            Column {
                                Text("This session does not change saved campaign progress.")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dark board")
                                    Switch(checked = boardDark, onCheckedChange = { boardDark = it })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Grid")
                                    Switch(checked = showGrid, onCheckedChange = { game.toggleGrid(showGrid) })
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSettings = false }) { Text("Close") }
                        }
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "CampaignPreview"
    }
}

/** Retained through Activity recreation, released with the preview's ViewModelStore. */
private class PreviewSessionResources(
    showGridInitially: Boolean,
    completedThrough: Int,
    unlockedThrough: Int
) : ViewModel() {
    val progress = MemoryPreviewProgress(showGridInitially, completedThrough, unlockedThrough)
    val sound = SoundEngine().apply { enabled = false }

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }
}

private class MemoryPreviewProgress(
    showGridInitially: Boolean,
    completedThrough: Int,
    unlockedThrough: Int
) : GameProgressWriter {
    val showGrid = MutableStateFlow(showGridInitially)
    val snapshot = MutableStateFlow(
        GamePreferences.Snapshot(
            highestUnlocked = LevelCatalog.TOTAL_LEVELS,
            completed = (1..completedThrough).toSet(),
            showGrid = showGridInitially,
            soundEnabled = false,
            musicEnabled = false,
            hapticsEnabled = false,
            tutorialShown = true
        )
    )

    override suspend fun onLevelCompleted(levelId: Int) {
        snapshot.value = snapshot.value.copy(
            completed = snapshot.value.completed + levelId,
            highestUnlocked = maxOf(snapshot.value.highestUnlocked, levelId + 1).coerceAtMost(LevelCatalog.TOTAL_LEVELS)
        )
        Log.i("CampaignPreview", "Memory-only completion level=$levelId")
    }

    override suspend fun setShowGrid(enabled: Boolean) {
        showGrid.value = enabled
        snapshot.value = snapshot.value.copy(showGrid = enabled)
    }
}