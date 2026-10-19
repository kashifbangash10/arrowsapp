package com.amowpuzzle.aitricker.data.prefs

/** Keeps completed-campaign players moving when additional levels are installed. */
internal fun unlockedThrough(saved: Int, completed: Set<Int>, total: Int): Int {
    require(total > 0)
    val lastCompleted = completed.filter { it in 1..total }.maxOrNull() ?: 0
    return maxOf(saved, lastCompleted + 1).coerceIn(1, total)
}
