package com.amowpuzzle.aitricker

import android.app.Application
import android.content.Context
import com.amowpuzzle.aitricker.data.haptics.Haptics
import com.amowpuzzle.aitricker.data.haptics.SystemHaptics
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.data.sound.MusicEngine
import com.amowpuzzle.aitricker.data.sound.SoundEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The single owner of everything with a lifetime longer than a screen.
 *
 * There is no dependency-injection framework here on purpose: the game needs four
 * long-lived objects, and a container the Application holds is easier to follow -
 * and starts faster - than a graph.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferences = GamePreferences(appContext)
    val sound = SoundEngine()
    val music = MusicEngine()
    val haptics: Haptics = SystemHaptics(appContext)

    /**
     * Builds the opening levels off the main thread so the first tap on a level card
     * never waits for generation. The rest are built on demand, and the game screen
     * warms the level after the one being played.
     */
    fun warmUp() {
        scope.launch { LevelCatalog.preload(1..12) }
    }

    fun release() {
        sound.release()
        music.release()
    }
}

class ArrowPuzzleApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.warmUp()
    }
}

/** Reaches the container from anywhere that has a Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as ArrowPuzzleApp).container
