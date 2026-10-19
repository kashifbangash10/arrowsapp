package com.amowpuzzle.aitricker.data.prefs

/** The two progress writes a play session needs; independent of Android storage. */
interface GameProgressWriter {
    suspend fun onLevelCompleted(levelId: Int)
    suspend fun setShowGrid(enabled: Boolean)
}
