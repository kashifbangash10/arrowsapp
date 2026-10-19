package com.amowpuzzle.aitricker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "arrow_puzzle")

/** Everything that has to survive the app being closed: progress and settings. */
class GamePreferences(private val context: Context) : GameProgressWriter {

    data class Snapshot(
        val highestUnlocked: Int = LevelCatalog.TOTAL_LEVELS,
        val completed: Set<Int> = emptySet(),
        val showGrid: Boolean = false,
        val soundEnabled: Boolean = true,
        val musicEnabled: Boolean = true,
        val hapticsEnabled: Boolean = true,
        val tutorialShown: Boolean = false,
        /** Uniform dark board and white ink by default; an explicit user setting is retained. */
        val boardDark: Boolean = true
    ) {
        // Open access is independent of earned completion records.
        fun isUnlocked(levelId: Int) = levelId in LevelCatalog.levelIds
        fun isCompleted(levelId: Int) = completed.contains(levelId)

        /** Where the Play button on the menu should take the player. */
        val resumeLevel: Int
            get() = (1..LevelCatalog.TOTAL_LEVELS).firstOrNull { it !in completed }
                ?: LevelCatalog.TOTAL_LEVELS
    }

    private object Keys {
        val highestUnlocked = intPreferencesKey("highest_unlocked_level")
        val completed = stringSetPreferencesKey("completed_levels")
        val showGrid = booleanPreferencesKey("show_grid")
        val sound = booleanPreferencesKey("sound_enabled")
        val music = booleanPreferencesKey("music_enabled")
        val haptics = booleanPreferencesKey("haptics_enabled")
        val tutorialShown = booleanPreferencesKey("tutorial_shown")
        val boardDark = booleanPreferencesKey("board_dark_theme")
    }

    val snapshots: Flow<Snapshot> = context.gameDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val completed = prefs[Keys.completed].orEmpty().mapNotNull { it.toIntOrNull() }
                .filter { it in 1..LevelCatalog.TOTAL_LEVELS }.toSet()
            Snapshot(
                highestUnlocked = LevelCatalog.TOTAL_LEVELS,
                completed = completed,
                showGrid = prefs[Keys.showGrid] ?: false,
                soundEnabled = prefs[Keys.sound] ?: true,
                musicEnabled = prefs[Keys.music] ?: true,
                hapticsEnabled = prefs[Keys.haptics] ?: true,
                tutorialShown = prefs[Keys.tutorialShown] ?: false,
                boardDark = prefs[Keys.boardDark] ?: true
            )
        }

    /** Records an earned win while keeping the entire campaign available. */
    override suspend fun onLevelCompleted(levelId: Int) {
        require(levelId in LevelCatalog.levelIds)
        context.gameDataStore.edit { prefs ->
            val done = prefs[Keys.completed].orEmpty().toMutableSet()
            done.add(levelId.toString())
            prefs[Keys.completed] = done
            prefs[Keys.highestUnlocked] = LevelCatalog.TOTAL_LEVELS
        }
    }

    override suspend fun setShowGrid(enabled: Boolean) = put(Keys.showGrid, enabled)

    suspend fun setSoundEnabled(enabled: Boolean) = put(Keys.sound, enabled)

    suspend fun setMusicEnabled(enabled: Boolean) = put(Keys.music, enabled)

    suspend fun setHapticsEnabled(enabled: Boolean) = put(Keys.haptics, enabled)

    suspend fun setTutorialShown(shown: Boolean) = put(Keys.tutorialShown, shown)

    suspend fun setBoardDark(enabled: Boolean) = put(Keys.boardDark, enabled)

    suspend fun resetProgress() {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.highestUnlocked] = LevelCatalog.TOTAL_LEVELS
            prefs[Keys.completed] = emptySet()
            prefs[Keys.tutorialShown] = false
        }
    }

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) {
        context.gameDataStore.edit { it[key] = value }
    }
}
